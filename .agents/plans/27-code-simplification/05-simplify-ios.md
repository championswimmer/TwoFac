---
name: Code Simplification Plan - iosApp
status: Planned
progress:
  - "[ ] Phase 0: Remove dead sync coordinator path"
  - "[ ] Phase 1: Unify iOS/watch payload contract keys"
  - "[ ] Phase 2: Reduce watch extension runtime overhead"
  - "[ ] Phase 3: Theme/token alignment and diagnostics cleanup"
  - "[ ] Phase 4: Validate iOS + watchOS sync end-to-end"
---

# Code Simplification Plan - iosApp

## Scope
- Module: `iosApp` (`iosApp` target + `watchAppExtension`)
- Goal: remove dead sync code, align cross-platform payload contracts, and reduce watch runtime overhead/battery drain.

## High-Signal Findings (evidence)
1. **Dead/unused Swift sync coordinator**
   - `iosApp/iosApp/WatchSyncCoordinator.swift`
   - not referenced by app entry (`iOSApp.swift`) or content flow.
   - initializes `TwoFacLib` with `MemoryStorage` default, which is incorrect for real sync.
2. **Payload key drift risk**
   - Swift watch extension uses `"payloadString"` key.
   - `composeApp` iOS coordinator also uses `"payloadString"` constant.
   - shared contract currently defines `SNAPSHOT_PAYLOAD_KEY = "payload"`.
3. **Avoidable I/O in incoming payload flow**
   - `watchAppExtension/WatchConnectivityManager.swift`
   - `handleIncomingPayloadString` persists then immediately re-reads file before processing.
4. **High-frequency ticker for second-level countdown**
   - `watchAppExtension/WatchExtensionContentView.swift`
   - `Timer.publish(every: 1.0 / 30.0, ...)` for UI that displays second-based expiry.
5. **Theme token bypass in watch extension**
   - `WatchThemeTokens.timerTrackColor()` and `backgroundColor()` hardcoded to `.black`.
6. **Diagnostics state churn in production**
   - `WatchConnectivityManager.debugEvents` always published/appended.

## Simplification Roadmap

### Phase 0: Remove dead sync coordinator path
- [ ] Delete `iosApp/iosApp/WatchSyncCoordinator.swift`.
- [ ] Update `iosApp/AGENTS.md` to reflect actual KMP-driven sync path (`IosCompanionSyncCoordinator`).

### Phase 1: Unify iOS/watch payload contract keys
- [ ] Define a single contract constant strategy for iOS payload key(s), shared across Kotlin + Swift bridge.
- [ ] Replace ad-hoc key literals in:
  - `composeApp/src/iosMain/.../IosCompanionSyncCoordinator.kt`
  - `iosApp/watchAppExtension/WatchConnectivityManager.swift`
- [ ] Add compatibility fallback during migration if needed.

### Phase 2: Reduce watch extension runtime overhead
- [ ] Refactor `handleIncomingPayloadString` to process in-memory payload directly after persist.
- [ ] Replace 30fps ticker with `TimelineView` or 1Hz update cadence for countdown.
- [ ] Keep behavior-equivalent UI output and verify no staleness in displayed OTP expiry.

### Phase 3: Theme/token alignment and diagnostics cleanup
- [ ] Route watch background/timer track colors through `TwoFacKit` tokens instead of hardcoded black.
- [ ] Gate verbose debug event collection/log feed behind DEBUG build condition.

### Phase 4: Validate iOS + watchOS sync end-to-end
- [ ] Validate on simulator/device pair:
  - initial sync
  - app relaunch with persisted payload
  - stale payload rejection logic
  - manual refresh behavior
- [ ] Confirm no regression in watch page navigation and OTP generation.

## Success Criteria
- Only one sync coordinator path exists for iOS -> watch sync.
- Payload key definitions are centralized and contract-safe.
- Watch extension update cadence is battery-friendly.
- Theme rendering and diagnostics are cleaner with no behavior regressions.
