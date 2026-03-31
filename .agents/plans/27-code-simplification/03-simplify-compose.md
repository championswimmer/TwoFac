---
name: Code Simplification Plan - composeApp
status: Completed
progress:
  - "[x] Phase 0: Remove tracked artifacts and release-debug leftovers"
  - "[x] Phase 1: Consolidate platform storage/session path helpers"
  - "[x] Phase 2: Reduce onboarding and DI duplication"
  - "[x] Phase 3: Delete dead compatibility and unused code"
  - "[x] Phase 4: Verify all targets and update docs"
---

# Code Simplification Plan - composeApp

## Scope
- Module: `composeApp` (commonMain + androidMain + iosMain + desktopMain + wasmJsMain + extension)
- Goal: cut accidental artifacts, remove duplicated platform plumbing, and shrink unnecessary abstraction layers.

## Research Notes (Steelman Review — 2026-03-31)

Each task below was evaluated against actual code evidence and KMP best practices.  
Two tasks were **dropped** (see § Dropped Tasks at the bottom).

---

## High-Signal Findings (evidence)

1. **Tracked build/artifact files in repo** ✅ CONFIRMED
   - `composeApp/src/desktopMain/native/macos/TwoFacKeychain/libtwofac_keychain.dylib`
   - `composeApp/bin/desktopMain/**` (8 files tracked; **MD5-confirmed byte-identical** to `src/desktopMain/resources/**`)

2. **Debug log file writes in production DI path** ✅ CONFIRMED
   - `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/di/DesktopModules.kt`
   - Writes three `logFile.appendText(...)` lines to `~/twofac-native-debug.log` unconditionally on every startup.

3. **iOS `getDocumentDirectory()` duplicated** ✅ CONFIRMED BYTE-FOR-BYTE
   - `storage/AppDirUtils.ios.kt` — private fun `getDocumentDirectory()`
   - `onboarding/OnboardingProgressStore.ios.kt` — identical private fun `getDocumentDirectory()`
   - Also: `Platform.ios.kt` has a third copy inside `IOSPlatform.getAppDataDir()` (see item 6).

4. **AppDirs block duplicated across JVM and Android targets** ✅ CONFIRMED
   - `AppDirUtils.jvm.kt` and `OnboardingProgressStore.jvm.kt` both declare `private val appDirs = AppDirs { appName="TwoFac" ... }`.
   - `AppDirUtils.android.kt` and `OnboardingProgressStore.android.kt` do the same (without the `macOS` flag).

5. **`ensureOnboardingFileExists` duplicated across JVM and iOS** ✅ CONFIRMED
   - Identical function bodies in `OnboardingProgressStore.jvm.kt` and `OnboardingProgressStore.ios.kt`.
   - Both use only `kotlinx.io.files.SystemFileSystem` + `kotlinx.io.buffered`, which are available in **commonMain**.
   - `ensureStorageFileExists` already lives in `commonMain/storage/AppDirUtils.kt` as precedent.

6. **Platform onboarding contributors near-identical** ✅ CONFIRMED
   - `AndroidOnboardingContributor`, `IosOnboardingContributor`, `WasmOnboardingContributor` differ by exactly **one string resource key** (`onboarding_step_secure_unlock_{android,ios,wasm}_description`).
   - `DesktopOnboardingContributor` is structurally different (just returns `omit`), so it stays separate.

7. **Triple DI registration boilerplate** ✅ CONFIRMED
   - `desktopSessionModule` and `androidBiometricModule` both contain:
     ```kotlin
     single<BiometricSessionManager> { … }
     single<SecureSessionManager> { get<BiometricSessionManager>() }
     single<SessionManager> { get<BiometricSessionManager>() }
     ```
   - Koin 3.2+ supports `singleOf(::Impl) { bind<A>(); bind<B>() }` or `single { … } binds arrayOf(…)`.

8. **Dead/legacy code** ✅ CONFIRMED
   - `DesktopSecureUnlockResult.kt`: grep over the entire codebase finds **zero import or usage sites**; only the file itself contains the symbol.
   - `wear/WatchSyncCoordinator.kt`: all deprecated `typealias` + wrapper functions; test class `WatchSyncCoordinatorTest` **already uses the new `CompanionSync*` API** directly — no migration needed.
   - `Platform` interface + `getPlatform()`: `getPlatform()` is **never called** in the codebase; `Platform.getAppDataDir()` is **never called** either. Each platform's AppDirUtils handles paths directly. The `IOSPlatform.getAppDataDir()` is a third copy of the `getDocumentDirectory()` pattern.

9. ~~**Near-duplicate UI error components**~~ → **DROPPED** (see § Dropped Tasks)

---

## Simplification Roadmap

### Phase 0: Remove tracked artifacts and release-debug leftovers

