---
name: Code Simplification Plan - sharedLib
status: Completed
progress:
  - "[x] Phase 0: Lock correctness with tests"
  - "[x] Phase 1: Fix URI/HOTP correctness issues"
  - "[x] Phase 2: Remove duplicated conversion/JSON/crypto setup"
  - "[x] Phase 3: Reduce backup/watchsync surface bloat"
  - "[x] Phase 4: API cleanup and migration notes"
---

# Code Simplification Plan - sharedLib

## Scope
- Module: `sharedLib`
- Goal: reduce API and package bloat while first fixing correctness risks in OTP URI parsing and model round-trips.

## Research Notes (steelman review — 2026-03-31)

Each task below was reviewed against actual code and researched for KMP-specific concerns.
Tasks that were dropped or scoped down are explained inline.

---

## High-Signal Findings (evidence)

1. **HOTP parse counter is ignored (correctness risk)**
   - `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/uri/OtpAuthURI.kt`
   - `counter` is parsed in the `Type.HOTP` branch but discarded — the `HOTP(...)` constructor
     call has a comment: `// Note: The counter is not used in the HOTP constructor`.
   - Compounded by `TwoFacLib.getAllAccountOTPs()` always calling `hotp.generateOTP(0)` —
     hardcoded zero regardless of the provisioned URI counter.
   - RFC 4226 §7.2 and the Google Key-URI spec both confirm `counter` in an `otpauth://hotp/`
     URI sets the **initial client-side counter state**. Discarding it means every HOTP account
     provisioned with `counter > 0` will generate the wrong code from the start.

2. **Magic default period literal in builder**
   - `OtpAuthURI.Builder.build()` uses `period != 30L` instead of `period != DEFAULT_PERIOD`.
   - `DEFAULT_PERIOD` is defined two lines above in the same `object`. Minor but inconsistent.

3. **Duplicated Algo string→enum mapping**
   - Identical `when (algo.uppercase()) { "SHA1" → ... "SHA256" → ... "SHA512" → ... }` blocks
     appear in:
     - `importer/adapters/AuthyImportAdapter.kt`
     - `importer/adapters/TwoFasImportAdapter.kt`
     - `uri/OtpAuthURI.kt`
     - `src/nativeMain/kotlin/libtwofac.kt`

4. **Repeated `Json` instances for same configuration**
   - All three importer adapters independently declare:
     ```kotlin
     private val json = Json { ignoreUnknownKeys = true; isLenient = true }
     ```
   - `WatchSyncSnapshotCodec` uses a **different** config (`encodeDefaults = true;
     ignoreUnknownKeys = true`) — these two configs must remain separate.
   - `IosWatchSyncHelper` has yet another instance (`ignoreUnknownKeys = true` only, no
     `isLenient`, no `encodeDefaults`) — this is **subtly wrong** (see Finding 7 below).

5. **Backup DTO mirror duplication**
   - `BackupProvider` data class mirrors every capability field of `BackupTransport` interface 1:1,
     plus adds `isAvailable`. The single mapping site is `BackupTransportRegistry.providerInfo()`.
   - The issue is not the existence of `BackupProvider` (it IS needed as a snapshot DTO) but the
     absence of an extension function: adding a new capability to `BackupTransport` requires
     remembering to update `BackupProvider` *and* `providerInfo()` in two unrelated files.

6. **Multiple independent `DefaultCryptoTools(CryptographyProvider.Default)` instances**
   - `otp/HOTP.kt`: creates a **new instance per HOTP object** (worst case).
   - `TwoFacLib.kt`: one per TwoFacLib (acceptable).
   - `storage/StorageUtils.kt`: singleton via `object` (already correct).
   - Research confirmed: `CryptographyProvider.Default` is a thread-safe factory; multiple
     instances have minimal overhead. However, creating one per `HOTP` instance is semantically
     wrong — crypto tools should not be owned by the domain model. Injection or a shared
     internal singleton is the right KMP pattern.

7. **WatchSync codec duplication and subtle correctness bug**
   - `IosWatchSyncHelper.decodeWatchSyncPayloadString()` duplicates the schema-version check
     already in `WatchSyncSnapshotCodec.decode()`.
   - `IosWatchSyncHelper` uses `Json { ignoreUnknownKeys = true }` without `encodeDefaults = true`.
     In kotlinx.serialization, `encodeDefaults` defaults to `false`, meaning fields whose runtime
     value equals the declared default may be **omitted** from the encoded JSON. For
     `WatchSyncSnapshot.version`, this means the `version` field could be absent from iOS-generated
     payloads, causing the version check on the receiver to fail or fall back to a default that
     may not match. `WatchSyncSnapshotCodec` correctly sets `encodeDefaults = true`.

8. **Deprecated API still present**
   - `TwoFacLib.exportAccountURIs()` is a one-liner deprecated wrapper for
     `exportAccountsPlaintext()`. Zero internal callers (confirmed by `grep`). Appears in the
     ABI dump, occupying public surface for no reason.

9. **`BackupProviderCapabilityRules` in production code, used only by tests**
   - Confirmed: the only callers of `BackupProviderCapabilityRules.expectedCapabilitiesFor()`
     are in `BackupRestorePolicyTest.kt`. No production code path calls it. It is a test helper
     misplaced in `commonMain`.

---

## DROPPED TASK — Reasoning

### ~~Consolidate tiny backup model files~~
The backup package contains 12 files ranging from 9 to 186 lines. This task was evaluated and
**dropped** for the following reasons:

- In Kotlin (and KMP specifically), one-file-per-type is the idiomatic convention. Every major
  Kotlin library (kotlinx.coroutines, Compose, Ktor) follows this.
