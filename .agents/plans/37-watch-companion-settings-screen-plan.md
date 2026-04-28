---
name: Watch Companion Settings Screen
status: Planned
progress:
  - "[ ] Phase 0 - Lock the shared settings scope and platform-specific sync semantics"
  - "[ ] Phase 1 - Add settings entry points and screen state models for Wear OS and watchOS"
  - "[ ] Phase 2 - Implement Wear OS settings actions for sync, last sync status, and app version"
  - "[ ] Phase 3 - Implement watchOS settings actions for sync, last sync status, and app version"
  - "[ ] Phase 4 - Add targeted tests and validate the watch experiences"
---

# Watch Companion Settings Screen

## Goal

Add a lightweight settings screen to both watch companions:

1. `watchApp` (Wear OS)
2. `iosApp/watchApp` + `iosApp/watchAppExtension` (Apple Watch)

The screen should cover the same basic user-facing tasks on both platforms:

1. fetch or sync from the phone
2. show the last fetch or sync time
3. show the watch app version

## Current state

### Wear OS (`watchApp`)

The Wear OS app currently has:

1. an OTP pager experience when snapshot data exists
2. an empty-state screen with a sync/request button
3. persisted sync metadata in `WatchSyncSnapshotRepository`, including `lastSyncedAtEpochSec`

What is missing:

1. no dedicated settings route or overflow/settings entry point
2. no reusable status surface for last sync time outside the empty state
3. no visible app version string
4. sync actions are only exposed when the watch has no accounts loaded

### Apple Watch (`iosApp/watchAppExtension`)

The Apple Watch app currently has:

1. a paged OTP view
2. an empty state that already shows `lastSyncTime`
3. a refresh button that only reloads the latest available payload on the watch

What is missing:

1. no dedicated settings screen or navigation into one
2. no app version display
3. sync and status details disappear once accounts exist
4. there is no explicit watch-to-phone sync request path today; the watch mainly consumes payloads already pushed by the iPhone companion

## Product direction

Ship one small settings surface per watch platform with matching intent, while allowing the underlying sync behavior to stay platform-specific.

### Shared settings content

Both settings screens should include:

1. **Sync from phone**
   - a primary action that requests or refreshes companion data
   - inline feedback for in-progress, success, and failure states
2. **Last sync**
   - formatted human-readable timestamp
   - clear fallback copy when sync has never completed
3. **Version**
   - app version string from platform-native bundle/build metadata

### Platform-specific sync meaning

1. **Wear OS**
   - the settings action should reuse the existing `WatchCompanionRegistrar.requestInitialization()` path
   - this is already a real watch-to-phone sync request over Play Services
2. **Apple Watch**
   - the settings action should become a real request to the iPhone companion, not just a local refresh of cached payload
   - the existing `refreshFromLatestAvailableData()` behavior can remain as a local fallback, but the plan should treat explicit watch-initiated sync as the desired end state

## UX shape

### Wear OS

Keep the OTP pager as the primary destination and add a clear route into settings from both:

1. the synced OTP experience
2. the empty state

Likely UX options to choose from during implementation:

1. a pager page dedicated to settings
2. a secondary screen reachable from a button/icon
3. a simple vertical list screen with cards for sync status and version

The implementation should prefer the smallest addition that feels native to Wear Compose and does not disrupt the OTP-first flow.

### Apple Watch

Keep the OTP pager as the main screen and add a dedicated settings destination that is reachable whether or not accounts are present.

Likely UX options to choose from during implementation:

1. a `NavigationStack` with a toolbar/menu entry to settings
2. a trailing page/tab dedicated to settings
3. a simple list-based settings screen pushed from the main content

The implementation should prefer the smallest SwiftUI-native pattern that works well on watchOS without overcomplicating the current layout.

## Architecture plan

### Phase 0 - Lock the shared settings scope and platform-specific sync semantics

1. Confirm that v1 scope is limited to:
   - sync/fetch from phone
   - last sync time
   - app version
2. Confirm that no account-management controls are part of this first settings screen.
3. Lock the meaning of the primary action on each platform:
   - Wear OS: send sync request to paired phone companion
   - Apple Watch: add a watch-to-phone sync request path, with local refresh fallback
