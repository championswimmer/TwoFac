---
name: Code Simplification Plan - cliApp
status: Planned
progress:
  - "[ ] Phase 0: Baseline and safety net"
  - "[ ] Phase 1: Remove redundant abstraction layers"
  - "[ ] Phase 2: Fix command dispatch and storage coroutine patterns"
  - "[ ] Phase 3: Package/layout flattening and cleanup"
  - "[ ] Phase 4: Verification and docs updates"
---

# Code Simplification Plan - cliApp

## Scope
- Module: `cliApp`
- Goal: reduce ceremony, remove useless abstractions, and fix delegation bugs while preserving CLI behavior.

## Bloat / Abstraction Findings (evidence)

### 1. Pass-through ViewModel layer ✅ CONFIRMED — Remove it
- **File:** `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/viewmodels/AccountsViewModel.kt`
- Only delegates 3 methods to `TwoFacLib` with no state, no transformation, and no lifecycle management.
- **Research verdict:** ViewModel is a UI/lifecycle pattern (Android MVVM). In a CLI there is no screen, no configuration change, and no lifecycle scope. Applying ViewModel to a CLI is an anti-pattern. The existing `StorageDeleteCommand` already injects `TwoFacLib` directly, proving no architectural mandate for the wrapper. All three delegated calls (`unlock`, `getAllAccountOTPs`, `addAccount`) are one-liners that add zero value. Remove it and inject `TwoFacLib` directly in commands.
- **Note:** The `typealias DisplayAccountsStatic` lives in `AccountsViewModel.kt`; move it to `DisplayCommand.kt` or a shared types file when deleting the ViewModel.

### 2. Unnecessary qualifier abstraction ✅ CONFIRMED — Remove it
- **File:** `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/di/qualifiers.kt`
- Single `@StorageFilePath` annotation, used once in `di/modules.kt` for one `Path` binding.
- **Research verdict:** Type-safe Koin qualifier annotations (like `named<T>()`) are justified when multiple bindings of the same type exist (e.g., two `OkHttpClient` instances). Here there is exactly one `Path` bound in the entire module, so no qualifier is needed at all — direct `get()` without any qualifier will work. The annotation just adds an extra file, a custom annotation class, and a `named<StorageFilePath>()` call site for zero benefit. Drop the qualifier file and remove the qualifier from `modules.kt`.

### 3. Default command flow double-parses arguments ✅ CONFIRMED BUG — Fix it
- **File:** `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/Main.kt`
- `MainCommand.run()` calls `DisplayCommand().main(args)` when no subcommand is given, triggering a second Clikt parse of the same `args` array.
- **Research verdict:** Clikt's `main()` parses arguments exactly once internally; calling `.main(args)` a second time from within a `run()` callback is a documented anti-pattern. The correct Clikt idiom for "run parent without a subcommand" (`invokeWithoutSubcommand = true`) is to execute behavior directly inside `run()`, not to re-enter the Clikt parse chain. The bug is latent but real: the stored `args` field on `MainCommand` is the full original argv; if Clikt's parent parse consumed or validated any tokens, re-feeding them to `DisplayCommand().main(args)` could produce double-validation errors. The safe fix is to call `DisplayCommand().main(emptyArray())` (passkey will be prompted, which is the correct UX for bare `2fac`), or better, restructure so `MainCommand` does not call `.main()` at all — e.g., move the passkey prompt and display logic into a shared function invoked from both `MainCommand.run()` and `DisplayCommand.run()`.

### 4. Suspend + async-await boilerplate in storage ✅ CONFIRMED — Fix it
- **File:** `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/storage/FileStorage.kt`
- Every suspend method wraps work in `coroutineScope.async { ... }.await()`.
- **Research verdict:**
  1. Using `async { }.await()` for a single sequential operation is universally recognised as a coroutines anti-pattern. The correct idiom is `withContext(dispatcher) { ... }` for context switching, or a plain `suspend` call when no switching is needed.
  2. The stored `CoroutineScope(Dispatchers.IO)` field breaks structured concurrency: coroutines launched on it are not children of the caller's scope and will not be cancelled when the caller is cancelled.
  3. KStore (`xxfast/KStore`) already dispatches file I/O to `Dispatchers.IO` internally; its public API is fully `suspend`. Wrapping its calls in an extra `async { }.await()` on a separate `CoroutineScope` achieves nothing except adding overhead and breaking cancellation.
  - **Correct fix:** Remove the `coroutineScope` field and call KStore methods directly inside the `suspend` functions. If you want belt-and-suspenders dispatcher safety, wrap in `withContext(Dispatchers.IO) { ... }`, but it is not strictly necessary given KStore's own guarantees.

