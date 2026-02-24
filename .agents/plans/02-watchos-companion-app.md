# TwoFac watchOS Companion App Plan (SwiftUI + TwoFacKit)

## Goal
Build a **watchOS companion app** for TwoFac where:
- UI is native SwiftUI on watchOS (no Compose on watchOS).
- business logic/OTP generation comes from `TwoFacKit` (framework built from `sharedLib`).
- watch syncs secrets from paired iPhone app periodically and on-demand.
- after sync, watch can show OTP entries offline on watch.
- UX is **one code per vertical swipe screen** to keep the interface uncluttered.

---

## Why this architecture

1. **Companion pattern fits Apple’s model**
   - `WCSession` is designed for iOS ↔ watchOS companion communication.
   - Immediate messages require reachability, while background transfer APIs queue/deliver opportunistically.
2. **TwoFacKit keeps crypto/OTP logic centralized**
   - avoids reimplementing TOTP/HOTP/security logic in Swift.
   - keeps parity with Android/Desktop/CLI behavior.
3. **SwiftUI watch-first UI is now preferred**
   - watchOS provides vertical page tab style (`.tabViewStyle(.verticalPage)`) suitable for one-code-per-screen swipe UI.

---

## Internet research summary (key findings)

### WatchConnectivity transport choices
From Apple WatchConnectivity docs and sample docs:
- `updateApplicationContext(_:)`
  - latest-state sync, overwrites previous context in pipeline.
  - ideal for “current snapshot” semantics.
- `sendMessage(_:replyHandler:errorHandler:)`
  - immediate/interactive, requires `isReachable == true`.
  - watch-to-phone can wake iOS app when sent from active watch app.
- `transferUserInfo(_:)`
  - guaranteed queued delivery, preserves order, continues when app suspended.
- `transferFile(_:metadata:)`
  - best for larger payloads/files; background and queued.
- `hasContentPending` and `WKWatchConnectivityRefreshBackgroundTask`
  - must complete background tasks; defer completion until pending content is drained.

### Session lifecycle requirements
- Must set delegate and call `activate()` before transfers.
- Transfers allowed only in `activationState == .activated`.
- iOS delegate must handle inactive/deactivated transitions for multi-watch switching.

### Background refresh behavior
- `scheduleBackgroundRefresh(withPreferredDate:...)` can request refresh windows.
- system budgets refreshes; complications can influence budget availability.
- watchOS docs prefer SwiftUI background task patterns on newer versions.

### watchOS UI pattern
- `TabView` with `.tabViewStyle(.verticalPage)` (watchOS 10+) provides native vertical one-screen paging.

---

## Current repository baseline (relevant)

- `sharedLib` already defines iOS framework base name as **`TwoFacKit`**.
- `iosApp` currently embeds and renders `ComposeApp` UI (`ContentView.swift` uses `MainViewControllerKt.MainViewController()`).
- iOS app Xcode project currently has only iOS target; no watch app target yet.

---

## Step-by-step implementation plan

## Phase 0 — Product + security decisions
1. Confirm supported watchOS/iOS versions (recommend watchOS 10+ for vertical page style).
2. Decide sync trigger policy:
   - on iOS app foreground,
   - on secret CRUD events,
   - periodic background push from iOS,
   - watch manual pull action.
3. Decide storage policy on watch:
   - encrypted-at-rest cache only,
   - optional “Require unlock before revealing code” gate.
4. Finalize payload contract version (`v1`) and migration strategy.

## Phase 1 — Shape `TwoFacKit` for Swift interop
1. Audit exported APIs from `sharedLib` for Swift friendliness:
   - avoid Kotlin-only types at public boundary where possible.
   - provide thin `@PublicApi` facades for Swift-consumable methods.
2. Expose minimal watch-facing use cases in `TwoFacKit`:
   - import serialized secrets payload,
   - list account view models (id, issuer, name, period, digits),
   - generate current code + remaining seconds,
   - clear all secrets.
3. Add stable DTOs for interchange:
   - `SecretRecordDto`, `SecretBundleDto`, `SyncEnvelopeDto` (schemaVersion, generatedAt, records).
4. Add deterministic serialization for sync payload + integrity check support.

## Phase 2 — Add watchOS app target in `iosApp` Xcode project
1. Add new targets:
   - Watch App target
   - Watch App Extension target
2. Ensure bundle id relationship and `WKCompanionAppBundleIdentifier` are correct.
3. Add capabilities/entitlements:
   - App Groups shared container (iOS + watch extension)
   - WatchConnectivity
   - Keychain sharing (if needed by chosen storage design)
