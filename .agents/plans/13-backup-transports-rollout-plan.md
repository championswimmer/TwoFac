---
name: Backup Transports Rollout Plan
status: In Progress
progress:
  - "[x] Phase 0 - Refactor current single-transport code into a multi-transport registry"
  - "[x] Phase 1 - Define provider capability metadata and single-provider automatic restore policy"
  - "[ ] Phase 2 - Implement Apple iCloud transport and entitlement wiring"
  - "[ ] Phase 3 - Implement Google Drive appDataFolder transport across supported platforms"
  - "[ ] Phase 4 - Ship manual backup and restore UX for every available provider"
  - "[ ] Phase 5 - Add automatic restore guardrails, tests, and rollout hardening"
---

# Backup Transports Rollout Plan

## Goal

Add provider-based backup transports through dependency injection:

- `sharedLib` owns generic backup contracts and orchestration.
- Each app/platform module registers whichever concrete backup transports it can support.
- Multiple backup transports may coexist on the same platform.
- Manual backup and manual restore must work for every available provider.
- Automatic restore must be opt-in and tied to exactly one selected provider at a time.
- Watch apps stay out of scope because they already recover from their companion-phone sync path instead of acting as backup sources.

This plan updates the older architecture sketch in `00-backup-transports-architecture-plan.md` around the code that already exists today.

---

## Current repository state (relevant)

- Shared backup contracts already exist in `sharedLib`:
  - `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/BackupTransport.kt`
  - `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/BackupService.kt`
- Desktop currently registers a **single** `BackupTransport` and a `BackupService`:
  - `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/di/DesktopModules.kt`
- Compose settings currently assumes exactly one `BackupTransport`:
  - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`
- CLI backup commands are local-file only today:
  - `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/commands/BackupCommand.kt`
- Android and iOS already use additive Koin platform modules for optional platform services:
  - `composeApp/src/androidMain/kotlin/tech/arnav/twofac/di/AndroidModules.kt`
  - `composeApp/src/iosMain/kotlin/tech/arnav/twofac/di/IosModules.kt`
- Watch flows are already modeled as companion sync, not backup ownership:
  - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/companion/CompanionSyncCoordinator.kt`
  - `watchApp/src/main/java/tech/arnav/twofac/watch/storage/WatchSyncSnapshotRepository.kt`

### Consequences for this plan

1. This is **not** a greenfield backup design anymore; the work is mostly a refactor from single transport to transport registry plus new providers.
2. The platform-module pattern already fits the desired DI approach.
3. Watch targets should not register any backup providers.
4. The first user-visible refactor is in settings/UI and DI, not in payload format.

---

## External research summary

### Apple iCloud / CloudKit

Research used:

- Apple: <https://docs.developer.apple.com/tutorials/data/documentation/cloudkit/deciding-whether-cloudkit-is-right-for-your-app.md>
- Xcode iCloud services overview: <https://help.apple.com/xcode/mac/current/en.lproj/dev52452c426.html>

Key takeaways:

- Apple exposes multiple iCloud storage models:
  - **iCloud Documents** for synced files in the app ubiquity container
  - **iCloud key-value storage** for small preference-like state
  - **CloudKit** for app/user data in private/public/shared databases
- Key-value storage is too limited for backup snapshots.
- For app-private backup records with explicit list/upload/download/delete behavior, **CloudKit private database** is the best long-term fit.
- CloudKit requires Apple-native entitlements/container setup in Xcode, which fits `iosApp` + `composeApp/iosMain`.
- CloudKit base APIs provide full control, but that also means explicit handling of account availability, errors, conflict cases, and remote state tracking.
- Apple system backup cannot be triggered by the app; this feature must therefore be modeled as explicit app-level backup snapshots, not as a wrapper around device iCloud Backup.

### Google Drive `appDataFolder`

Research used:

- Google Drive app data guide: <https://developers.google.com/workspace/drive/api/guides/appdata>

Key takeaways:

