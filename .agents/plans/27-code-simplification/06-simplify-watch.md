---
name: Code Simplification Plan - watchApp
status: Planned
progress:
  - "[ ] Phase 0: Stabilize repository lifecycle and message semantics"
  - "[ ] Phase 1: Remove dead state and synchronous suspend noise"
  - "[ ] Phase 2: Simplify startup/theme abstractions"
  - "[ ] Phase 3: Build hygiene and dependency cleanup"
  - "[ ] Phase 4: Battery/perf validation on wearable"
---

# Code Simplification Plan - watchApp

## Scope
- Module: `watchApp` (Wear OS companion)
- Goal: simplify data-layer flow, remove duplicated initialization/state bloat, and improve battery/performance behavior.

## High-Signal Findings (evidence)
1. **Repository initialization duplicated at multiple call-sites**
   - `WatchSyncListenerService.onCreate()`
   - `WatchSyncListenerService.onMessageReceived()`
   - `MainActivity.onCreate()`
   - `WearApp()` `LaunchedEffect`
2. **Path semantic collision risk in message flow**
   - watch->phone and phone->watch both use `REQUEST_SYNC_NOW_MESSAGE_PATH` in current flows.
3. **Persisted cache fields not consumed by app logic/UI**
   - `WatchSyncCacheState.lastSyncedAtEpochSec`
   - `WatchSyncCacheState.schemaVersion`
   - `WatchSyncCacheState.lastError`
4. **Synchronous code exposed as suspend + duplicated computation**
   - `WatchOtpProvider.buildCodes()` has no suspension points.
   - `nextCodeAt(nowEpochSec)` computed twice per item.
5. **Low-value abstraction wrapper**
   - `WatchCapabilityRegistrar` is a single-call object from `WatchApplication`.
6. **Naming and build hygiene**
   - `WatchSyncSyncError` naming stutter.
   - release minify disabled; unused appdirs dependency likely removable.
7. **30fps wall-clock loop without lifecycle/ambient gating**
   - `MainActivity.WearApp()` updates every 33ms continuously.

## Simplification Roadmap

### Phase 0: Stabilize repository lifecycle and message semantics
- [ ] Make repository initialization idempotent and centralize to one authoritative entrypoint.
- [ ] Clarify directionality of sync message paths:
  - separate request path(s) for watch->phone and phone->watch if both are needed.
- [ ] Add logging/tests for message flow to avoid loops.

### Phase 1: Remove dead state and synchronous suspend noise
- [ ] Remove unused cache fields or surface them meaningfully in UI.
- [ ] Rename `WatchSyncSyncError` -> `WatchSyncError`.
- [ ] Convert `WatchOtpProvider.buildCodes` to non-suspend and cache repeated values locally.

### Phase 2: Simplify startup/theme abstractions
- [ ] Inline or reduce `WatchCapabilityRegistrar` abstraction.
- [ ] Simplify watch theme token access if CompositionLocal indirection is constant-only.
- [ ] Ensure `Preview` path does not require live repository/filesystem.

### Phase 3: Build hygiene and dependency cleanup
- [ ] Remove unused dependencies (`kotlin.multiplatform.appdirs` if unreferenced).
- [ ] Enable release minification/optimization and keep rules verification.

### Phase 4: Battery/perf validation on wearable
- [ ] Gate high-frequency update loop with lifecycle/ambient awareness.
- [ ] Consider lower cadence updates when in ambient/background.
- [ ] Validate battery impact during 10-15 minute OTP browsing session.

## Success Criteria
- Single clear init path and clean message semantics.
- Lean cache model with only meaningful persisted data.
- Reduced per-frame/per-second CPU overhead on watch.
- Cleaner module boundaries and naming consistency.
