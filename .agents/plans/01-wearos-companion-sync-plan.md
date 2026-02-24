# Wear OS Companion App Plan (Phone-Paired, Secret Sync, OTP Display)

## Goal
Build `watchApp` into a **paired companion** for the Android app (`composeApp` android target), not a standalone app.

The watch app should:
1. receive/sync 2FA account secrets from the phone periodically,
2. persist synced data locally on watch,
3. generate/show OTP codes on watch even when temporarily disconnected,
4. support vertical swipe navigation (top-to-bottom/bottom-to-top) where each page shows one account + code.

---

## Current repository state (relevant)
- `watchApp` is a starter Compose-for-Wear skeleton (`MainActivity.kt`) with no data-layer sync yet.
- `watchApp` already depends on:
  - `play-services-wearable`
  - `sharedLib`
  - `kstore` + appdirs (good foundation for local persisted cache)
- `watchApp` manifest currently marks app as standalone (`com.google.android.wearable.standalone=true`) and must be changed for companion behavior.
- `composeApp` Android app currently has no wearable data-layer integration code.
- `sharedLib` already exposes the core primitives needed for this feature:
  - `TwoFacLib.exportAccountURIs()` (phone-side export for sync)
  - OTP generation from stored secrets via `getAllAccountOTPs()` / OTP models.

---

## External research summary (internet)

### Data-layer architecture patterns
From Google’s Wear DataLayer sample (`android/wear-os-samples`):
- Use `DataClient` for state/data replication,
- Use `MessageClient` for fire-and-forget or command-style triggers,
- Use `CapabilityClient` for discovering reachable paired nodes and installed app capability,
- Handle incoming events via listeners and/or `WearableListenerService`.

### Capability-based companion discovery
From sample `wear.xml` files and listener patterns:
- Declare capabilities on each side (`mobile` / `wear` style).
- Query capabilities with `CapabilityClient.FILTER_REACHABLE` before send.
- Keep `android_wear_capabilities` through shrinking with `wear_keep.xml`.

### Swipe UX on Wear Compose
From AndroidX Wear Compose samples:
- `VerticalPager` + `VerticalPagerScaffold` is the direct pattern for up/down swipe paging.
- Pair pager with `rememberPagerState` and pager snap behavior.

### Companion helper reference
From Horologist `DataLayerAppHelper`:
- Companion app install/start/status flows are capability- and node-driven.
- Start/launch actions should be foreground-initiated and timeout-handled.

---

## Recommended target architecture

## 1) Responsibility split
- `sharedLib`: all reusable account/OTP logic + serialization helpers for sync payload model.
- `composeApp` (`androidMain`): source-of-truth publisher for watch sync + pairing/discovery UI/status.
- `watchApp`: Wear-only sync receiver, persistence, OTP UI.

## 2) Data model for sync
Create a watch sync payload model (in `sharedLib`, commonMain), e.g.:
- `WatchSyncSnapshot(version, generatedAtEpochSec, accounts: List<WatchSyncAccount>)`
- `WatchSyncAccount(accountId, issuer, accountLabel, otpAuthUri)`

Notes:
- Keep payload versioned from day 1.
- Include only fields required for OTP rendering on watch.
- Phone computes payload from `TwoFacLib.exportAccountURIs()` + display metadata.

## 3) Sync transport contract
- Use `DataClient` for snapshot replication path, e.g. `/twofac/sync/snapshot`.
- Use `MessageClient` optional command path, e.g. `/twofac/sync/request_now`, for watch-triggered refresh.
- Use `CapabilityClient` with app capabilities:
  - phone capability: `twofac_mobile`
  - watch capability: `twofac_watch`

## 4) Security model (minimum acceptable)
- In-transit: rely on Wearable Data Layer transport.
- At rest on watch: persist synced snapshot in watch local storage with encrypted secret fields where practical.
- Keep payload scope minimal (only needed secret material + display metadata).
- Never log otpAuth URIs or generated OTP values.

---

## Step-by-step implementation plan

## Phase A — Foundation and companion wiring
1. **Manifest and capability wiring**
   - `watchApp/src/main/AndroidManifest.xml`
     - change `com.google.android.wearable.standalone` to `false`.
   - add `res/values/wear.xml` in both Android phone app and watch app:
     - phone: `twofac_mobile`
     - watch: `twofac_watch`
   - add `res/raw/wear_keep.xml` in both apps to keep capabilities through shrink.

2. **Define sync constants and payload DTOs in `sharedLib`**
   - Add `watchsync` package with:
     - sync paths/capability constants,
     - serializable payload classes,
     - encode/decode helpers.
   - Add unit tests for payload versioning + roundtrip serialization.

3. **Phone-side Data Layer client scaffold (`composeApp/src/androidMain`)**
   - Add a small Android-only service/repository wrapper around:
     - `Wearable.getDataClient(context)`
     - `Wearable.getMessageClient(context)`
     - `Wearable.getCapabilityClient(context)`
   - Add node discovery method: reachable watch nodes with `twofac_watch` capability.

## Phase B — Phone publishes snapshots
4. **Build snapshot from current app data**
   - Add phone-side mapper that builds `WatchSyncSnapshot` from `TwoFacLib` data:
     - unlock guard,
     - export URIs + account labels/issuer.

5. **Publish snapshot to watch via `DataClient`**
   - Put data item at `/twofac/sync/snapshot` with incrementing timestamp for updates.
   - Mark urgent only for user-initiated sync; normal for periodic/background sync.

6. **Trigger points on Android app**
   - Trigger immediate sync after account-changing actions (add/import/edit/delete when added).
   - Trigger sync after successful unlock and initial account load.
   - Add periodic sync using `WorkManager` (minimum interval constraints) while app has at least one account.

