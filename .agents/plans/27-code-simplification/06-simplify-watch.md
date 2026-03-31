---
name: Code Simplification Plan - watchApp
status: Planned
progress:
  - "[ ] Phase 0: Stabilize repository lifecycle and message semantics"
  - "[ ] Phase 1: Remove dead state and synchronous suspend noise"
  - "[ ] Phase 2: Simplify startup abstractions"
  - "[ ] Phase 3: Build hygiene and dependency cleanup"
  - "[ ] Phase 4: Battery/perf on wearable"
---

# Code Simplification Plan - watchApp

## Scope
- Module: `watchApp` (Wear OS companion)
- Goal: simplify data-layer flow, remove duplicated initialization/state bloat, and improve battery/performance behavior.

## High-Signal Findings (evidence)
1. **Repository initialization duplicated at multiple call-sites**
   - `WatchSyncListenerService.onCreate()`
   - `WatchSyncListenerService.onMessageReceived()` ← spurious, re-reads disk on every phone ping
   - `MainActivity.onCreate()`
   - `WearApp()` `LaunchedEffect` ← duplicate of the onCreate call above
2. **Path semantic collision in message flow**
   - `REQUEST_SYNC_NOW_MESSAGE_PATH` is sent watch→phone (in `WatchCompanionRegistrar`) AND
     phone→watch (in `WearSyncDataLayerClient.forceDiscoverWatchCompanion()`).
   - There is no phone-side `WearableListenerService` or `addMessageListener` for the
     watch→phone direction — the watch's sync-request message is silently dropped by the phone.
     This means `WatchCompanionRegistrar.requestInitialization()` relies on the phone reacting to
     the `DataLayerClient` DataChanged events, not to this message.
3. **Redundant cache field `schemaVersion`**
   - `WatchSyncCacheState.schemaVersion` duplicates `snapshot.version` already present in
     the stored `WatchSyncSnapshot`. Defaults to the current contract constant, which is
     wrong once a schema migration occurs.
4. **Synchronous suspend function + duplicated computation**
   - `WatchOtpProvider.buildCodes()` has zero suspension points → raises `RedundantSuspendModifier`
     lint (confirmed: Kotlin IDE inspection ID `RedundantSuspendModifier`).
   - `otp.nextCodeAt(nowEpochSec)` computed twice per TOTP item (once for `DisplayAccount.nextCodeAt`
     and once for `WatchOtpEntry.Valid.nextRefreshAtEpochSec`).
5. **Dead `WatchCapabilityRegistrar` object**
   - `wear.xml` already declares `twofac_watch` via `android_wear_capabilities` — static capability
     registration is authoritative and persists without the app running.
   - The programmatic `addLocalCapability` call always hits error 4006 (capability already
     declared by XML), per the comment in the class itself. The class is dead code.
   - Research confirms: use `wear.xml` for core/static identity, `addLocalCapability` only for
     runtime-toggled capabilities.
6. **Broken `@Preview`**
   - `DefaultPreview()` calls `WearApp()` directly. `WearApp` calls
     `WatchSyncSnapshotRepository.get(context)` (requires real filesystem + KStore) and
     `WatchCompanionRegistrar(context)` (requires Google Play Services). Both will crash
     in Android Studio's preview renderer.
7. **Unused `appdirs` dependency**
   - `build.gradle.kts` lists `implementation(libs.kotlin.multiplatform.appdirs)` but no
     Kotlin source file in `watchApp/src/` imports or uses this library. File paths are
     obtained directly via `context.filesDir` (Android-native API). Confirmed by
     `grep -r "appdirs" watchApp/src --include="*.kt"` returning no matches.
8. **Release minification disabled**
   - `isMinifyEnabled = false` in the `release` build type. Wear OS APKs are installed over
     BLE; smaller APK = faster install. Modern `kotlinx.serialization` (≥ 1.5) ships consumer
     ProGuard rules covering standard `@Serializable` data classes — no manual rules needed for
     the classes used here. Proguard file already present in repo.
