# watchApp AGENTS.md

`watchApp` is the Android Wear OS companion application.

## What this module does
It provides a specialized, offline-capable interface for Android smartwatches. It syncs securely with the primary Android app via Google Play Services Wearable APIs, requests initialization from nearby phone companions, caches synced secrets locally on the watch, and renders pager-based OTP screens with a continuously updating countdown UI via Compose for Wear OS.

## Dependencies
Depends on `:sharedLib` for data structures and OTP generation logic.

## Platforms
- **Android Wear OS**

## Libraries Used
- [Compose for Wear OS](https://developer.android.com/training/wearables/compose) - Wearable-optimized UI toolkit for watch screens.
- [Play Services Wearable](https://developers.google.com/android/reference/com/google/android/gms/wearable/package-summary) - For `DataClient` and `MessageClient` communication with the phone.
- [KStore](https://github.com/xxfast/KStore) - For caching the synchronized account payload locally on the watch.

## Code Structure
- `src/main/java/tech/arnav/twofac/watch/`:
  - `WatchApplication.kt`: Registers the watch capability on startup so the phone app can discover the companion.
  - `datalayer/`: Capability registration, `WatchCompanionRegistrar` initialization requests, and `WatchSyncListenerService` background sync handling.
  - `storage/`: `WatchSyncSnapshotRepository` handles persisting snapshots plus sync-error state for malformed or unsupported payloads.
  - `otp/`: `WatchOtpProvider` uses `sharedLib` to convert synced URIs into real-time `WatchOtpEntry` values within a coroutine ticker.
  - `ui/`: Compose Wear OS screens including `OtpPagerScreen`, `OtpAccountScreen`, and `EmptyState`.
  - `presentation/`: `MainActivity` and theme configuration that switch between empty-state and synced-account experiences.
