# sharedLib simplification review

## Scope

- Reviewed `sharedLib/src/commonMain/kotlin`
- Focused on opportunities to reduce code, lower complexity, and simplify architecture
- Validated the current module baseline with `./gradlew --no-daemon :sharedLib:jvmTest`

## High-level observations

- `sharedLib` packs a lot of orchestration into a few large files, especially `TwoFacLib.kt`, `OtpAuthURI.kt`, and `BackupService.kt`
- The module already has good test coverage, but several responsibilities are still concentrated in facade/utility objects
- Most simplification opportunities come from consolidating duplicated flows rather than changing algorithms

## Highest-priority opportunities

### 1. Reuse the existing shared crypto singleton everywhere

**Category:** Reduce code  
**Files:** `TwoFacLib.kt`, `storage/StorageUtils.kt`, `crypto/SharedCryptoTools.kt`

`sharedLib` already defines `sharedCryptoTools`, but `TwoFacLib` and `StorageUtils` still create their own `DefaultCryptoTools(CryptographyProvider.Default)` instances. That duplicates construction logic and spreads crypto wiring across the module.

**Recommendation**

- Replace local `DefaultCryptoTools(...)` instantiations with `sharedCryptoTools`
- Keep explicit injection only where tests or alternate providers truly need it

**Why this matters**

- Removes repeated setup code
- Makes crypto usage more uniform
- Reduces the chance of behavior drifting between call sites

### 2. Split `TwoFacLib` by responsibility

**Category:** Simplify architecture  
**Files:** `TwoFacLib.kt`

`TwoFacLib` currently owns vault lock state, account CRUD, OTP generation, import orchestration, URI export, and encrypted-backup decryption helpers. At 300+ lines, it is becoming a god object for the module.

**Recommendation**

- Keep `TwoFacLib` as the public facade
- Move account export/import logic into dedicated collaborators
- Move account-to-display/account-to-code mapping into a focused account query service

**Why this matters**

- Lowers the amount of branching in one file
- Makes future features easier to place without growing the facade further
- Improves testability of individual behaviors

### 3. Consolidate repeated “derive key from passkey + salt” flows

**Category:** Reduce code  
**Files:** `TwoFacLib.kt`, `storage/StorageUtils.kt`

`TwoFacLib` repeats the same pattern in `getAllAccounts`, `getAllAccountOTPs`, `getDecryptedUriForAccount`, and `exportAccountsPlaintext`: get current passkey, combine it with account salt, derive signing key, then decrypt/convert. The same logic also appears in `StorageUtils.decryptURI`.

**Recommendation**

- Extract small internal helpers around signing-key derivation and decrypted URI lookup
- Prefer one shared internal path for “stored account -> decrypted otpauth URI”

**Why this matters**

- Removes repeated boilerplate
- Makes account-reading behavior easier to reason about
- Reduces the number of places that must change if key derivation evolves

### 4. Pull backup payload construction/parsing out of `BackupService`

**Category:** Reduce complexity  
**Files:** `backup/BackupService.kt`, `TwoFacLib.kt`, `backup/BackupPayload.kt`

`BackupService` does transport orchestration and also knows how to build plaintext payloads, transform encrypted accounts, normalize passkeys, decrypt encrypted entries, parse URIs, and deduplicate restores. That mixes transport coordination with payload-domain logic.

**Recommendation**

- Keep `BackupService` responsible for provider/transport workflows
- Move payload assembly/disassembly into a dedicated backup mapper/restorer component
- Reuse `TwoFacLib` only through narrow export/import APIs

**Why this matters**

- Shortens the main backup service flow
- Makes restore rules easier to test in isolation
- Avoids backup concerns leaking across transport and vault layers

### 5. Introduce helpers for `BackupResult` / `ImportResult` unwrapping

**Category:** Reduce complexity  
**Files:** `backup/BackupService.kt`, `backup/BackupRestorePolicy.kt`, `TwoFacLib.kt`, `importer/ImportResult.kt`, `backup/BackupResult.kt`

The code repeatedly checks `is Failure`, returns early, and then casts to `Success`. That is especially visible in `BackupService.inspectBackup` and `BackupService.restoreBackup`.

**Recommendation**

- Add common helpers such as `fold`, `map`, `flatMap`, or `getOrElse`
- Use them to replace manual cast-heavy branching

**Why this matters**

- Removes noisy control flow
- Makes success/failure paths more declarative
- Reduces repeated type checks and casts

