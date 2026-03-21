---
name: Fix Companion Watch Detection
status: Done
progress:
  - "[x] Phase 1: Fix iOS WCSession activation race condition"
  - "[x] Phase 2: Fix Android detection to use FILTER_ALL fallback"
  - "[x] Phase 3: Add reactive capability/state change listeners"
  - "[x] Phase 4: Fix watch-side REQUEST_SYNC_NOW no-op"
  - "[x] Phase 5: Validate with builds"
---

# Fix Companion Watch Detection

## Problem

Both Android and iOS apps are unable to detect their companion watch apps
(Wear OS and watchOS respectively), even when the watch app is installed and
the watch is paired. This feature previously worked.

Root causes identified:

1. **iOS race condition**: `IosCompanionSyncCoordinator.isCompanionActive()`
   calls `activateSession()` (async) then immediately reads `isPaired()` and
   `isWatchAppInstalled()`. These properties are only valid after
   `activationState == .activated`, so they return `false` when checked before
   activation completes.

2. **Android overly-strict filter**: `WearSyncDataLayerClient.hasReachableWatchCompanion()`
   uses `CapabilityClient.FILTER_REACHABLE`, which requires active Bluetooth.
   If the watch is paired but momentarily disconnected, detection fails.
   `forceDiscoverWatchCompanion()` uses `FILTER_ALL` but is only triggered
   manually.

3. **No state-change listeners** on either platform: detection runs once in a
   `LaunchedEffect` when the Settings screen opens. If the initial check fails,
   the UI remains stale.

4. **Watch no-op**: `WatchSyncListenerService.onMessageReceived()` receives
   `REQUEST_SYNC_NOW_MESSAGE_PATH` but does nothing (immediate `return`).

## Investigation: Bundle / Package IDs

All IDs are **correct** — the detection issue is not caused by identifier
mismatches.

| Platform       | Module      | ID                                        | OK? |
|----------------|-------------|-------------------------------------------|-----|
| Android phone  | androidApp  | `applicationId = "tech.arnav.twofac"`     | ✅  |
| Android watch  | watchApp    | `applicationId = "tech.arnav.twofac"`     | ✅  |
| iOS phone      | iosApp      | `tech.arnav.twofac.app` (xcconfig)        | ✅  |
| iOS watch      | watchApp    | `tech.arnav.twofac.app.watchkitapp` (pbxproj) | ✅ |
| iOS watch plist| WKCompanionAppBundleIdentifier | `tech.arnav.twofac.app`    | ✅  |

### Architectural changes that could have affected detection

1. **Commit `f8eac8c`** — `composeApp` switched from `androidApplication` to
   `androidKotlinMultiplatformLibrary`. The `wear.xml` capability declaration
   now lives in a library module instead of an app module. Resource merging
   _should_ still work, but should be verified by inspecting the merged APK
   resources (`aapt dump resources androidApp/build/.../app-debug.apk`).

2. **Commit `4cf290e`** — iOS watchApp product type changed from
   `com.apple.product-type.application.watchapp2` to
   `com.apple.product-type.application` (modern single-target watchOS arch).
   This is the correct modern approach, but the `watchAppExtension` target was
   removed while **the watchApp's `CODE_SIGN_ENTITLEMENTS` still points to
   `watchAppExtension/watchAppExtension.entitlements`** instead of
   `watchApp/watchApp.entitlements`. This should be corrected but is unlikely to
   cause the detection failure.

## Plan

### Phase 1 – Fix iOS WCSession activation race condition

**File:** `composeApp/src/iosMain/kotlin/tech/arnav/twofac/companion/IosCompanionSyncCoordinator.kt`

- Track activation state inside the coordinator via the session delegate
  callback (`activationDidCompleteWithState`). Use a
  `CompletableDeferred<Boolean>` (or `StateFlow<Boolean>`) that completes when
  `activationState == WCSessionActivationStateActivated`.
- In `isCompanionActive()` and `forceDiscoverCompanion()`, **suspend until
  activation completes** (with a short timeout ~3 s) before reading `isPaired()`
  and `isWatchAppInstalled()`.
- Keep the existing `init` block that eagerly starts activation so the deferred
  is likely already complete by the time Settings opens.

### Phase 2 – Fix Android detection to use FILTER_ALL fallback

**File:** `composeApp/src/androidMain/kotlin/tech/arnav/twofac/wear/WearSyncDataLayerClient.kt`

- In `hasReachableWatchCompanion()`, if `FILTER_REACHABLE` returns empty,
  fall back to `FILTER_ALL` to detect installed-but-not-currently-connected
  watch companions. Return a result that distinguishes "installed but not
  reachable" from "not installed at all" so the UI can show an appropriate
  message.
- Update `AndroidWatchSyncCoordinator.isCompanionActive()` to treat
  "installed but not reachable" as a soft-active state (companion exists, sync
  can be attempted when connectivity returns).

### Phase 3 – Add reactive capability/state change listeners

**Android** (`WearSyncDataLayerClient.kt` / `AndroidWatchSyncCoordinator.kt`):
- Register a `CapabilityClient.OnCapabilityChangedListener` for
  `WatchSyncContract.WATCH_CAPABILITY` in the coordinator. Expose a
  `StateFlow<Boolean>` for companion availability.

**iOS** (`IosCompanionSyncCoordinator.kt`):
- In the `WCSessionDelegateProtocol` implementation, observe
  `sessionWatchStateDidChange` (called when `isPaired` or
  `isWatchAppInstalled` change). Expose a `StateFlow<Boolean>`.

**Common** (`SettingsScreen.kt`):
- Add `companionActiveFlow: StateFlow<Boolean>?` to
  `CompanionSyncCoordinator` interface (with null default for
  non-watch platforms).
- In SettingsScreen, `collectAsState()` from this flow so the UI
  updates reactively instead of relying on a one-shot `LaunchedEffect`.

### Phase 4 – Fix watch-side REQUEST_SYNC_NOW no-op

**File:** `watchApp/src/main/java/tech/arnav/twofac/watch/datalayer/WatchSyncListenerService.kt`

- In `onMessageReceived()`, when receiving `REQUEST_SYNC_NOW_MESSAGE_PATH`,
  trigger a data refresh from the latest received snapshot (re-read persisted
  data via `WatchSyncSnapshotRepository`), or send an acknowledgement message
  back to the phone to confirm the watch app is alive.

### Phase 5 – Validate with builds

- `./gradlew --no-daemon :composeApp:compileKotlinMetadata :composeApp:desktopTest`
  (common + Android source validation)
- `./gradlew --no-daemon :watchApp:assembleDebug` (watch app validation)
- `./gradlew --no-daemon :androidApp:assembleDebug` (full Android app)

## Key Files

| File | Change |
|------|--------|
| `composeApp/src/iosMain/.../IosCompanionSyncCoordinator.kt` | Await activation before reading session properties |
| `composeApp/src/androidMain/.../WearSyncDataLayerClient.kt` | FILTER_ALL fallback in detection |
| `composeApp/src/androidMain/.../AndroidWatchSyncCoordinator.kt` | Capability change listener, StateFlow |
| `composeApp/src/commonMain/.../CompanionSyncCoordinator.kt` | Add `companionActiveFlow` to interface |
| `composeApp/src/commonMain/.../screens/SettingsScreen.kt` | Collect from reactive flow |
| `watchApp/.../WatchSyncListenerService.kt` | Handle REQUEST_SYNC_NOW properly |