4. Wire `TwoFacKit.framework` to watch extension build settings (framework search paths/link/embed).

## Phase 3 — Introduce iOS↔watch connectivity layer (iOS side)
1. Create `WatchSyncCoordinator` in iOS app:
   - owns `WCSession.default`, delegate, activation, retries.
2. Implement outbound sync channels with clear intent:
   - `updateApplicationContext` for latest snapshot metadata (quick state signal)
   - `transferUserInfo` for guaranteed snapshot deliveries
   - `sendMessage` for immediate manual refresh when reachable
3. Add de-duplication/versioning:
   - monotonic `revision` and payload hash.
4. Add iOS-side queueing/backoff when not activated or app state is constrained.
5. Handle multi-watch switching in iOS delegate:
   - `sessionDidBecomeInactive`, `sessionDidDeactivate`, then `activate()`.

## Phase 4 — Implement watch extension connectivity intake
1. Create `WatchConnectivityManager` in watch extension:
   - activate session on launch.
   - receive all transport forms (`applicationContext`, `userInfo`, optional file).
2. Centralize decode/validate pipeline:
   - schemaVersion check,
   - payload signature/integrity check,
   - timestamp/revision guard to prevent stale overwrite.
3. Persist accepted payload to watch local encrypted store.
4. Mark background tasks complete only after pending content processed (`hasContentPending == false`).

## Phase 5 — watch data model + timer engine
1. Build `WatchOtpStore` (observable state):
   - cached accounts,
   - lastSyncAt,
   - sync state/error.
2. Build `OtpTicker`:
   - 1s timer updates `remainingSeconds` and currently visible code.
   - minimize recomposition/updates for battery.
3. Ensure offline behavior:
   - no phone needed to generate codes once secrets synced.

## Phase 6 — SwiftUI watch UX (one code per vertical swipe)
1. Root screen uses:
   - `TabView(selection: ...)` over accounts,
   - `.tabViewStyle(.verticalPage)`.
2. Each page shows one account card only:
   - issuer + account name,
   - current OTP code (large, monospaced),
   - countdown ring/progress,
   - small “synced X min ago” status.
3. Add optional controls:
   - pull-to-refresh or button to request phone sync (`sendMessage`).
4. Empty/error states:
   - “Open iPhone app to sync accounts” when none available,
   - explicit connectivity last error message.

## Phase 7 — iOS app integration updates needed
1. Add settings screen section in iOS app (can be SwiftUI alongside Compose host):
   - watch sync enabled toggle,
   - last sync timestamp/status,
   - “Sync now” action.
2. Trigger sync events from iOS app lifecycle:
   - after account add/edit/delete/import,
   - on iOS app foreground.
3. Respect privacy/security settings:
   - if user disables watch sync, send wipe signal to watch + clear watch store.
4. Add migration behavior:
   - first launch after feature flag on => full snapshot transfer.

## Phase 8 — Security hardening
1. Don’t transmit raw backup blobs; transmit minimal secret bundle for OTP generation.
2. Encrypt payload before transfer when possible (application-layer encryption key derived/stored in shared group/keychain).
3. Add replay/stale protection (revision + generatedAt + hash).
4. Protect logs: never log secrets or OTPs.
5. Add local wipe pathways:
   - phone unpair/account sign-out/watch sync disable.

## Phase 9 — Testing strategy
1. `sharedLib` tests:
   - sync envelope serialization/deserialization,
   - integrity check/revision conflict logic,
   - Swift-facing facade behavior.
2. iOS/watch unit tests:
   - connectivity manager routing per transport type,
   - stale update rejection,
   - watch store persistence/clear behavior.
3. Manual device tests (required; simulator has WatchConnectivity limits):
   - first sync, incremental sync, phone unreachable, watch offline,
   - watch app restart persistence,
   - unpair/repair behavior,
   - background delivery + task completion correctness.

## Phase 10 — Rollout plan
1. Ship behind feature flag (`watchSyncEnabled`).
2. Internal dogfood with telemetry:
   - sync success rate,
   - median sync latency,
   - stale payload rejection rate,
   - crash-free sessions for watch extension.
3. Expand rollout in stages; keep a remote disable flag.

---

## Concrete code-change checklist (repo-focused)

1. **`sharedLib`**
   - add/adjust Swift-friendly public APIs for secret import/list/code generation.
   - add sync DTO + serialization tests.
2. **`iosApp`**
   - update `iosApp.xcodeproj` with watch targets/capabilities.
   - add Swift files for `WatchSyncCoordinator` and delegate handlers.
   - add iOS settings UI hooks for watch sync control/status.