- `appDataFolder` is a hidden, app-specific storage area suitable for non-user-facing backup files.
- It requires the least-privilege scope `https://www.googleapis.com/auth/drive.appdata`.
- Files in `appDataFolder`:
  - are not meant for normal user-facing Drive browsing,
  - cannot be shared,
  - cannot be moved between Drive spaces,
  - must be listed using `spaces=appDataFolder`.
- This is a good fit for app-private remote backups.
- Because Google Drive access is OAuth- and HTTP-based, the transport should be split into:
  - shared backup behavior in common code, and
  - platform-specific auth/session/token acquisition per app surface.
- The app must still preserve local/manual restore paths because remote app data can be cleared or become unavailable independently of local device state.

### Architecture implication from the research

- Manual backup/restore can support several providers at once.
- Automatic restore must select exactly one source-of-truth provider, otherwise the app risks ambiguous startup behavior and conflicting imports.
- Apple and Google need different platform glue, but both fit the same shared backup contract if transport state, auth, and remote markers are modeled cleanly.

---

## Recommended architecture decisions

### 1) Keep provider-neutral contracts in `sharedLib`

Use `sharedLib` for the generic domain and orchestration only:

- `BackupTransport`
- `BackupTransportRegistry` or equivalent multi-provider lookup abstraction
- `BackupProviderInfo` / capability metadata for UI and CLI
- `AutomaticRestoreSelection` or similar settings model
- `BackupService` methods that operate on an explicit provider/transport ID

The shared layer should not depend on Google or Apple SDKs.

### 2) Use additive DI registration per platform

Each platform module should be free to register zero, one, or many transports:

- Desktop may register `local` and `gdrive-appdata`
- iOS may register `icloud` and `gdrive-appdata`
- Android may register `gdrive-appdata`
- Web/Wasm may register `gdrive-appdata` only if auth and JS interop are viable
- CLI may keep `local` first and add `gdrive-appdata` later if a secure installed-app auth flow is acceptable
- Watch apps register none

Do **not** model backup support as a single nullable `BackupTransport`.

### 3) Separate manual operations from automatic restore

- **Manual backup**: allowed for every available provider
- **Manual restore**: allowed for every available provider
- **Automatic restore**:
  - off by default
  - allowed only for providers advertising that capability
  - persisted as a single selected provider ID
  - never runs from multiple providers

### 4) Preserve watch exclusion explicitly

Watch apps do not own the authoritative vault and do not need backup transports.
Their recovery path remains:

- phone app restore/import
- then existing companion sync updates watch state

### 5) Prefer manual-first cloud rollout

Initial transport work should focus on:

- connect / availability
- upload backup now
- list snapshots
- restore a selected snapshot
- delete a selected snapshot

Automatic restore should come only after provider identity, remote markers, and user-confirmed restore rules are stable.

---

## Delivery phases

### Phase 0 - Refactor current single-transport code into a multi-transport registry

1. Introduce a shared multi-provider abstraction.
   - Add `BackupTransportRegistry` or equivalent provider lookup API in `sharedLib`.
   - Expose provider metadata (`id`, display name, manual backup support, manual restore support, automatic restore support, auth required, availability status).

2. Refactor `BackupService`.
   - Keep payload encode/decode responsibilities in shared code.
   - Change service APIs so they operate on a chosen transport/provider rather than assuming a singular DI-resolved transport.
   - Preserve existing local backup behavior as the baseline implementation.

3. Update DI wiring.
   - Replace single `BackupTransport` registration patterns with additive registration.
   - Shared/common code should depend on the registry or a provider list, not `getOrNull<BackupTransport>()`.

4. Update UI callers.
   - `SettingsScreen` must stop assuming one provider.
   - Replace the current "Local Backup" card with a provider-aware backup section.

5. Update CLI plumbing.
   - Keep current local backup working.
   - Prepare commands to resolve providers through the same registry model later, even if only `local` is registered at first.

