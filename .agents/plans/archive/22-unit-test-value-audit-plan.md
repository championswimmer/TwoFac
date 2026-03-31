---
name: Unit Test Value Audit Plan
status: Complete
progress:
  - "[x] Inventory all Kotlin `*Test.kt` files and separate unit tests from instrumented tests"
  - "[x] Run a baseline `./gradlew --no-daemon check` and capture the current failing-test baseline"
  - "[x] Assess each unit test file for business value vs obvious or redundant coverage"
  - "[x] Remove the placeholder `composeApp` common test that only checks `1 + 2 == 3`"
  - "[x] Decide whether to annotate or delete the dormant `CryptoToolsTest.testRoundTripCreateSigningKey` case"
  - "[x] Consolidate low-signal repetition in the small set of CLI/UI helper tests called out below"
---

# Unit Test Value Audit Plan

## Objective

Go through every Kotlin unit test in the repository and make a blunt call on which tests are truly protecting behavior versus which ones are mostly checking obvious plumbing or placeholder logic.

This plan does **not** remove tests by itself. It records the audit and turns the cleanup work into a targeted roadmap.

## Baseline

- Repository validation command: `./gradlew --no-daemon check`
- Current baseline result: **fails before any changes**
- Existing failing test:
  - `:cliApp:linuxX64Test`
  - `tech.arnav.twofac.cli.commands.BackupCommandTest.testImportEncryptedWithWrongBackupPasskeyFailsNonZero[linuxX64]`
- Report locations from the baseline run:
  - `cliApp/build/reports/tests/linuxX64Test/index.html`
  - `cliApp/build/reports/tests/allTests/index.html`

## Assessment rubric

- **Keep**: protects non-trivial behavior, cross-platform logic, integration boundaries, or edge cases
- **Keep but fix inactive coverage**: the intent is valid, but part of the suite is currently not executing or is otherwise incomplete
- **Keep but trim obvious assertions**: useful test coverage, but contains repetitive or low-signal assertions that should be consolidated
- **Candidate for consolidation**: multiple tests are individually fine but the same intent can be expressed with fewer, stronger cases
- **Candidate for removal**: placeholder/tautological coverage that does not protect meaningful behavior

## Test inventory and raw assessment

### `sharedLib`

| Path | Assessment | Raw assessment |
| --- | --- | --- |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/TwoFacLibTest.kt` | Keep | Core library contract coverage. These tests guard lock/unlock state, error messages, storage lifecycle, and destructive operations. High value. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/backup/BackupPayloadCodecTest.kt` | Keep | Valuable codec/schema tests. Backup payload encoding and encryption bugs are expensive; this suite earns its keep. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/backup/BackupRestorePolicyTest.kt` | Keep | Good policy-level regression coverage. These rules are not obvious and are easy to break silently. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/backup/BackupServiceTest.kt` | Keep | One of the strongest suites in the repo. It exercises real backup/restore behavior, encrypted and plaintext paths, and duplicate-handling rules. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/backup/BackupTransportRegistryTest.kt` | Keep | Real value: registry lookup, unsupported transport handling, and cancellation/error propagation are the kind of failures users actually feel. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/crypto/CryptoToolsTest.kt` | Keep but fix inactive coverage | The encryption/decryption round-trip test is useful. The second function (`testRoundTripCreateSigningKey`) is currently unannotated and therefore dead weight until fixed or removed. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/crypto/EncodingTest.kt` | Keep | Encoding/decoding edge cases are worth pinning down. This is not “obvious” coverage because malformed input handling matters. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/importer/AuthyImportAdapterTest.kt` | Keep | Import adapters are format-sensitive and brittle. Good, concrete regression value. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/importer/EnteImportAdapterTest.kt` | Keep | Worth keeping because import/export compatibility is a user-facing promise and this suite covers unsupported encryption too. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/importer/ImportIntegrationTest.kt` | Keep | High-value end-to-end import coverage. Protects the adapter-to-library seam rather than tiny internals. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/importer/TwoFasImportAdapterTest.kt` | Keep | Same story as other adapters: format translation bugs are easy to introduce and painful in production. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/otp/HOTPTest.kt` | Keep | RFC-vector coverage is exactly what these algorithms should be tested with. High confidence per test line. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/otp/OTPEquivalenceTest.kt` | Keep | Business logic, not boilerplate. Useful for dedupe/equality semantics. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/otp/TOTPTest.kt` | Keep | Another strong suite with RFC vectors and time-window validation. Definitely required. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/storage/MemoryStorageTest.kt` | Keep | Solid contract tests for CRUD, mutability, and identity behavior. This is useful protection for a core abstraction. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/storage/StorageUtilsTest.kt` | Keep | Good round-trip coverage for stored-account conversion and encryption boundaries. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/theme/ThemeColorTest.kt` | Keep | Small but legitimate parser/validation coverage. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/theme/TimerSemanticsTest.kt` | Keep | Boundary-condition tests around timing semantics are meaningful and non-trivial. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/theme/TwoFacThemeTokensTest.kt` | Keep but trim obvious assertions | The range check is useful. The fixed token anchor checks are acceptable, but the suite is tiny enough that this is more about maintainability than raw value. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/uri/OtpAuthURITest.kt` | Keep | URI parsing/building is full of edge cases and interoperability concerns. Good value. |
| `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/watchsync/WatchSyncSnapshotCodecTest.kt` | Keep | Small, focused codec tests with real value because schema drift across devices is costly. |

### `cliApp`