3. **new watch extension/watch app sources (under `iosApp`)**
   - `WatchConnectivityManager`, `WatchOtpStore`, `OtpTicker`, SwiftUI vertical page screens.
4. **build integration**
   - ensure `TwoFacKit` framework available to watch extension build path and signed correctly.

---

## Risks and mitigations

1. **Connectivity API misuse / wrong channel**
   - Mitigation: strict channel policy (state via context, guaranteed snapshots via userInfo/file, interactive pull via sendMessage).
2. **Background budget exhaustion**
   - Mitigation: coalesce updates, bounded sync frequency, complete background tasks promptly.
3. **Stale data on watch**
   - Mitigation: revision/hash checks + explicit sync status UI.
4. **Security leakage**
   - Mitigation: encrypted payloads, scrubbed logs, explicit wipe flows.

---

## Sources researched

- Apple docs mirror (WCSession overview):  
  https://raw.githubusercontent.com/zhangyu1818/apple-docs-for-rag/df89f1451e3da24868b8489602d1bf72f179d7f7/WatchConnectivity/md/watch%20connectivity-wcsession.md
- Apple docs mirror (`updateApplicationContext`):  
  https://raw.githubusercontent.com/zhangyu1818/apple-docs-for-rag/df89f1451e3da24868b8489602d1bf72f179d7f7/WatchConnectivity/md/watch%20connectivity-wcsession-updateapplicationcontext(_:).md
- Apple docs mirror (`sendMessage`):  
  https://raw.githubusercontent.com/zhangyu1818/apple-docs-for-rag/df89f1451e3da24868b8489602d1bf72f179d7f7/WatchConnectivity/md/watch%20connectivity-wcsession-sendmessage(_:replyhandler:errorhandler:).md
- Apple docs mirror (`transferUserInfo`):  
  https://raw.githubusercontent.com/zhangyu1818/apple-docs-for-rag/df89f1451e3da24868b8489602d1bf72f179d7f7/WatchConnectivity/md/watch%20connectivity-wcsession-transferuserinfo(_:).md
- Apple docs mirror (`transferFile`):  
  https://raw.githubusercontent.com/zhangyu1818/apple-docs-for-rag/df89f1451e3da24868b8489602d1bf72f179d7f7/WatchConnectivity/md/watch%20connectivity-wcsession-transferfile(_:metadata:).md
- Apple sample doc mirror (Transferring data with Watch Connectivity):  
  https://raw.githubusercontent.com/zhangyu1818/apple-docs-for-rag/df89f1451e3da24868b8489602d1bf72f179d7f7/WatchConnectivity/md/watch%20connectivity-transferring%20data%20with%20watch%20connectivity-connectivity.md
- Apple docs mirror (`WKWatchConnectivityRefreshBackgroundTask`):  
  https://raw.githubusercontent.com/zhangyu1818/apple-docs-for-rag/df89f1451e3da24868b8489602d1bf72f179d7f7/WatchKit/md/watchkit-wkwatchconnectivityrefreshbackgroundtask.md
- Apple docs mirror (`scheduleBackgroundRefresh`):  
  https://raw.githubusercontent.com/zhangyu1818/apple-docs-for-rag/df89f1451e3da24868b8489602d1bf72f179d7f7/WatchKit/md/watchkit-wkextension-schedulebackgroundrefresh(withpreferreddate:userinfo:scheduledcompletion:).md
- Apple docs mirror (`WKApplicationRefreshBackgroundTask` budget guidance):  
  https://raw.githubusercontent.com/zhangyu1818/apple-docs-for-rag/df89f1451e3da24868b8489602d1bf72f179d7f7/WatchKit/md/watchkit-wkapplicationrefreshbackgroundtask.md
- Apple docs mirror (`TabViewStyle.verticalPage`):  
  https://raw.githubusercontent.com/zhangyu1818/apple-docs-for-rag/df89f1451e3da24868b8489602d1bf72f179d7f7/SwiftUI/md/swiftui-tabviewstyle-verticalpage.md
- WWDC notes (watchOS 10 vertical page examples):  
  https://raw.githubusercontent.com/drewcrawford/wwdc-notes/a981d396efeb286112db97d8407192a024e10235/23/Update%20your%20app%20for%20watchOS%2010.md
- Additional community implementation references:  
  https://raw.githubusercontent.com/KaneCheshire/Communicator/main/README.md  
  https://raw.githubusercontent.com/NAOYA-MAEDA-DEV/Watch-Connectivity-Sample/main/README.md
