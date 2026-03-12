---
name: Encrypted Backup Support Plan
status: Not Started
progress:
  - "[ ] Phase 0 - Extend backup payload format to support encrypted entries"
  - "[ ] Phase 1 - Add encrypted export path in BackupService"
  - "[ ] Phase 2 - Add encrypted restore path in BackupService"
  - "[ ] Phase 3 - Surface encryption choice in Compose UI"
  - "[ ] Phase 4 - Surface encryption choice in CLI"
  - "[ ] Phase 5 - Tests and validation"
---

# Encrypted Backup Support Plan

## Goal

Add an option for users to create encrypted backups alongside the existing plaintext backup workflow.

Currently, `BackupPayload` stores plaintext `otpauth://` URIs — even though accounts are stored encrypted at rest (per-account AES-GCM with a PBKDF2-derived key from the user's passkey). The export flow decrypts every URI before writing the backup file.

This plan adds a user-facing toggle so the backup file can instead carry the **already-encrypted** account data directly from storage, without introducing any new encryption scheme.

### Design constraint

> We should **not** encrypt the URIs for backup in any new or special way.
> Just let them remain encrypted as they already are in storage — each account is encrypted with passkey + PBKDF2 + AES-GCM.

This means:

- **Encrypted backup** = export `StoredAccount` entries (encryptedURI + salt + label) as-is.
- **Plaintext backup** = existing behavior: decrypt each URI before export.
- **Restore from encrypted backup** = user must provide the passkey that was used at encryption time so each entry can be re-imported.

---

## Current state (relevant code)

### Storage model

```
StoredAccount
├── accountID: Uuid          // derived from salt
├── accountLabel: String     // "issuer:name"
├── salt: String             // hex-encoded 16-byte salt
└── encryptedURI: String     // hex-encoded AES-GCM ciphertext
```

Each account's URI is encrypted with a key derived via **PBKDF2-SHA256(passkey, salt, 200 iterations, 256 bits)** and then AES-GCM encrypted.

Reference: `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/storage/StorageUtils.kt`

### Backup payload (v1 — plaintext only)

```kotlin
data class BackupPayload(
    val schemaVersion: Int = 1,
    val createdAt: Long,
    val accounts: List<String>,  // plaintext otpauth:// URIs
)
```

Reference: `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/BackupPayload.kt`

### Export flow

`BackupService.createBackup()` calls `twoFacLib.exportAccountURIs()` which:

1. Iterates over in-memory `StoredAccount` list.
2. For each account: derives key from passkey + account salt → decrypts encryptedURI → returns plaintext URI.
3. Collects plaintext URIs into `BackupPayload.accounts`.

Reference: `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/BackupService.kt`

### Restore flow

`BackupService.restoreBackup()`:

1. Downloads blob, decodes `BackupPayload`.
2. Validates each plaintext URI via `OtpAuthURI.parse()`.
3. Calls `twoFacLib.addAccount(uri)` for each — which re-encrypts with the current passkey.

---

## Proposed changes

### Phase 0 — Extend backup payload format to support encrypted entries

**Bump schema version to 2** with backward-compatible decoding.

Add a new serializable model for encrypted account entries:

```kotlin
@Serializable
data class EncryptedAccountEntry(
    val accountLabel: String,
    val salt: String,          // hex-encoded salt used for PBKDF2 key derivation
    val encryptedURI: String,  // hex-encoded AES-GCM ciphertext
)
```

Update `BackupPayload`:

```kotlin
@Serializable
data class BackupPayload(
    val schemaVersion: Int = 2,
    val createdAt: Long,
    val encrypted: Boolean = false,
    val accounts: List<String> = emptyList(),                       // plaintext URIs (when encrypted == false)
    val encryptedAccounts: List<EncryptedAccountEntry> = emptyList(), // encrypted entries (when encrypted == true)
)
```

Rules:

- `encrypted == false` → `accounts` is populated (plaintext URIs), `encryptedAccounts` is empty. This is the same as current v1 behavior.
- `encrypted == true` → `encryptedAccounts` is populated, `accounts` is empty.
- Schema version 1 payloads are still decoded as plaintext (backward compatible).
- Schema version 2 payloads use the `encrypted` flag to determine which field to read.

Update `BackupPayloadCodec.decode()` to accept both schema version 1 and 2.

#### Files to change

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/BackupPayload.kt` — add `EncryptedAccountEntry`, add `encrypted` and `encryptedAccounts` fields.
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/BackupPayloadCodec.kt` — accept schema version 1 and 2, validate that the correct field is populated based on `encrypted` flag.

### Phase 1 — Add encrypted export path in BackupService

Add a method to `TwoFacLib` that exports raw `StoredAccount` data without decryption:

```kotlin
suspend fun exportStoredAccounts(): List<StoredAccount>
```

This simply returns the in-memory account list (already loaded on unlock) without any decryption.

Update `BackupService.createBackup()` to accept an encryption mode parameter:

```kotlin
suspend fun createBackup(providerId: String, encrypted: Boolean = false): BackupResult<BackupDescriptor>
```

When `encrypted == true`:

1. Call `twoFacLib.exportStoredAccounts()` instead of `exportAccountURIs()`.
2. Map each `StoredAccount` to `EncryptedAccountEntry(accountLabel, salt, encryptedURI)`.
3. Build `BackupPayload(schemaVersion = 2, encrypted = true, encryptedAccounts = entries)`.

When `encrypted == false`:

1. Existing behavior: call `twoFacLib.exportAccountURIs()` for plaintext URIs.
2. Build `BackupPayload(schemaVersion = 2, encrypted = false, accounts = uris)`.

#### Files to change

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/TwoFacLib.kt` — add `exportStoredAccounts()`.
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/BackupService.kt` — add `encrypted` parameter to `createBackup()`, branch on it.

### Phase 2 — Add encrypted restore path in BackupService

Update `BackupService.restoreBackup()` to handle both payload types:

**Plaintext payload** (`encrypted == false`):

- Existing behavior: parse each plaintext URI → `addAccount(uri)`.

**Encrypted payload** (`encrypted == true`):

1. Read `encryptedAccounts` from the payload.
2. For each `EncryptedAccountEntry`:
   a. Reconstruct a `StoredAccount` (derive accountID from salt, use provided label/salt/encryptedURI).
   b. Save the `StoredAccount` directly to storage via `storage.saveAccount()`.
3. This preserves the original encryption — the accounts are stored as-is without needing the passkey at restore time for decryption.

**Important nuance**: On restore of encrypted entries, the accounts are written with their _original_ encryption. This means:

- The user must use the **same passkey** that was active when the backup was created in order to later unlock and use those accounts.
- If the user's current passkey differs from the backup's passkey, the restored accounts will fail to decrypt at use time.
- The restore flow should **warn the user** that encrypted backups require the same passkey.

Alternative approach (more user-friendly, can be evaluated during implementation):

- At restore time, ask the user for the passkey that was used to create the backup.
- Attempt to decrypt each `EncryptedAccountEntry` using that passkey to validate it works.
- Then re-encrypt each URI with the **current** passkey before saving.
- This adds a validation step but allows restoring encrypted backups even when the passkey has changed.

#### Files to change

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/BackupService.kt` — branch restore logic based on `payload.encrypted`.
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/TwoFacLib.kt` — optionally add a method to import `StoredAccount` objects directly, or to decrypt-then-re-encrypt with current passkey.
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/storage/Storage.kt` — ensure `saveAccount(StoredAccount)` is available (it likely already is).

### Phase 3 — Surface encryption choice in Compose UI

Add a toggle or option in the backup section of `SettingsScreen`:

- When the user initiates a manual backup, present a choice:
  - **Plaintext backup** (current default) — readable export, no passkey needed to restore.
  - **Encrypted backup** — secrets remain encrypted, requires same passkey to use after restore.
- Pass the chosen mode to `BackupService.createBackup(providerId, encrypted)`.
- When restoring, detect `encrypted` flag in the payload and show appropriate messaging:
  - For encrypted backups: inform the user that they must be using the same passkey that was active when the backup was created.

#### Files to change

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt` — add encryption toggle to backup UI section.
- Potentially a new composable for backup options dialog/bottom-sheet.

### Phase 4 — Surface encryption choice in CLI

Update CLI backup commands to accept an `--encrypted` flag:

```
twofac backup create --provider local --encrypted
twofac backup restore --provider local <backup-id>
```

- `--encrypted` flag on create: passes `encrypted = true` to `BackupService.createBackup()`.
- Restore auto-detects whether the backup is encrypted from the payload and prints appropriate messages.

#### Files to change

- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/commands/BackupCommand.kt` — add `--encrypted` option to create subcommand.

### Phase 5 — Tests and validation

#### Unit tests (`sharedLib`)

1. **Codec round-trip tests**:
   - Encode plaintext payload → decode → verify accounts match.
   - Encode encrypted payload → decode → verify encryptedAccounts match.
   - Decode v1 payload (backward compatibility) → verify plaintext accounts.
   - Reject payload where both `accounts` and `encryptedAccounts` are populated.

2. **BackupService tests with fake transport**:
   - `createBackup(encrypted = false)` → verify payload contains plaintext URIs.
   - `createBackup(encrypted = true)` → verify payload contains encrypted entries with correct salt/encryptedURI from storage.
   - `restoreBackup()` of plaintext payload → verify accounts added via `addAccount()`.
   - `restoreBackup()` of encrypted payload → verify `StoredAccount` entries written to storage.

3. **Backward compatibility**:
   - A v1 backup file (no `encrypted` field) decodes as plaintext with `encrypted = false`.

#### Integration / manual tests

- Create plaintext backup → restore → verify OTP codes match.
- Create encrypted backup → restore on same vault with same passkey → verify OTP codes match.
- Create encrypted backup → attempt restore with different passkey → verify appropriate error or warning.
- Create backup on one platform → restore on another platform → verify cross-platform compatibility.

---

## Payload format examples

### Plaintext backup (v2, encrypted = false)

```json
{
  "schemaVersion": 2,
  "createdAt": 1741790400,
  "encrypted": false,
  "accounts": [
    "otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&digits=6&period=30",
    "otpauth://totp/AWS:bob@example.com?secret=ABCDEF1234567890&issuer=AWS&digits=6&period=30"
  ],
  "encryptedAccounts": []
}
```

### Encrypted backup (v2, encrypted = true)

```json
{
  "schemaVersion": 2,
  "createdAt": 1741790400,
  "encrypted": true,
  "accounts": [],
  "encryptedAccounts": [
    {
      "accountLabel": "GitHub:alice@example.com",
      "salt": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4",
      "encryptedURI": "2f5c3e1a9b8d7f4e2c1a9b8d7f4e2c1a..."
    },
    {
      "accountLabel": "AWS:bob@example.com",
      "salt": "f1e2d3c4b5a6f1e2d3c4b5a6f1e2d3c4",
      "encryptedURI": "8a7b6c5d4e3f2a1b8a7b6c5d4e3f2a1b..."
    }
  ]
}
```

### Legacy v1 backup (still supported for restore)

```json
{
  "schemaVersion": 1,
  "createdAt": 1741790400,
  "accounts": [
    "otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub&digits=6&period=30"
  ]
}
```

---

## Architecture decisions

1. **No new encryption mechanism**: encrypted backups reuse the existing per-account PBKDF2 + AES-GCM encryption that is already used for at-rest storage. The `salt` and `encryptedURI` fields are carried over verbatim from `StoredAccount`.

2. **Schema version bump**: v2 adds the `encrypted` flag and `encryptedAccounts` field. v1 payloads remain fully supported for restore (treated as plaintext).

3. **Transport-agnostic**: the encryption choice is at the payload level, not the transport level. Both local and cloud transports can carry either plaintext or encrypted payloads. This is orthogonal to any transport-level encryption (e.g., Google Drive or iCloud encryption).

4. **Passkey coupling**: encrypted backups are tied to the passkey used at creation time. This is an intentional trade-off — we avoid adding a separate backup password or key exchange mechanism and instead reuse the existing storage encryption as-is.

5. **Label visibility**: `accountLabel` remains in cleartext in encrypted backups. This allows the UI to display which accounts are in a backup without requiring decryption. The sensitive data (the OTP secret within the URI) is protected by the encryption.

---

## Interaction with existing plans

- **Plan 00 (Architecture)**: mentioned `BackupCrypto` as a future abstraction for encrypt/decrypt bytes. This plan intentionally avoids adding a new `BackupCrypto` layer — the encryption is already done at storage time and we simply preserve it in the backup payload.

- **Plan 13 (Rollout)**: the encrypted backup feature is orthogonal to the multi-transport rollout. It can be implemented on top of any transport (local, iCloud, Google Drive). The `encrypted` parameter is added to `BackupService.createBackup()` which all transports already use.

---

## Open questions to resolve during implementation

1. **Restore with different passkey**: should the restore flow require the original passkey and re-encrypt with the current one, or should it write the encrypted entries as-is (requiring the same passkey)? The re-encrypt approach is more user-friendly but adds complexity.

2. **Default encryption mode**: should encrypted backup be the default for cloud transports? Plan 00 suggested "encrypted-by-default for cloud providers" — this feature could enable that by defaulting `encrypted = true` for non-local transports.

3. **Mixed payloads**: should we support a payload that contains both plaintext and encrypted accounts? The current design says no (either all plaintext or all encrypted) for simplicity. This can be revisited if a use case emerges.

4. **accountID in encrypted entries**: should `EncryptedAccountEntry` include `accountID`? It can be derived from `salt` (as done in `StorageUtils`), so omitting it avoids redundancy. But including it could simplify restore logic and deduplication.