| Path | Assessment | Raw assessment |
| --- | --- | --- |
| `cliApp/src/commonTest/kotlin/tech/arnav/twofac/cli/commands/BackupCommandTest.kt` | Keep | High-value CLI integration coverage. It validates command behavior, prompts, encryption paths, and failure behavior. |
| `cliApp/src/commonTest/kotlin/tech/arnav/twofac/cli/commands/DisplayCommandTest.kt` | Keep but trim obvious assertions | The tests are valid, but this is a small command with repetitive output-contains assertions. Worth keeping, but it could be tighter. |
| `cliApp/src/commonTest/kotlin/tech/arnav/twofac/cli/commands/InfoCommandTest.kt` | Keep | Simple smoke coverage for the info command. Not deep, but still useful because it protects a user-facing entrypoint with little maintenance cost. |
| `cliApp/src/commonTest/kotlin/tech/arnav/twofac/cli/commands/StorageCommandTest.kt` | Keep | Good value because it exercises confirmation behavior and destructive command flows. |
| `cliApp/src/commonTest/kotlin/tech/arnav/twofac/cli/di/KoinVerificationTest.kt` | Keep | DI wiring tests are boring until wiring breaks. This one is worth the small cost. |

### `composeApp`

| Path | Assessment | Raw assessment |
| --- | --- | --- |
| `composeApp/src/commonTest/kotlin/tech/arnav/twofac/ComposeAppCommonTest.kt` | Candidate for removal | Pure placeholder test. `assertEquals(3, 1 + 2)` adds zero product confidence and should not exist. |
| `composeApp/src/commonTest/kotlin/tech/arnav/twofac/components/otp/OTPCardTest.kt` | Keep | Focused logic coverage around OTP interval behavior. Small but real. |
| `composeApp/src/commonTest/kotlin/tech/arnav/twofac/onboarding/OnboardingAutoShowResolverTest.kt` | Keep | Non-trivial decision logic with real UX impact. Good value. |
| `composeApp/src/commonTest/kotlin/tech/arnav/twofac/onboarding/OnboardingGuideRegistryTest.kt` | Keep | Registry/platform override logic is exactly the kind of thing that benefits from tests. |
| `composeApp/src/commonTest/kotlin/tech/arnav/twofac/onboarding/OnboardingViewModelTest.kt` | Keep | Useful state-management coverage for a view model with user-progress implications. |
| `composeApp/src/commonTest/kotlin/tech/arnav/twofac/qr/QRCodePayloadValidationTest.kt` | Keep | QR parsing/validation has plenty of edge cases and deserves tests. |
| `composeApp/src/commonTest/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModelSessionManagerTest.kt` | Candidate for consolidation | The behavior under test matters, but seven narrow cases mostly toggle the same three flags. This can likely be expressed with fewer stronger scenarios. |
| `composeApp/src/commonTest/kotlin/tech/arnav/twofac/wear/WatchSyncCoordinatorTest.kt` | Keep | Worth keeping because cross-device sync logic is not obvious and regressions are easy to miss manually. |
| `composeApp/src/commonTest/kotlin/tech/arnav/twofac/session/IosBiometricSessionManagerTest.kt` | Keep | Platform-specific but still valuable unit-level behavior coverage. |
| `composeApp/src/wasmJsTest/kotlin/tech/arnav/twofac/session/BrowserSessionManagerTest.kt` | Keep | High-value browser credential/session coverage with meaningful fallback/error behavior. |

## Non-unit tests found during the audit

These are real tests, but they are **not** part of the unit-test cleanup scope:

| Path | Type | Assessment |
| --- | --- | --- |
| `composeApp/src/androidInstrumentedTest/kotlin/tech/arnav/twofac/session/AndroidBiometricSessionManagerInstrumentedTest.kt` | Android instrumented test | Keep. It belongs in instrumentation because it depends on platform/runtime behavior. |

## Bottom line

### Tests that are clearly pulling their weight

The backup, OTP, importer, storage, URI, browser-session, onboarding, and core library suites are the backbone of this repository's confidence. Those tests are not “obvious”; they protect real behavior and should stay.

### Tests that feel weak or wasteful

1. `composeApp/src/commonTest/kotlin/tech/arnav/twofac/ComposeAppCommonTest.kt`
   - This is the only truly worthless test in the repo.
   - It is a placeholder and should be removed.

2. `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/crypto/CryptoToolsTest.kt`
   - Half useful, half dead.
   - One test runs and is fine.
   - One test never runs because it is missing `@Test`.
   - Decision needed: either annotate it and make it intentional, or delete it.

3. `cliApp/src/commonTest/kotlin/tech/arnav/twofac/cli/commands/DisplayCommandTest.kt`
   - Useful but a bit shallow.
   - Keep it, but tighten it.

4. `composeApp/src/commonTest/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModelSessionManagerTest.kt`
   - Covers worthwhile behavior, but it is over-sliced.
   - Best candidate for consolidation rather than removal.

5. `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/theme/TwoFacThemeTokensTest.kt`
   - Not bad, just slightly assertion-heavy for what it protects.
   - Lowest urgency of the “trim” candidates.

## Recommended cleanup order

1. Remove `ComposeAppCommonTest`
2. Resolve the dormant unannotated `CryptoToolsTest.testRoundTripCreateSigningKey`
3. Consolidate `AccountsViewModelSessionManagerTest`
4. Trim repetitive output assertions in `DisplayCommandTest`
5. Leave the rest alone unless new evidence appears

## Scope guardrails

- Do **not** gut meaningful tests just because they are small.
- Do **not** remove RFC-vector, codec, importer, backup, or cross-platform state tests.
- Prefer removing placeholder coverage and consolidating repetition over deleting legitimate behavior tests.