### 5. Dead imports in test scaffolding ✅ CONFIRMED — Clean it up
- **File:** `cliApp/src/commonTest/kotlin/tech/arnav/twofac/cli/di/testModules.kt`
- Three unused imports: `org.koin.core.qualifier.named`, `tech.arnav.twofac.cli.storage.AppDirUtils`, `tech.arnav.twofac.cli.storage.FileStorage`.
- **Research verdict:** Stale imports are low-risk noise that inflate cognitive overhead and can cause confusion during future refactors (e.g., someone wondering why `AppDirUtils` is imported into test modules). Trivial to remove; no behaviour change.

---

## Simplification Roadmap

### Phase 0: Baseline and safety net
- [ ] Capture current behavior with command smoke tests:
  - `2fac` (default display path)
  - `2fac add`, `2fac backup export/import`, `2fac storage delete`
- [ ] Run module tests before edits:
  - `./gradlew :cliApp:allTests`

### Phase 1: Remove redundant abstraction layers
- [ ] Delete `AccountsViewModel` and inject `TwoFacLib` directly in commands:
  - Move `typealias DisplayAccountsStatic` to `DisplayCommand.kt`
  - Update `DisplayCommand.kt`: replace `accountsViewModel: AccountsViewModel by inject()` with `twoFacLib: TwoFacLib by inject()`; call `twoFacLib.unlock(...)`, `twoFacLib.getAllAccountOTPs()`, `twoFacLib.addAccount(...)`
  - Update `AddCommand.kt`: same `TwoFacLib` injection (mirrors existing `StorageDeleteCommand` pattern)
  - Remove `AccountsViewModel` Koin binding from `di/modules.kt`
  - Delete `viewmodels/AccountsViewModel.kt`
- [ ] Remove `di/qualifiers.kt` and drop qualifier from module wiring:
  - In `di/modules.kt`, change `single(named<StorageFilePath>()) { AppDirUtils.getStorageFilePath(...) }` → `single { AppDirUtils.getStorageFilePath(...) }`
  - Change `FileStorage(get(named<StorageFilePath>()))` → `FileStorage(get())`
  - Delete `di/qualifiers.kt`

### Phase 2: Fix command dispatch and storage coroutine patterns
- [ ] Refactor `MainCommand` no-subcommand behaviour — do not re-enter `.main(args)`:
  - Preferred: extract a shared `runDisplay(passkey)` function (or move the passkey prompt + display logic to a top-level function) callable from both `MainCommand.run()` and `DisplayCommand.run()` without going through Clikt's parse chain again.
  - Acceptable minimal fix: change `DisplayCommand().main(args)` → `DisplayCommand().main(emptyArray())` so the passkey is always prompted fresh and no original tokens are re-fed to the parser.
- [ ] Replace `async { ... }.await()` pattern in `FileStorage`:
  - Remove `private val coroutineScope = CoroutineScope(Dispatchers.IO)` field
  - Remove the `import kotlinx.coroutines.CoroutineScope`, `import kotlinx.coroutines.async`, and `import kotlinx.coroutines.IO` imports
  - Replace each `coroutineScope.async { <kstore-call> }.await()` with a direct `<kstore-call>` (KStore is already suspend-safe on IO)
  - If you want explicit IO guarantees, use `withContext(Dispatchers.IO) { <kstore-call> }` instead

### Phase 3: Package/layout flattening and cleanup
- [ ] Delete now-empty `viewmodels/` package directory.
- [ ] Remove dead imports from `src/commonTest/kotlin/tech/arnav/twofac/cli/di/testModules.kt`:
  - `org.koin.core.qualifier.named`
  - `tech.arnav.twofac.cli.storage.AppDirUtils`
  - `tech.arnav.twofac.cli.storage.FileStorage`
- [ ] Keep command files focused (split `BackupCommand.kt` if it becomes too dense after refactor).

### Phase 4: Verification and docs updates
- [ ] Re-run tests: `./gradlew :cliApp:allTests`
- [ ] Manual smoke run on native target used in local dev.
- [ ] Update `cliApp/AGENTS.md` structure section after package cleanup (remove `viewmodels/` entry).

## Success Criteria
- No pass-through ViewModel layer remains.
- Root command does not re-enter Clikt parser.
- Storage implementation has no redundant async-await wrappers and no orphaned `CoroutineScope` field.
- File/package count reduced with same user-facing behaviour.
