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
1. **Pass-through ViewModel layer**
   - `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/viewmodels/AccountsViewModel.kt`
   - Only delegates 3 methods to `TwoFacLib` with no state or transformation.
2. **Unnecessary qualifier abstraction**
   - `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/di/qualifiers.kt`
   - Single annotation used once in `di/modules.kt` for one `Path` binding.
3. **Default command flow double-parses arguments**
   - `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/Main.kt`
   - `DisplayCommand().main(args)` in root `run()` triggers second parse/prompt path.
4. **Suspend + async-await boilerplate in storage**
   - `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/storage/FileStorage.kt`
   - Every suspend method wraps work in `coroutineScope.async { ... }.await()`.
5. **Low-value test noise**
   - `cliApp/src/commonTest/kotlin/tech/arnav/twofac/cli/di/testModules.kt`
   - Dead imports / stale DI scaffolding.

## Simplification Roadmap

### Phase 0: Baseline and safety net
- [ ] Capture current behavior with command smoke tests:
  - `2fac` (default display path)
  - `2fac add`, `2fac backup export/import`, `2fac storage delete`
- [ ] Run module tests before edits:
  - `./gradlew :cliApp:allTests`

### Phase 1: Remove redundant abstraction layers
- [ ] Delete `AccountsViewModel` and inject `TwoFacLib` directly in commands:
  - update `commands/DisplayCommand.kt`
  - update `commands/AddCommand.kt`
  - remove binding from `di/modules.kt`
- [ ] Remove `di/qualifiers.kt` and replace with either:
  - direct constructor wiring, or
  - single string qualifier (if still needed)

### Phase 2: Fix command dispatch and storage coroutine patterns
- [ ] Refactor `MainCommand` no-subcommand behavior to avoid nested `.main(args)` dispatch.
- [ ] Replace `async { ... }.await()` with `withContext(Dispatchers.IO)` (or direct suspend call where no dispatcher switch is needed).
- [ ] Remove `CoroutineScope` field from `FileStorage` if no longer required.

### Phase 3: Package/layout flattening and cleanup
- [ ] Delete now-empty `viewmodels/` package.
- [ ] Keep command files focused (split `BackupCommand.kt` if it becomes too dense after refactor).
- [ ] Remove dead imports and stale comments in tests.

### Phase 4: Verification and docs updates
- [ ] Re-run tests: `./gradlew :cliApp:allTests`
- [ ] Manual smoke run native target used in local dev.
- [ ] Update `cliApp/AGENTS.md` structure section after package cleanup.

## Success Criteria
- No pass-through ViewModel layer remains.
- Root command does not re-enter Clikt parser.
- Storage implementation has no redundant async-await wrappers.
- File/package count reduced with same user-facing behavior.