9. **30 fps wall-clock loop without ambient gating**
   - `WearApp()` runs a `delay(33)` loop unconditionally, updating `currentEpochMillis` at
     ~30 fps for the countdown arc. In ambient mode the display cannot refresh at this rate and
     the system limits UI updates to ~1/min. The correct API is `AmbientLifecycleObserver`
     (`onEnterAmbient` / `onExitAmbient` / `onUpdateAmbient`), which should stop or pause the
     loop in ambient mode and resume it on exit.

---

## Simplification Roadmap

### Phase 0: Stabilize repository lifecycle and message semantics

- [ ] **Remove spurious `repository.initialize()` from `onMessageReceived`.**
  `WatchSyncListenerService.onMessageReceived()` calls `repository.initialize()` (a disk read)
  every time the phone pings the watch. The service's `onCreate()` already initializes the
  repository. The `onMessageReceived` handler should send the ACK only; it should not re-read
  the store on every ping.

- [ ] **Remove duplicate `LaunchedEffect` init from `WearApp()`.**
  `MainActivity.onCreate()` already launches `repository.initialize()`. The
  `LaunchedEffect(repository) { repository.initialize() }` in `WearApp()` is a redundant second
  call in the same process. Remove it; the composable reads state via `collectAsState()`, which
  works with whatever the `onCreate` init left.

- [ ] **Clarify/rename sync message paths in `WatchSyncContract`.**
  Both directions currently share `REQUEST_SYNC_NOW_MESSAGE_PATH`. Rename to make direction
  explicit:
  - `WATCH_REQUEST_SYNC_PATH` = `"/twofac/sync/watch/request_sync"` — watch → phone (sync request)
  - `PHONE_PING_WATCH_PATH`   = `"/twofac/sync/phone/ping"`           — phone → watch (discovery ping)
  
  *Note: investigation confirmed the phone has no `WearableListenerService` and no
  `MessageClient.addMessageListener` for the watch→phone sync-request path. The watch's
  `requestInitialization()` message is currently undelivered on the phone side. Renaming
  clarifies the contract and unblocks a future fix (adding a phone-side listener).*

---

### Phase 1: Remove dead state and synchronous suspend noise

- [ ] **Remove redundant `schemaVersion` from `WatchSyncCacheState`.**
  `schemaVersion: Int = WatchSyncContract.SCHEMA_VERSION` duplicates `snapshot?.version`.
  Remove it. Consumers that need the version should read `snapshot?.version` directly.
  `lastSyncedAtEpochSec` and `lastError` are intentionally retained — they have clear
  diagnostic/display value even if not yet surfaced in UI.

- [ ] **Rename `WatchSyncSyncError` → `WatchSyncError`.**
  The "SyncSync" naming stutter is a straightforward rename. The enum is internal to
  `storage/`, so no external API breakage.

- [ ] **Remove `suspend` from `WatchOtpProvider.buildCodes()` and cache `nextCodeAt`.**
  - `buildCodes()` contains zero suspension points. The Kotlin compiler emits
    `RedundantSuspendModifier` lint for exactly this pattern.
  - The `suspend` modifier forces all callers into a coroutine for no benefit, adds CPS
    (Continuation-Passing Style) bytecode overhead, and misleads readers.
  - Fix: `fun buildCodes(...)` (non-suspend). The `ticker()` flow calls `buildCodes()` inside
    a `flow { }` builder which already suspends via `delay(1000)` — removing `suspend` from
    `buildCodes` is safe.
  - Cache `otp.nextCodeAt(nowEpochSec)` in a local `val` to eliminate the duplicate call per
    TOTP item.

---

### Phase 2: Simplify startup abstractions