### 6. Standardize JSON codec configuration

**Category:** Reduce code  
**Files:** `backup/BackupPayloadCodec.kt`, `watchsync/WatchSyncSnapshotCodec.kt`, `importer/ImporterJson.kt`

`ImporterJson` is already centralized, but backup and watch-sync each create their own similar `Json` instances. The current setup is still small, but it is already three separate codec configuration entry points for one module.

**Recommendation**

- Add shared internal JSON builders/factories for codec families
- Keep importer JSON separate where behavior intentionally differs
- Document why backup/watch-sync need `encodeDefaults = true`

**Why this matters**

- Reduces config duplication
- Makes serialization policy easier to change consistently
- Clarifies which differences are intentional versus accidental

### 7. Collapse importer adapter duplication with shared parsing helpers

**Category:** Reduce code  
**Files:** `importer/adapters/AuthyImportAdapter.kt`, `TwoFasImportAdapter.kt`, `EnteImportAdapter.kt`

The adapters repeat similar shapes: decode input, convert each entry to otpauth URIs, return failure when nothing valid remains, and wrap everything in nearly identical exception handling.

**Recommendation**

- Extract shared adapter helpers for:
  - JSON decode + error wrapping
  - “empty result becomes failure”
  - entry conversion with invalid-entry skipping
- Keep source-format-specific field mapping inside each adapter

**Why this matters**

- Removes repetitive scaffolding
- Makes each adapter mostly about format translation
- Eases addition of future import formats

### 8. Break up `OtpAuthURI.parse` into focused parsing stages

**Category:** Reduce complexity  
**Files:** `uri/OtpAuthURI.kt`

`OtpAuthURI.parse` does scheme validation, type parsing, label decoding, issuer reconciliation, parameter splitting, default resolution, and OTP object creation in one method. The behavior is correct, but the method is dense and harder to extend safely.

**Recommendation**

- Split parsing into helpers for:
  - type/path extraction
  - label parsing
  - query parameter parsing
  - OTP construction
- Keep the public API unchanged

**Why this matters**

- Makes the parser easier to read and review
- Improves failure-path clarity
- Supports adding more validation without growing one large function

### 9. Replace boolean-heavy backup capability modeling

**Category:** Simplify architecture  
**Files:** `backup/BackupProvider.kt`, `backup/BackupTransport.kt`, `backup/BackupProviderCapabilityRules.kt`

`BackupProvider` and `BackupTransport` express capabilities through multiple booleans such as `supportsManualBackup`, `supportsManualRestore`, `supportsAutomaticRestore`, and `requiresAuthentication`. This is workable now, but it spreads one concept across several flags and defaults.

**Recommendation**

- Model capabilities as a single grouped value instead of separate booleans
- Keep provider identity/availability separate from capability metadata

**Why this matters**

- Simplifies condition checks
- Makes provider state easier to evolve
- Reduces the likelihood of invalid flag combinations

### 10. Move conversion logic away from `StorageUtils`

**Category:** Simplify architecture  
**Files:** `storage/StorageUtils.kt`, `storage/StoredAccount.kt`, `uri/OtpAuthURI.kt`

`StorageUtils` currently mixes data-model conversion (`OTP -> StoredAccount`, `StoredAccount -> OTP`) with decryption helpers and its own crypto dependency. It behaves like both a mapper and a crypto utility.

**Recommendation**

- Move pure model conversion closer to `StoredAccount` or a dedicated mapper
- Keep raw crypto helpers in one internal crypto-oriented location
- Reduce the role of the current catch-all utility object

**Why this matters**

- Improves separation of concerns
- Makes file ownership more obvious
- Reduces utility-object sprawl

## Secondary opportunities

- `BackupRestorePolicy.evaluateAutomaticRestore` could be simplified by passing a small context object instead of multiple scalar inputs
- Passkey normalization with `takeIf(String::isNotBlank)` appears more than once and could be centralized
- `TwoFacLib.importAccounts` currently mixes parsing, add-account side effects, and summary construction in one block

## Suggested implementation order

1. Reuse `sharedCryptoTools`
2. Extract small helpers for signing-key derivation and result unwrapping
3. Split `TwoFacLib` and `BackupService` into narrower collaborators
4. Standardize codec and importer scaffolding
5. Revisit backup/provider capability modeling after the service split

## Expected payoff

- Less repeated crypto and serialization setup
- Smaller orchestration methods with clearer ownership boundaries
- Easier extension of import, backup, and URI parsing features without growing central files further
