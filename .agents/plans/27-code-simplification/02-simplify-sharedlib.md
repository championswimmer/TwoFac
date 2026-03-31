---
name: Code Simplification Plan - sharedLib
status: Planned
progress:
  - "[ ] Phase 0: Lock correctness with tests"
  - "[ ] Phase 1: Fix URI/HOTP correctness issues"
  - "[ ] Phase 2: Remove duplicated conversion/JSON/crypto setup"
  - "[ ] Phase 3: Reduce backup/watchsync surface bloat"
  - "[ ] Phase 4: API cleanup and migration notes"
---

# Code Simplification Plan - sharedLib

## Scope
- Module: `sharedLib`
- Goal: reduce API and package bloat while first fixing correctness risks in OTP URI parsing and model round-trips.

## High-Signal Findings (evidence)
1. **HOTP parse counter is ignored (correctness risk)**
   - `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/uri/OtpAuthURI.kt`
   - `counter` parsed in `Type.HOTP` branch but not represented in resulting `HOTP` model.
2. **Magic default period literal in builder**
   - `OtpAuthURI.Builder.build()` uses `period != 30L` instead of `DEFAULT_PERIOD`.
3. **Duplicated Algo string->enum mapping**
   - `importer/adapters/AuthyImportAdapter.kt`
   - `importer/adapters/TwoFasImportAdapter.kt`
   - `uri/OtpAuthURI.kt`
   - `src/nativeMain/kotlin/libtwofac.kt` (multiple sites)
4. **Repeated Json instances for same configuration**
   - identical lenient JSON setup in importer adapters.
5. **Backup DTO mirror duplication**
   - `backup/BackupTransport.kt` + `backup/BackupProvider.kt` + `backup/BackupTransportRegistry.kt::providerInfo()` copy capability fields 1:1.
6. **Multiple independent `DefaultCryptoTools(CryptographyProvider.Default)` instances**
   - `otp/HOTP.kt`, `TwoFacLib.kt`, `storage/StorageUtils.kt`.
7. **Deprecated API still present**
   - `TwoFacLib.exportAccountURIs()` is deprecated wrapper.

## Simplification Roadmap

### Phase 0: Lock correctness with tests
- [ ] Add/expand tests for:
  - HOTP URI round-trip preserving counter semantics.
  - TOTP period default behavior (constant-based).
  - importer algorithm parsing consistency.

### Phase 1: Fix URI/HOTP correctness issues
- [ ] Define and implement one clear HOTP counter policy:
  - either model-level `initialCounter`, or
  - explicit parse/create helper preserving counter metadata.
- [ ] Replace hardcoded `30L` with `DEFAULT_PERIOD` in `OtpAuthURI.Builder`.
- [ ] Add regression tests for both issues.

### Phase 2: Remove duplicated conversion/JSON/crypto setup
- [ ] Add `CryptoTools.Algo.fromString(...)` helper and migrate all duplicated `when` mappings.
- [ ] Extract shared importer JSON config (`ignoreUnknownKeys + isLenient`) to one internal constant.
- [ ] Centralize default crypto tools construction into a shared internal singleton/factory.

### Phase 3: Reduce backup/watchsync surface bloat
- [ ] Consolidate tiny backup model files (where cohesion remains clear).
- [ ] Rework `BackupProvider` mirroring:
  - either derive from transport via mapper extension, or
  - replace with slimmer view model containing only computed fields.
- [ ] Remove watchsync decode duplication by routing helper logic through one codec path.

### Phase 4: API cleanup and migration notes
- [ ] Remove deprecated `exportAccountURIs()` after compatibility check.
- [ ] Evaluate moving low-value capability rules objects to tests if production code never uses them.
- [ ] Refresh ABI dumps in `sharedLib/api/`.

## Success Criteria
- HOTP/TOTP URI behaviors are explicit and test-covered.
- Algo parsing logic exists in one place only.
- JSON/crypto instantiation duplication is removed.
- Backup/watchsync package file surface is smaller and easier to navigate.