4. Lock where the screen is entered from in each watch app.
5. Lock copy for:
   - never synced
   - sync in progress
   - sync succeeded
   - companion unavailable or unreachable

### Phase 1 - Add settings entry points and screen state models for Wear OS and watchOS

1. Add a dedicated settings destination for `watchApp`.
2. Add a dedicated settings destination for `iosApp/watchAppExtension`.
3. Extract or introduce small UI state models so settings content does not duplicate sync-status formatting logic.
4. Ensure both apps can surface settings when:
   - there are synced accounts
   - there are no synced accounts yet

### Phase 2 - Implement Wear OS settings actions for sync, last sync status, and app version

1. Reuse `WatchSyncSnapshotRepository.state` to show the persisted last sync timestamp.
2. Reuse `WatchCompanionRegistrar.requestInitialization()` as the primary sync action.
3. Move sync-result messaging out of `EmptyState` into a reusable state holder or settings-specific UI so the same feedback model can be shown from multiple entry points.
4. Expose the watch app version from Android build metadata, most likely via `BuildConfig.VERSION_NAME` with optional `VERSION_CODE` if desired.
5. Keep the empty state focused on the missing-data message while pointing users to the same settings/sync affordance.

### Phase 3 - Implement watchOS settings actions for sync, last sync status, and app version

1. Extend `WatchConnectivityManager` with a dedicated user-initiated sync action.
2. Add an explicit watch-to-phone request flow if the iPhone companion does not already accept one.
3. Keep `refreshFromLatestAvailableData()` as a fallback that rehydrates the latest cached payload.
4. Reuse `lastSyncTime` and `lastSyncError` in a settings-specific status section that stays available even when accounts are present.
5. Expose the app version from `Bundle.main`, using `CFBundleShortVersionString` and optionally `CFBundleVersion`.
6. Keep the main OTP display simple and avoid scattering sync details across both the empty state and the new settings screen unless necessary.

### Phase 4 - Add targeted tests and validate the watch experiences

1. **Wear OS**
   - verify settings entry points render in empty and non-empty states
   - verify last sync formatting handles null and non-null timestamps
   - verify sync action reflects registrar results
2. **Apple Watch**
   - verify settings screen renders with and without accounts
   - verify version and last sync data display correctly
   - verify the user-initiated sync action updates status for success and failure paths
3. Manual validation:
   - paired phone available
   - paired phone unavailable
   - never synced state
   - previously synced state
   - version visible on device/simulator

## Files likely impacted during implementation

### Wear OS

- `watchApp/src/main/java/tech/arnav/twofac/watch/presentation/MainActivity.kt`
- `watchApp/src/main/java/tech/arnav/twofac/watch/ui/EmptyState.kt`
- `watchApp/src/main/java/tech/arnav/twofac/watch/ui/*` (new settings UI)
- `watchApp/src/main/java/tech/arnav/twofac/watch/datalayer/WatchCompanionRegistrar.kt`
- `watchApp/src/main/java/tech/arnav/twofac/watch/storage/WatchSyncSnapshotRepository.kt`
- `watchApp/build.gradle.kts`

### Apple Watch

- `iosApp/watchAppExtension/WatchExtensionContentView.swift`
- `iosApp/watchAppExtension/WatchConnectivityManager.swift`
- `iosApp/watchAppExtension/TwoFacWatchApp.swift`
- `composeApp/src/iosMain/kotlin/tech/arnav/twofac/companion/IosCompanionSyncCoordinator.kt`
- `iosApp/watchApp/Info.plist`

## Out of scope for this plan

1. adding full account-management controls to the watch settings screen
2. changing the main OTP paging model beyond what is needed to reach settings
3. redesigning the broader phone-to-watch sync architecture outside the narrow watch-initiated request needed for settings
4. adding deep diagnostics, developer menus, or advanced sync logs

## Summary

The safest path is to add a small, always-available settings destination to both watch companions and keep the visible feature set aligned across Wear OS and watchOS.

The main implementation difference is sync initiation: Wear OS already has a watch-to-phone request path, while Apple Watch still needs one. Once that gap is closed, both settings screens can present the same three core capabilities without complicating the OTP-first watch experience.