- [x] Remove tracked `.dylib` from source tree; add `*.dylib` to `.gitignore`.
  - _Reason_: Compiled native binaries must never be committed. The `build.sh` + `TwoFacKeychain.swift` source is already in the same directory.
- [x] Remove tracked `composeApp/bin/desktopMain/**` resource duplicates.
  - _Reason_: MD5 confirmed byte-for-byte identical to `src/desktopMain/resources/**`. Two copies mean divergence risk on any icon update. Add `composeApp/bin/` to `.gitignore`.
- [x] Update `.gitignore` to prevent future artifact commits (`*.dylib`, `composeApp/bin/`).
- [x] Remove startup debug file writes from `DesktopModules.kt`.
  - _Reason_: Production code should not write arbitrary files to `~/` without user consent. These were clearly temporary debug statements. Remove the three `logFile.appendText(...)` calls and the `logFile` variable; keep the OS-detection logic.

### Phase 1: Consolidate platform storage/session path helpers

- [x] **iOS**: Extract `getDocumentDirectory()` into a single `internal` function in a new file (e.g., `iosMain/kotlin/tech/arnav/twofac/internal/IosFileUtils.kt`) and have both `AppDirUtils.ios.kt` and `OnboardingProgressStore.ios.kt` call it.
  - _Reason_: Currently byte-for-byte duplicated. Any change (e.g., switching to `FileManager.defaultManager`) must be made in two places. `IOSPlatform.getAppDataDir()` in `Platform.ios.kt` is a third copy and will be removed with the Platform abstraction (Phase 3).
- [ ] **JVM/Android**: Extract `AppDirs { … }` initialisation into a single top-level val per source set (e.g., a shared `internal val appDirs` in a new `jvmMain/internal/JvmAppDirs.kt`) and reuse it in both `AppDirUtils.jvm.kt` and `OnboardingProgressStore.jvm.kt`.
  - _Reason_: The same `AppDirs { appName="TwoFac"; appAuthor="tech.arnav"; macOS.useSpaceBetweenAuthorAndApp=false }` block appears independently in two JVM files (and two Android files). Note: the Android config intentionally omits the `macOS` flag, so keep the targets separate.
- [ ] **commonMain**: Move `ensureOnboardingFileExists(filePath: Path)` from both `OnboardingProgressStore.jvm.kt` and `OnboardingProgressStore.ios.kt` into `commonMain/kotlin/tech/arnav/twofac/onboarding/OnboardingProgressStore.kt` as an `internal` function.
  - _Reason_: Both bodies are identical and use only `kotlinx.io.files.SystemFileSystem` + `kotlinx.io.buffered` — both available in commonMain. The analogous `ensureStorageFileExists` already lives in `commonMain/storage/AppDirUtils.kt` and serves as a direct precedent.

### Phase 2: Reduce onboarding and DI duplication

- [ ] **Collapse near-identical platform onboarding contributors** into a single `commonMain` class parameterised by the platform-specific description resource:
  ```kotlin
  // commonMain
  class SecureUnlockOnboardingContributor(
      private val descriptionRes: StringResource,
  ) : PlatformOnboardingStepContributor {
      override suspend fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
          if (!context.secureUnlockAvailable) return listOf(omit(OnboardingStepSlot.SECURE_UNLOCK))
          return listOf(
              OnboardingGuideStep(
                  id = OnboardingStepIds.SECURE_UNLOCK,
                  slot = OnboardingStepSlot.SECURE_UNLOCK,
                  title = getString(Res.string.onboarding_step_secure_unlock_title),
                  description = getString(descriptionRes),
                  required = false,
                  icon = OnboardingStepIcon.SECURE_UNLOCK,
                  action = OnboardingGuideAction.OpenSettings,
                  actionLabel = getString(Res.string.onboarding_step_secure_unlock_action),
                  completionRule = OnboardingCompletionRule.SECURE_UNLOCK_READY,
              ).provide()
          )
      }
  }
  ```
  Each platform DI module then becomes a one-liner:
  ```kotlin
  // androidMain: single<PlatformOnboardingStepContributor> { SecureUnlockOnboardingContributor(Res.string.onboarding_step_secure_unlock_android_description) }
  ```
  - _Reason_: Three files (`Android/Ios/WasmOnboardingContributor.kt`) that are structurally identical differ by exactly one `StringResource`. `StringResource` is accessible in `commonMain`. `DesktopOnboardingContributor` is genuinely different (`omit` only) and stays separate.

- [ ] **Replace repetitive multi-interface Koin registrations** with Koin's `binds` DSL:
  ```kotlin
  // Before (3 lines per module)
  single<BiometricSessionManager> { DesktopBiometricSessionManager(get()) }
  single<SecureSessionManager> { get<BiometricSessionManager>() }
  single<SessionManager> { get<BiometricSessionManager>() }

  // After (1 declaration)
  single { DesktopBiometricSessionManager(get()) } binds arrayOf(
      BiometricSessionManager::class,
      SecureSessionManager::class,
      SessionManager::class,
  )
  ```
  - _Reason_: `binds` is the idiomatic Koin 3.x approach for multi-interface single instances. The current pattern creates multiple Koin bindings that individually delegate to each other, which is noisier and slightly less efficient at container startup. Apply to both `desktopSessionModule` and `androidBiometricModule`.

