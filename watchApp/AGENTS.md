# watchApp AGENTS.md

`watchApp` is the Android Wear OS companion application. 

## What this module does
It provides a specialized, offline-capable interface for Android smartwatches. It syncs securely with the primary Android app via Google Play Services Wearable APIs, caches the secrets locally on the watch, and provides a continuous ticker for generating and displaying the OTP codes via Compose for Wear OS.

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
  - `datalayer/`: Manages capability registration and the `WatchSyncListenerService` which processes background updates from the phone.
  - `storage/`: `WatchSyncSnapshotRepository` handles saving the received data to persistent storage.
  - `otp/`: `WatchOtpProvider` uses `sharedLib` to convert synced URIs into real-time codes within a coroutine ticker.
  - `ui/`: Compose Wear OS screens including `OtpPagerScreen`, `OtpAccountScreen`, and `EmptyState`.
  - `presentation/`: Standard `MainActivity` and theme configuration.
