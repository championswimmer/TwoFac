# Extensible Backup Transports Plan

## Objective
Add a provider-agnostic backup system for TwoFac where core logic is shared, and transport plugins can sync backup files to services like Google Drive and Apple iCloud (plus any future file-sync provider).

This plan is intentionally designed to support both:
1. plaintext backup files (for local/manual workflows), and
2. encrypted backup files (recommended default for cloud sync).

## Design Principles
- **Shared contracts in `sharedLib`**: transport API shape, manifest model, orchestration use-cases.
- **Provider SDK code outside `sharedLib`**: Google/Apple SDK dependencies stay in `composeApp` target source sets.
- **Transport-agnostic backup format**: same file payload regardless of provider.
- **Encrypted-by-default for cloud providers**: transport never receives raw secrets unless explicitly using plaintext mode.
- **Small, composable interfaces**: add new providers by implementing one adapter.

---

## Architecture (target state)

### 1) Core abstractions (`sharedLib/commonMain`)

Create a new package:

`sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/`

#### `BackupTransport` (common adapter interface)
```kotlin
interface BackupTransport {
    val id: String // e.g. "gdrive", "icloud"
    suspend fun isAvailable(): Boolean
    suspend fun listBackups(): BackupResult<List<BackupDescriptor>>
    suspend fun upload(request: UploadBackupRequest): BackupResult<BackupDescriptor>
    suspend fun download(backupId: String): BackupResult<BackupBlob>
    suspend fun delete(backupId: String): BackupResult<Unit>
}
```

#### `BackupResult`
A sealed result wrapper (success/failure with typed error codes) so UI/CLI can map failures consistently.

#### `BackupDescriptor`
Metadata only (id, transportId, createdAt, updatedAt, byteSize, schemaVersion, encryption info).

#### `BackupBlob`
Raw payload bytes + metadata (contentType, checksum, optional etag/revision).

#### `BackupPayloadCodec`
Serializes/deserializes account snapshots to/from a versioned file format.
- v1 payload includes:
  - schemaVersion
  - createdAt
  - appVersion (optional)
  - accounts: list of exportable account entries

#### `BackupCrypto`
Encrypt/decrypt bytes using existing crypto primitives and a backup-specific key derivation context.

#### `BackupService`
Orchestrates backup/restore:
- read accounts from `Storage`
- encode payload
- optional encrypt
- call selected `BackupTransport`
- restore path: download -> decrypt -> decode -> write/import accounts

### 2) Provider implementations (`composeApp`)

Create a new package:

`composeApp/src/commonMain/kotlin/tech/arnav/twofac/backup/transports/`

With platform-specific expect/actual provider clients where needed:
- Android/JVM: Drive REST client wrapper
- iOS: iCloud Drive/CloudKit wrapper

Planned transport classes:
- `GDriveBackupTransport` (Google Drive `appDataFolder` scoped file storage)
- `ICloudBackupTransport` (Apple private app container sync; implementation may use iCloud Drive app container first, CloudKit follow-up)
- `LocalFileBackupTransport` (manual file export/import path; useful fallback and tests)

### 3) Registration and selection

Add a transport registry abstraction:
```kotlin
interface BackupTransportRegistry {
    fun all(): List<BackupTransport>
    fun get(id: String): BackupTransport?
}
```

DI wiring:
- register available transports per platform
- expose only transports where `isAvailable()` is true

### 4) UI/CLI integration points

- `composeApp`: Backup settings screen for provider auth, backup now, restore from selected backup.
- `cliApp`: local-file transport first, cloud providers optional later if auth flow is feasible.

---

## File format and security decisions

## Backup file format (`twofac-backup-v1.json` before encryption)
- versioned JSON for readability and migration safety.
- deterministic field naming for diff/debug.
- no provider-specific fields inside payload.

### Encryption mode
- Default for cloud transports: encrypted payload.
- Keep plaintext mode only for explicit manual export/import use-cases.
- Store non-sensitive metadata separately in `BackupDescriptor` when provider supports it.

### Integrity
- Include checksum of ciphertext/plaintext payload in descriptor/metadata.
- Verify checksum before decode/restore.

### Restore safety
- dry-run parse validation before mutating storage.
- fail atomically if payload is invalid.
- optional merge mode later; first version can implement replace-or-skip strategy with explicit user choice.

---

## Provider-specific notes (research-backed)

### Google Drive
- Use `appDataFolder` for app-private hidden backups.
- Scope: `drive.appdata` for least privilege.
- Keep each backup as a separate file (timestamped name) + metadata for listing/restore.

### Apple iCloud
- Prefer app-private storage path for backup data (not user-editable documents).
- Start with a minimal implementation that supports upload/list/download/delete semantics through a single provider client.
- Preserve transport contract so backend can be swapped between iCloud Drive container and CloudKit without changing shared backup orchestration.

---

## Step-by-step implementation plan

1. **Create backup domain models in `sharedLib`**
   - Add `BackupTransport`, `BackupResult`, `BackupDescriptor`, `BackupBlob`, request models.
   - Add unit tests for result/error mapping and descriptor serialization.

2. **Add payload codec (`BackupPayloadCodec`)**
   - Define v1 JSON schema.
   - Implement encode/decode and version checks.
   - Add tests for round-trip and schema rejection.

3. **Add backup crypto helper (`BackupCrypto`)**
   - Reuse existing crypto primitives.
   - Add encrypt/decrypt tests and wrong-key failure tests.

4. **Implement `BackupService` orchestration**
   - `createBackup(transportId, encryptionMode)`
   - `listBackups(transportId)`
   - `restoreBackup(transportId, backupId)`
   - Add unit tests using fake transport and fake storage.

5. **Implement `LocalFileBackupTransport` first**
   - Use existing file access patterns for Compose/CLI.
   - Add integration tests for upload/list/download/delete in local sandbox.

6. **Implement `GDriveBackupTransport`**
   - Add provider client wrapper + auth/session glue.
   - Store in `appDataFolder`, map file metadata to `BackupDescriptor`.
   - Add transport contract tests (mock/fake HTTP) and manual smoke test.

7. **Implement `ICloudBackupTransport`**
   - Add Apple provider client wrapper with same contract mapping.
   - Validate behavior for availability/offline/errors.
   - Add transport contract tests + manual device smoke test.

8. **Add transport registry + DI wiring**
   - Register available transports per platform.
   - Add tests that unavailable providers are filtered out.

9. **Expose backup/restore flows in app surfaces**
   - Compose settings page actions.
   - CLI local transport commands.
   - Add UI/CLI tests for happy path and common failures.

10. **Migration + rollout hardening**
   - Document backup schema v1.
   - Add telemetry hooks (success/failure/error code only; no secrets).
   - Add restore compatibility tests to protect future schema upgrades.

---

## Test strategy (minimal but complete)

- **sharedLib common tests**
  - codec round-trip
  - crypto round-trip
  - backup service orchestration with fake transport
- **composeApp platform tests**
  - transport adapters contract tests per provider
- **manual checks**
  - create encrypted backup -> list -> restore on same account set
  - restore with wrong key
  - offline/list/download error mapping

---

## Suggested initial delivery slices

### Slice A (foundation)
- shared contracts, payload codec, crypto helper, backup service, local transport.

### Slice B (Google Drive)
- auth + `GDriveBackupTransport` + UI integration.

### Slice C (iCloud)
- `ICloudBackupTransport` + iOS integration.

This sequence keeps the architecture extensible while delivering user-visible value early.