### Phase 3: Delete dead compatibility and unused code

- [ ] **Remove `DesktopSecureUnlockResult.kt`**.
  - _Reason_: Exhaustive grep across the entire codebase finds zero import or usage sites. The sealed class was never wired up. Dead code.

- [ ] **Remove `wear/WatchSyncCoordinator.kt`** (the deprecated compat layer).
  - _Reason_: All four deprecated symbols (`WatchSyncCoordinator`, `WatchSyncSourceAccount`, `buildWatchSyncSnapshot`, `isSyncToWatchEnabled`) are `typealias` / forwarding wrappers. `WatchSyncCoordinatorTest` already imports and calls `CompanionSyncSourceAccount`, `buildCompanionSyncSnapshot`, and `isSyncToCompanionEnabled` directly — **no migration required**. The file can be deleted immediately.

- [ ] **Remove the `Platform` interface and `getPlatform()` expect/actual declarations**.
  - _Reason_: `getPlatform()` is declared as `expect fun` but is **never called** anywhere in the codebase. `Platform.getAppDataDir()` is also never called; each platform's `AppDirUtils` handles paths independently. `Platform.name` is never used. The `IOSPlatform.getAppDataDir()` is a third copy of the `getDocumentDirectory()` logic that will be cleaned up in Phase 1. Removing the entire `Platform.kt` + all `Platform.*.kt` files eliminates ~50 LOC of dead abstraction. Precedent: KMP style guides recommend removing `expect class Platform()` once its responsibilities are absorbed by DI-injected interfaces (which `AppDirUtils` already is).

### Phase 4: Verify all targets and update docs

- [ ] Build/test key targets:
  - `:composeApp:compileKotlinAndroid`
  - `:composeApp:compileKotlinDesktop`
  - `:composeApp:compileKotlinIosSimulatorArm64`
  - `:composeApp:compileKotlinWasmJs`
- [ ] Update `composeApp/AGENTS.md` structure notes.

---

## Dropped Tasks

The following items from the original plan were evaluated and **removed** because the simplification does not pay off or the current design is already idiomatic.

### ~~Re-evaluate marker interfaces (`CommonOnboardingStepContributor` / `PlatformOnboardingStepContributor`) versus named qualifier strategy~~ — DROPPED

**Original proposal**: Replace distinct `CommonOnboardingStepContributor` and `PlatformOnboardingStepContributor` interfaces with a single `OnboardingStepContributor` differentiated by Koin `named("common")` / `named("platform")` qualifiers.

**Why dropped**: The current two-interface approach is idiomatic KMP/Koin. The two contributor roles are genuinely distinct (platform-agnostic common steps vs. platform-specific steps), and the type system enforces that distinction at compile time. Switching to runtime string qualifiers trades type safety for a trivial reduction in interface count. KMP best-practice research confirms: prefer type-driven DI differentiation over string qualifiers when the roles are semantically different.

---

### ~~Merge or standardize accounts error-state composables~~ — DROPPED

**Original proposal**: Merge `InlineErrorMessage` and `AccountsErrorState` into a single component.

**Why dropped**: The two components serve genuinely different call sites and have intentionally different behaviour:

| | `InlineErrorMessage` | `AccountsErrorState` |
|---|---|---|
| Padding | 8 dp | 16 dp |
| String handling | raw `message: String` | wraps with `Res.string.error_prefix` |
| Used by | `AddAccountScreen` (inline, field-level errors) | `AccountsScreen` (full-screen error state) |

Merging would require either a boolean `addPrefix` parameter or two overloads — both worse than two small, focused composables. Research on Compose Multiplatform component strategy confirms: components that will evolve differently (error annotation vs. full-state display) should remain separate; merging is only warranted when there is a genuine shared "internal base" with pure parameter variation.

---

## Success Criteria

- No generated binaries or resource staging artifacts are tracked in git.
- iOS `getDocumentDirectory()` defined exactly once; JVM `AppDirs` block defined exactly once per source set.
- `ensureOnboardingFileExists` lives in commonMain.
- Platform onboarding contributor boilerplate reduced from 3 identical files to 1 parameterised class.
- Koin session module uses `binds` instead of triple-delegation pattern.
- `DesktopSecureUnlockResult.kt`, `WatchSyncCoordinator.kt`, and all `Platform.kt` files deleted.
- Compose module remains behaviorally unchanged across Android/iOS/Desktop/Web targets (verified by Phase 4 builds).