7. **Optional manual sync action in Android settings**
   - Add "Sync to watch now" action + last sync status text.
   - Show basic pairing state (watch reachable / app installed capability present).

## Phase C — Watch receives and persists
8. **Watch listener implementation**
   - Add `WearableListenerService` in `watchApp` to receive:
     - data item updates for snapshot path,
     - message path for immediate sync requests (optional).
   - Decode payload safely; reject unknown versions with non-fatal error state.

9. **Watch local persistence**
   - Store latest valid snapshot in watch `KStore`.
   - Persist `lastSyncedAt` and schema version.
   - On app start, load snapshot from disk before trying network.

10. **Watch OTP provider layer**
   - Convert synced otpAuth URI to OTP model using shared logic.
   - Generate current OTP and next refresh epoch per account.
   - Drive a per-second/per-period refresh loop for visible page(s).

## Phase D — Wear UI (vertical swipe account pages)
11. **Replace skeleton greeting screen**
   - Implement `VerticalPager` UI (`VerticalPagerScaffold`) with one page per account.
   - Each page: issuer, account label, OTP code, countdown/progress, sync freshness indicator.

12. **UI states**
   - Empty state: "No accounts synced yet. Open phone app to sync."
   - Error state: malformed snapshot/version mismatch.
   - Stale-data badge if last sync exceeds threshold (e.g., >24h).

13. **Interaction polish**
   - Optional rotary support with pager snap behavior.
   - Haptic/visual cue on code rollover.

## Phase E — Pairing + install/launch UX on phone
14. **Pairing status card in Android app**
   - Show if Wear APIs available.
   - Show connected nodes and whether `twofac_watch` capability exists.
   - If no companion app capability, show "Install watch app" guidance.

15. **Optional launch companion flow**
   - Send `/twofac/app/start` message to watch capability node(s) to foreground watch app.
   - Keep this as best-effort; do not block sync.

## Phase F — Validation and rollout hardening
16. **Tests**
   - `sharedLib` unit tests for payload codec and mapping.
   - Android unit tests for phone snapshot builder and sync trigger logic.
   - Watch unit tests for payload decode + OTP mapping + UI state reducer.

17. **Manual QA matrix**
   - Fresh pair: install both apps, first sync works.
   - Account add/update/remove on phone propagates to watch.
   - Watch offline after sync still shows valid OTPs.
   - Reconnect updates snapshot.
   - App reinstall scenarios (phone/watch) recover gracefully.

18. **Security checklist**
   - no secret/OTP logs,
   - at-rest protection enabled for watch cache,
   - strict schema validation,
   - reject unknown payload versions safely.

---

## Suggested execution slices (small PR sequence)
1. **PR 1**: companion manifest/capabilities + shared payload DTO/codec/tests.
2. **PR 2**: phone DataLayer publisher + manual sync button + basic status.
3. **PR 3**: watch listener + local store + non-paged list UI.
4. **PR 4**: watch vertical pager OTP UI + stale/error states.
5. **PR 5**: periodic sync worker + polish + docs.

---

## File-level change map (planned)

### watchApp
- `src/main/AndroidManifest.xml` (set non-standalone, register listener service)
- `src/main/res/values/wear.xml` (watch capability)
- `src/main/res/raw/wear_keep.xml`
- `src/main/java/.../presentation/MainActivity.kt` (replace skeleton with pager-driven UI)
- `src/main/java/.../datalayer/*` (listener + repository + mapper)
- `src/main/java/.../storage/*` (snapshot cache wrapper)

### composeApp (android side)
- `src/androidMain/AndroidManifest.xml` (if service declarations needed)
- `src/androidMain/res/values/wear.xml` (phone capability)
- `src/androidMain/res/raw/wear_keep.xml`
- `src/androidMain/kotlin/.../wear/*` (phone sync publisher + pairing status)
- `src/commonMain/...` integration points in existing settings/home/viewmodel surface for manual trigger/status exposure

### sharedLib
- `src/commonMain/kotlin/.../watchsync/*` (payload models, constants, codec)
- `src/commonTest/kotlin/.../watchsync/*` (roundtrip/version tests)

---

## Sources consulted
- Wear OS Samples repository overview:
  - https://raw.githubusercontent.com/android/wear-os-samples/main/README.md
- DataLayer sample architecture and usage notes:
  - https://raw.githubusercontent.com/android/wear-os-samples/main/DataLayer/README.md
- DataLayer wearable listener/service example:
  - https://raw.githubusercontent.com/android/wear-os-samples/main/DataLayer/Wearable/src/main/java/com/example/android/wearable/datalayer/DataLayerListenerService.kt
- DataLayer application side clients/capability/message usage:
  - https://raw.githubusercontent.com/android/wear-os-samples/main/DataLayer/Application/src/main/java/com/example/android/wearable/datalayer/MainActivity.kt
- Capability declaration examples:
  - https://raw.githubusercontent.com/android/wear-os-samples/main/DataLayer/Application/src/main/res/values/wear.xml
  - https://raw.githubusercontent.com/android/wear-os-samples/main/DataLayer/Wearable/src/main/res/values/wear.xml
  - https://raw.githubusercontent.com/android/wear-os-samples/main/DataLayer/Wearable/src/main/res/raw/wear_keep.xml
- Wear Compose pager sample (vertical swipe pattern):
  - https://raw.githubusercontent.com/androidx/androidx/androidx-main/wear/compose/compose-material3/samples/src/main/java/androidx/wear/compose/material3/samples/PagerScaffoldSample.kt
- Horologist helper reference for node/install/start flows:
  - https://raw.githubusercontent.com/google/horologist/main/datalayer/core/src/main/java/com/google/android/horologist/data/apphelper/DataLayerAppHelper.kt