- Every file has a clear, distinct concern: transport interface, provider snapshot DTO, service
  orchestrator, payload format, codec, result sealed class, descriptor, blob, preferences, restore
  policy, capability rules. There is no incohesion.
- "Consolidating" would create a monolithic `BackupModels.kt` mixing unrelated types, harming
  discoverability and making ABI diffs harder to read.
- The files are small *because they do one thing* — that is a quality indicator, not a smell.
- Net change: negative. Reorganising 12 files adds diff noise, risks ABI surface changes, and
  gives no runtime or readability benefit.

---

## Simplification Roadmap

### Phase 0: Lock correctness with tests
- [ ] Add/expand tests for:
  - HOTP URI round-trip preserving counter semantics (currently broken — counter is discarded).
  - TOTP period default behavior (constant-based, not magic literal).
  - Importer algorithm parsing consistency across all adapters.

### Phase 1: Fix URI/HOTP correctness issues

- [ ] **Add `initialCounter: Long = 0` field to `HOTP` model** so the provisioned counter
  survives storage round-trips.
  - `OtpAuthURI.parse()` must pass the parsed counter to the constructor.
  - `OtpAuthURI.create()` must read it back (currently defaults to `builder.counter(0)`).
  - `TwoFacLib.getAllAccountOTPs()` must use `hotp.initialCounter` instead of hardcoded `0`.
  - StoredAccount round-trip: because the URI is stored encrypted, fixing parse/create is
    sufficient — no schema migration needed.

- [ ] **Replace hardcoded `30L` with `DEFAULT_PERIOD`** in `OtpAuthURI.Builder.build()`:
  ```kotlin
  // Before
  if (type == Type.TOTP && period != 30L) {
  // After
  if (type == Type.TOTP && period != DEFAULT_PERIOD) {
  ```

- [ ] Add regression tests for both issues.

### Phase 2: Remove duplicated conversion/JSON/crypto setup

- [ ] **Add `CryptoTools.Algo.fromString(value: String): Algo` companion helper** and migrate
  all four duplicated `when`-mapping sites to use it. A reasonable default (`SHA1`) or a
  nullable variant (`fromStringOrNull`) should handle unknown values consistently.

- [ ] **Extract shared importer JSON config** to one internal top-level constant (e.g.,
  `internal val ImporterJson = Json { ignoreUnknownKeys = true; isLenient = true }`) in the
  importer package, and reference it from all three adapters.
  - Do NOT merge this with `WatchSyncSnapshotCodec`'s config — those two configs are
    intentionally different (importer needs `isLenient`; codec needs `encodeDefaults = true`).

- [ ] **Centralize `DefaultCryptoTools` construction** into a shared internal singleton or
  require callers to inject it.
  - Remove `private val cryptoTools = DefaultCryptoTools(CryptographyProvider.Default)` from
    `HOTP.kt`. Instead, accept a `CryptoTools` parameter in the constructor (with a default
    that references the shared instance) — this is idiomatic KMP and makes the class testable.
  - `StorageUtils.kt` (object) is already effectively a singleton — no change needed.
  - `TwoFacLib.kt` instance is acceptable; it can remain.

### Phase 3: Reduce backup/watchsync surface bloat

- [ ] **Add `BackupTransport.toProvider(isAvailable: Boolean): BackupProvider` extension
  function** (in `BackupTransportRegistry.kt` or a new `BackupTransportExt.kt`).
  - `BackupTransportRegistry.providerInfo()` then becomes a one-liner map call.
  - When a new capability is added to `BackupTransport`, only `toProvider()` needs updating —
    the registry stays stable.
  - `BackupProvider` data class is kept as-is; it is the correct concrete DTO for UI consumption.

- [ ] **Fix `IosWatchSyncHelper` codec duplication and correctness bug**:
  - Replace the standalone `Json { ignoreUnknownKeys = true }` with delegation to
    `WatchSyncSnapshotCodec`. Since the codec operates on `ByteArray`, the helper can convert:
    ```kotlin
    fun encodeWatchSyncSnapshot(snapshot: WatchSyncSnapshot): String =
        WatchSyncSnapshotCodec.encode(snapshot).decodeToString()
    fun decodeWatchSyncPayloadString(payload: String): WatchSyncSnapshot =
        WatchSyncSnapshotCodec.decode(payload.encodeToByteArray())
    ```
  - This also fixes the missing `encodeDefaults = true` bug in the iOS path.

### Phase 4: API cleanup and migration notes

- [ ] **Remove deprecated `exportAccountURIs()`** — zero internal callers, safe to delete.
  Update `sharedLib/api/` ABI dumps afterwards.

- [ ] **Move `BackupProviderCapabilityRules` to test sources** (`commonTest`) since it is
  exclusively used by `BackupRestorePolicyTest`. Remove `@PublicApi` annotation and clean from
  production ABI surface.

- [ ] **Refresh ABI dumps** in `sharedLib/api/` after all above changes.

---

## Success Criteria
- HOTP accounts provisioned with `counter > 0` generate correct codes.
- `DEFAULT_PERIOD` constant used consistently; no magic `30L` literals.
- Algo parsing logic exists in exactly one place (`CryptoTools.Algo.fromString`).
- Importer JSON config is shared across adapters; watch-sync codec config remains separate.
- `HOTP` no longer owns its own `CryptoTools` instance.
- `BackupTransport → BackupProvider` mapping lives in one extension function.
- iOS watch-sync encode/decode path uses `encodeDefaults = true` (via codec delegation).
- `exportAccountURIs()` removed from public ABI.
- `BackupProviderCapabilityRules` lives in test sources only.