6. Tests.
   - Shared tests for zero/one/many provider registration.
   - Shared tests for provider lookup by ID.
   - Regression tests that the existing local backup path still works.

### Phase 1 - Define provider capability metadata and single-provider automatic restore policy

7. Add a shared settings model for backup preferences.
   - selected automatic restore provider ID
   - per-provider last successful backup/restore metadata
   - per-provider remote marker metadata for deduping automatic restore

8. Define restore behavior clearly.
   - Manual restore should always require explicit provider selection plus explicit snapshot selection.
   - Automatic restore should be disabled by default.
   - Automatic restore should never merge data from multiple providers.

9. Define safe import behavior.
   - For a non-empty vault, do not silently auto-import remote data.
   - Prefer a recovery-style prompt or explicit confirmation flow before applying remote data.
   - Keep restore validation and decode steps atomic before mutating storage.

10. Add provider capability rules.
    - `local`: manual backup + manual restore, no automatic restore
    - `icloud`: manual backup + manual restore, automatic restore eligible
    - `gdrive-appdata`: manual backup + manual restore, automatic restore eligible

11. Add remote marker strategy.
    - Google Drive: file ID + modified time and/or revision metadata
    - Apple CloudKit: record name + modified timestamp and/or server change marker
    - These markers are required before any automatic restore implementation

12. Tests.
    - Single-provider automatic restore selection tests
    - Conflict-prevention tests that multiple automatic sources cannot be enabled together

### Phase 2 - Implement Apple iCloud transport and entitlement wiring

13. Choose Apple backend.
    - Prefer **CloudKit private database** for app-private backup snapshots.
    - Do not use key-value storage for backups.
    - Avoid tying the shared contract to CloudKit-specific types.

14. Build Apple-native provider client in Apple source sets.
    - Provider implementation should live behind Apple-native code (`iosMain`, with host app support from `iosApp`).
    - Shared code only sees `BackupTransport`.

15. Define Apple backup record shape.
    - one logical backup snapshot per record
    - metadata: logical backup ID, createdAt, schema version, checksum, payload size
    - payload stored in a record-friendly blob/asset field as appropriate

16. Wire entitlements and container setup.
    - Enable the needed iCloud / CloudKit capability in the Apple host app
    - Keep container naming/configuration documented in the plan as implementation proceeds

17. Manual provider operations.
    - availability check
    - upload snapshot
    - list snapshots
    - download selected snapshot
    - delete selected snapshot

18. Failure handling.
    - signed-out iCloud account
    - unavailable container
    - account change
    - offline state

19. macOS feasibility gate.
    - The current repository has iOS native hosting (`iosApp`) but not a native macOS Apple host module.
    - Because current desktop packaging is JVM-based, macOS iCloud support needs a verified entitlement/bridge story before registration can be enabled there.
    - Do not block iPhone/iOS delivery on unresolved macOS packaging work; keep macOS parity as an explicit follow-up subphase if needed.

### Phase 3 - Implement Google Drive `appDataFolder` transport across supported platforms

20. Build a shared Drive transport core.
    - Shared code handles backup naming, metadata mapping, and remote marker comparison.
    - Platform code handles sign-in, access token acquisition, refresh, and HTTP engine wiring.

21. Scope Drive access correctly.
    - Use `drive.appdata` scope only.
    - Use `appDataFolder` space only.
    - Keep backups private and app-scoped.

22. Model backup files as discrete snapshots.
    - one file per backup snapshot
    - metadata includes file ID, created/modified time, checksum, logical backup ID, schema version

23. Roll out by platform capability.
    - Android: highest-priority Google Drive candidate
    - Desktop JVM: viable if installed-app/browser OAuth flow is acceptable
    - iOS: viable if browser/native OAuth glue is acceptable
    - Wasm/Web: viable only after confirming Google Identity JS interop + CSP fit
    - CLI: optional follow-up, not MVP by default

24. Provider operations.
    - auth/connect
    - upload backup now
    - list backups
    - restore selected backup
    - delete backup

