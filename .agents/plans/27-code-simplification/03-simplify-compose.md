---
name: Code Simplification Plan - composeApp
status: Planned
progress:
  - "[ ] Phase 0: Remove tracked artifacts and release-debug leftovers"
  - "[ ] Phase 1: Consolidate platform storage/session path helpers"
  - "[ ] Phase 2: Reduce onboarding and DI duplication"
  - "[ ] Phase 3: Delete dead compatibility and unused code"
  - "[ ] Phase 4: Verify all targets and update docs"
---

# Code Simplification Plan - composeApp

## Scope
- Module: `composeApp` (commonMain + androidMain + iosMain + desktopMain + wasmJsMain + extension)
- Goal: cut accidental artifacts, remove duplicated platform plumbing, and shrink unnecessary abstraction layers.

## High-Signal Findings (evidence)
1. **Tracked build/artifact files in repo**
   - `composeApp/src/desktopMain/native/macos/TwoFacKeychain/libtwofac_keychain.dylib`
   - `composeApp/bin/desktopMain/**` (8 files tracked; byte-identical to `src/desktopMain/resources/**`)
2. **Debug log file writes in production DI path**
   - `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/di/DesktopModules.kt`
   - writes `~/twofac-native-debug.log` during startup.
3. **Platform path setup duplicated across multiple files**
   - repeated `AppDirs { appName = "TwoFac" ... }` usage in multiple platform files.
   - iOS `getDocumentDirectory()` duplicated in:
     - `storage/AppDirUtils.ios.kt`
     - `onboarding/OnboardingProgressStore.ios.kt`
4. **Onboarding contributor duplication**
   - `AndroidOnboardingContributor`, `IosOnboardingContributor`, `WasmOnboardingContributor` differ mostly by one resource key.
5. **Triple DI registration boilerplate for session manager interfaces**
   - platform modules repeatedly bind same instance as `BiometricSessionManager`, `SecureSessionManager`, `SessionManager`.
6. **Dead/legacy code**
   - `session/DesktopSecureUnlockResult.kt` (unreferenced)
   - `wear/WatchSyncCoordinator.kt` (deprecated wrapper compatibility layer)
   - `Platform` interface appears vestigial for current storage flow.
7. **Near-duplicate UI error components**
   - `components/accounts/InlineErrorMessage.kt`
   - `components/accounts/AccountsErrorState.kt`

## Simplification Roadmap

### Phase 0: Remove tracked artifacts and release-debug leftovers
- [ ] Remove tracked `.dylib` from source tree and ensure it is built/generated, not committed.
- [ ] Remove tracked `composeApp/bin/desktopMain/**` resource duplicates.
- [ ] Update `.gitignore` to prevent future artifact commits.
- [ ] Remove startup debug file writes from `DesktopModules.kt` (or gate under explicit debug flag).

### Phase 1: Consolidate platform storage/session path helpers
- [ ] Create shared platform app-dir helper per target, reuse across `storage` + `onboarding` paths.
- [ ] Extract iOS `getDocumentDirectory()` into one internal utility and reuse.
- [ ] Deduplicate `ensureOnboardingFileExists(...)` bodies across targets.

### Phase 2: Reduce onboarding and DI duplication
- [ ] Collapse near-identical platform onboarding contributors into one common template + platform-specific description key.
- [ ] Replace repetitive multi-interface Koin registrations with explicit `bind` chains where supported.
- [ ] Re-evaluate marker interfaces (`CommonOnboardingStepContributor` / `PlatformOnboardingStepContributor`) versus named qualifier strategy.

### Phase 3: Delete dead compatibility and unused code
- [ ] Remove `DesktopSecureUnlockResult.kt` if still unreferenced after refactor.
- [ ] Remove deprecated `wear/WatchSyncCoordinator.kt` once callsites/tests use new companion APIs.
- [ ] Merge or standardize accounts error-state composables.
- [ ] Remove vestigial `Platform` abstraction if no practical consumers remain.

### Phase 4: Verify all targets and update docs
- [ ] Build/test key targets:
  - `:composeApp:compileKotlinAndroid`
  - `:composeApp:compileKotlinDesktop`
  - `:composeApp:compileKotlinIosSimulatorArm64`
  - `:composeApp:compileKotlinWasmJs`
- [ ] Update `composeApp/AGENTS.md` structure notes.

## Success Criteria
- No generated binaries/resource staging artifacts are tracked.
- Platform path/session setup duplication is materially reduced.
- Dead compatibility/legacy files removed.
- Compose module remains behaviorally unchanged across Android/iOS/Desktop/Web targets.