- [ ] **Delete `WatchCapabilityRegistrar`.**
  The `wear.xml` static declaration is authoritative. The programmatic call always results in
  error 4006 (as documented by the code's own comment). The class serves no purpose.
  `WatchApplication.onCreate()` can be simplified to an empty override or removed entirely.

- [ ] **Fix `DefaultPreview` to not require live infrastructure.**
  Replace the parameterless `WearApp()` call in `DefaultPreview` with a preview-specific
  composable that accepts pre-built `WatchOtpEntry` list and a `WatchSyncCacheState`. Options:
  - Extract `WearAppContent(entries, state)` composable and have `WearApp()` pass real state
    through; `DefaultPreview` passes hard-coded mock data.
  - Or annotate with `@PreviewParameter` supplying sample `WatchOtpEntry` values.
  Either approach removes the Play Services / KStore dependency from the preview path.

---

### Phase 3: Build hygiene and dependency cleanup

- [ ] **Remove `kotlin.multiplatform.appdirs` from `watchApp/build.gradle.kts`.**
  Confirmed: zero imports in any `.kt` source file. File paths are resolved via
  `context.filesDir` (Android platform API) which is correct for an Android-only module.

- [ ] **Enable release minification.**
  Set `isMinifyEnabled = true` in the `release` block.
  `kotlinx.serialization` ≥ 1.5 includes consumer rules that automatically cover standard
  `@Serializable` data classes (the only pattern used in `WatchSyncCacheState`). Verify
  the existing `proguard-rules.pro` and add rules for Play Services Wearable if R8 strips
  any Wearable API references. Run a release build smoke-test on a connected watch or
  emulator before merging.

---

### Phase 4: Battery/perf on wearable

- [ ] **Gate the 30 fps update loop with `AmbientLifecycleObserver`.**
  The `delay(33)` loop in `WearApp` must stop when the watch enters ambient mode.
  Correct approach:
  ```kotlin
  val ambientObserver = AmbientLifecycleObserver(activity, object : AmbientLifecycleObserver.AmbientLifecycleCallback {
      override fun onEnterAmbient(ambientDetails: AmbientDetails) { /* pause loop */ }
      override fun onExitAmbient() { /* resume loop */ }
      override fun onUpdateAmbient() { /* optional: update clock once per minute */ }
  })
  lifecycle.addObserver(ambientObserver)
  ```
  The `MutableState<Boolean> isAmbient` flag derived from the observer can be passed to
  `WearApp` / `OtpPagerScreen` to also switch the arc animation to a static display.

- [ ] **Lower `otpProvider.ticker` cadence in ambient mode.**
  The OTP ticker (`delay(1000)`) is fine when interactive. In ambient, OTPs do not need
  second-by-second refresh — updating once every 30 s is sufficient. Pass `isAmbient` into
  the ticker (or use a separate ambient flow) to slow the refresh.

---

## Removed Tasks (with rationale)

| Removed task | Reason |
|---|---|
| Add logging/tests for message flow | Not a simplification. Integration-testing Wearable `MessageClient` requires physical devices; adding log statements is editorial, not structural. Not appropriate for this plan. |
| Simplify CompositionLocal theme indirection | `staticCompositionLocalOf { TwoFacThemeTokens.dark }` overhead is negligible. The pattern is consistent with `composeApp` theming and provides future-proofing for ambient-mode colour adjustments. No meaningful simplification. |
| Battery validation (10–15 min session) | QA validation task, not a code change. Out of scope for a code simplification plan; track separately if needed. |

---

## Success Criteria
- Single clear init path: repository init happens once at process start; no redundant disk reads on pings.
- Clean message contract: separate path constants for each direction; phone-side gap documented.
- Lean cache model: `WatchSyncCacheState` carries only meaningful fields.
- `buildCodes` is a regular function with no CPS overhead and no duplicate computation.
- `WatchCapabilityRegistrar` removed; `DefaultPreview` works without Play Services.
- `appdirs` dependency gone from build file.
- Release build minified; Wear OS APK size reduced.
- `delay(33)` loop paused in ambient mode; OTP ticker slowed in ambient.