25. Remote marker support for later automatic restore.
    - Track the remote snapshot already consumed by auto-restore logic
    - Ignore unchanged remote state

26. Tests and QA.
    - revoked consent
    - empty `appDataFolder`
    - multiple devices creating snapshots
    - interrupted upload/download

### Phase 4 - Ship manual backup and restore UX for every available provider

27. Redesign the Compose backup section.
    - replace the current single-provider local backup card
    - enumerate all available providers
    - show provider availability and sign-in state
    - present per-provider actions

28. Manual restore flow.
    - choose provider
    - list available snapshots
    - inspect snapshot metadata
    - confirm restore
    - trigger companion sync after successful phone restore where relevant

29. Manual backup flow.
    - unlock if required
    - choose provider
    - create snapshot
    - surface success/failure cleanly

30. UX rules.
    - local/manual provider stays available where already supported
    - unsupported providers should either be hidden or shown with a precise availability reason
    - watch apps show no backup UI

31. CLI follow-up.
    - retain current local backup commands
    - if/when more providers land in CLI, add provider selection as an argument instead of duplicating command trees

### Phase 5 - Add automatic restore guardrails, tests, and rollout hardening

32. Add a dedicated "Automatic restore source" setting.
    - default: Off
    - selectable only from providers that advertise automatic restore capability
    - enforce single selection in both UI and persisted settings

33. Restrict automatic restore to safe lifecycle points.
    - app start after unlock
    - explicit recovery flow
    - empty-vault startup path
    - never from multiple providers in one session

34. Add "already consumed" protection.
    - only restore when remote marker is newer than the last consumed marker
    - never repeatedly import the same snapshot

35. Protect non-empty local state.
    - if local accounts already exist, do not silently apply remote data
    - prompt the user or require a recovery path

36. Expand the test matrix.
    - sharedLib policy tests
    - provider registration tests
    - platform-specific auth/availability tests
    - manual device tests for Android, iOS, desktop, and watch non-participation regression

37. Document setup and rollout.
    - Google OAuth client/scopes
    - Apple entitlements/containers
    - provider capability matrix by platform
    - migration notes from current single-local-provider UI

---

## Suggested execution slices

1. **PR 1**: multi-transport registry refactor, DI cleanup, settings/CLI plumbing
2. **PR 2**: provider preference model, single-provider automatic restore policy, tests
3. **PR 3**: first Google Drive implementation on the first supported target
4. **PR 4**: Apple iCloud implementation on iPhone/iOS
5. **PR 5**: manual multi-provider UX polish and provider metadata surfacing
6. **PR 6**: automatic restore rollout and hardening
7. **PR 7**: macOS iCloud parity once entitlement/bridge feasibility is proven

---

## Planned file-level hotspots

### Shared domain

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/*`
- shared backup settings/persistence location to be added

### Compose common UI / DI

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/di/modules.kt`

### Platform registrations

- `composeApp/src/androidMain/kotlin/tech/arnav/twofac/di/AndroidModules.kt`
- `composeApp/src/iosMain/kotlin/tech/arnav/twofac/di/IosModules.kt`
- `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/di/DesktopModules.kt`
- `composeApp/src/wasmJsMain/kotlin/...` backup/auth integration files to be introduced if web support is included

### Existing local backup codepaths

- `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/backup/LocalFileBackupTransport.kt`
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/commands/BackupCommand.kt`
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/backup/*`

### Apple host integration

- `iosApp/` Xcode project and related entitlement/capability files

---

## Open questions to resolve while implementing

1. For manual restore, should the first rollout import into the existing vault, replace the vault, or offer both with confirmation?
2. Which Google Drive targets are in MVP after Android: Desktop, iOS, Web, or some subset?
3. What is the cleanest macOS iCloud story for this repository: entitling the existing Compose Desktop app, or introducing an Apple-native macOS host path later?
4. Should automatic backup exist in the first rollout, or should the first rollout stay manual-backup + optional automatic-restore only?
