# composeApp — Refactoring & Simplification Opportunities

**Generated:** 2026-04-28  
**Scope:** `composeApp/src/` — all platform source sets  
**Total LOC analysed:** ~12,170 across 5 source sets (commonMain, androidMain, iosMain, desktopMain, wasmJsMain)

---

## Executive Summary

The `composeApp` module is well-structured for a Kotlin Multiplatform project. Platform abstractions (session managers, backup transports, QR readers) are cleanly layered behind interfaces, and the DI graph is coherent. However, there are three high-impact structural problems, two medium-impact complexity hot-spots, and several low-impact polish items that, if addressed, would reduce total LOC by ~15–20 %, make the module far easier to test, and flatten the learning curve for new contributors.

---

## 1. HIGH — `SettingsScreen.kt` (759 LOC) is a God Composable

**File:** `src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`

### Problem

`SettingsScreen` is a `@Composable` function that has grown to 759 lines. It directly owns:

| Responsibility | Approx. LOC |
|---|---|
| Backup export / import / prepare logic | ~200 |
| Passkey enrollment dialog + state | ~150 |
| Backup-restore passkey dialog + state | ~150 |
| Companion sync trigger logic | ~100 |
| Preferences toggle logic | ~80 |
| Scaffold, top bar, Snackbar plumbing | ~80 |

It accumulates **15+ `remember { mutableStateOf(...) }` local state variables** and **15+ pre-resolved string variables** (lines 141–170) at the top of the function to work around `stringResource` not being callable from coroutines. It also inlines `suspend fun executeBackupExport(...)`, `suspend fun executeBackupImport(...)` and `suspend fun prepareBackupImport(...)` as nested local functions.

```kotlin
// Lines 96–170 — a wall of state and pre-resolved strings in one composable
var pendingAction by remember { mutableStateOf<BackupAction?>(null) }
var passkeyError by remember { mutableStateOf<String?>(null) }
var backupRestorePasskeyError by remember { mutableStateOf<String?>(null) }
var currentRestorePasskeyError by remember { mutableStateOf<String?>(null) }
var isLoading by remember { mutableStateOf(false) }
var exportProviderId by remember { mutableStateOf<String?>(null) }
var encryptedImportRequest by remember { mutableStateOf<EncryptedImportRequest?>(null) }
var showDeleteStorageDialog by remember { mutableStateOf(false) }
var isDeleteStorageInProgress by remember { mutableStateOf(false) }
var isCompanionSyncInProgress by remember { mutableStateOf(false) }
var isCompanionDiscoveryInProgress by remember { mutableStateOf(false) }
// ... 15 more string pre-resolutions ...
val msgBackupUnavailable = stringResource(Res.string.backup_unavailable_message)
val msgNoFilesFound = stringResource(Res.string.backup_no_files_found)
// etc.
```

The screen uses `getKoin()` directly to resolve services (lines 87–94), coupling the composable tightly to the DI container instead of receiving prepared state through a ViewModel.

### Recommended Fix

1. **Introduce `SettingsViewModel`** that owns all mutable state and side effects. Expose stable `StateFlow` properties.
2. **Split backup operations** into `BackupSettingsViewModel` or include them as a clearly bounded section of `SettingsViewModel`.
3. **Remove `getKoin()` calls** from the composable; instead inject the ViewModel via the standard Koin Compose extension (`koinViewModel()`).
4. **Replace pre-resolved strings with UI-layer logic** — move string selection into composable sub-functions that receive the state from the ViewModel, not into one flat block at the top.
5. **Extract sub-composables**: `BackupSettingsSection`, `SecuritySettingsSection`, `CompanionSyncSection`, `PreferencesSection`.

Estimated result: `SettingsScreen.kt` shrinks to ~200–250 LOC; `SettingsViewModel.kt` is ~200 LOC with full testability.

---

## 2. HIGH — `AccountsViewModel` mixes OTP timing logic with unlock/session concerns

**File:** `src/commonMain/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModel.kt` (315 LOC)

### Problem

`AccountsViewModel` has two responsibilities that do not belong together:

#### 2a — OTP auto-refresh loop (lines 257–289)

The ViewModel implements its own coroutine-based TOTP timing engine:

```kotlin
private fun startOtpAutoRefreshLoop() {
    otpAutoRefreshJob = viewModelScope.launch {
        while (isActive) {
            val nowEpochSeconds = Clock.System.now().epochSeconds
            val nextTotpCodeAt = _accountOtps.value
                .asSequence()
                .map { it.first.nextCodeAt }
                .filter { it > 0L }
                .minOrNull()
            if (nextTotpCodeAt != null) {
                val timeToNext = nextTotpCodeAt - nowEpochSeconds
                if (timeToNext <= 0 || (timeToNext <= 10 && _accountOtps.value.any { it.second.nextOTP == null })) {
                    refreshOtpsInternal()
                }
            }
            delay(1000)
        }
    }
}
```

This logic is hard to unit-test (requires real time or coroutine-test manipulation), and is conceptually a "service" concern, not a UI state concern.

**Recommended Fix:** Extract an `OtpRefreshCoordinator` class that takes a `TwoFacLib` and a `MutableStateFlow<List<Pair<...>>>`, starts/stops with its own job, and is injected into the ViewModel.

#### 2b — Post-unlock enrollment (lines 106–113, 306–315)

The ViewModel decides whether to enroll the passkey in the session manager after unlock:

```kotlin
sessionManagerForPostUnlockEnrollment(sessionManager, fromAutoUnlock)
    ?.enrollPasskey(passkey)
```

The helper `sessionManagerForPostUnlockEnrollment` (lines 306–315) contains three `isSecureUnlockEnabled()` / `isSecureUnlockAvailable()` / `isSecureUnlockReady()` checks that belong to the `SessionManager` hierarchy, not to a ViewModel.

**Recommended Fix:** Move the enrollment decision into `SecureSessionManager` as a `maybeEnrollPasskey(passkey, fromAutoUnlock)` method that applies its own guard conditions, reducing the ViewModel to a single unconditional call.

---

## 3. MEDIUM — `SettingsScreen` and `AccountsViewModel` pull raw services from Koin / constructor

Both `SettingsScreen` (via `getKoin()`) and `AccountsViewModel` (via constructor parameters) expose `CameraQRCodeReader` and `ClipboardQRCodeReader` as `val` (public) properties on the ViewModel:

```kotlin
class AccountsViewModel(
    ...
    val cameraQRCodeReader: CameraQRCodeReader? = null,
    val clipboardQRCodeReader: ClipboardQRCodeReader? = null,
) : ViewModel()
```

Screens access these directly (`viewModel.cameraQRCodeReader`), making the ViewModel a service locator. Readers should be consumed inside the ViewModel (or passed to the screen via UI state), not exposed as public properties.

---

## 4. MEDIUM — `OTPCard.kt` (297 LOC) has animation state coupled with display state

**File:** `src/commonMain/kotlin/tech/arnav/twofac/components/otp/OTPCard.kt`

### Problem

`OTPCard` is 297 lines. The composable contains:
- A `rememberInfiniteTransition` animation synced to the TOTP interval (lines 80–97)
- A `LaunchedEffect` coroutine that polls wall-clock time every second (lines 99–105)
- The display layout for the card itself

The `var elapsedDuration by remember { mutableLongStateOf(10L) }` state variable is declared but never written to after initialisation — it always equals `10L`, making the "show upcoming code for last N seconds" window fixed at 10 seconds regardless of any future configurability intention.

### Recommended Fix

1. **Extract `rememberOtpTimerState`** as a `@Composable` function returning `OtpTimerState(progress, timeRemaining, timerState)`, keeping animation logic separate from card layout.
2. **Remove `elapsedDuration`** if it is not configurable, and replace the magic `10L` with a named constant (e.g., `UPCOMING_CODE_VISIBILITY_SECONDS = 10L`).
3. The hardcoded `"NEXT"` string (line 212) should be a string resource for localization.

---

## 5. MEDIUM — `ensureOnboardingFileExists` duplicated across source sets

**Files:**
- `commonMain/.../onboarding/OnboardingProgressStore.kt` (lines 13–18)
- `desktopMain/.../onboarding/OnboardingProgressStore.jvm.kt` (lines 27–33)

The desktop `actual` file re-declares an **identical** private `ensureOnboardingFileExists` function:

```kotlin
// desktopMain — exact duplicate of the commonMain internal function
private fun ensureOnboardingFileExists(filePath: Path) {
    if (SystemFileSystem.exists(filePath)) return
    SystemFileSystem.sink(filePath).buffered().use { sink ->
        sink.write("""{"hasSeenInitialOnboardingGuide":false,"stepStates":{}}""".encodeToByteArray())
        sink.flush()
    }
}
```

The `internal` modifier on the commonMain version should be sufficient to make it visible to the `desktopMain` source set. The desktop copy should be removed.

---

## 6. LOW — Per-platform DI session binding boilerplate

**Files:** `androidMain/.../di/AndroidModules.kt`, `iosMain/.../di/IosModules.kt`, `desktopMain/.../di/DesktopModules.kt`

Each platform binds its session manager to three interfaces using the same pattern:

```kotlin
single { PlatformSessionManager(...) } binds arrayOf(
    BiometricSessionManager::class,
    SecureSessionManager::class,
    SessionManager::class,
)
```

This three-interface array is repeated verbatim in every platform module. A small helper in `commonMain`:

```kotlin
// In di/modules.kt
fun secureSessionManagerBindings() = arrayOf(
    BiometricSessionManager::class,
    SecureSessionManager::class,
    SessionManager::class,
)
```

would make the intent explicit and prevent drift if the hierarchy ever changes.

---

## 7. LOW — `App.kt` navigation `when` expression will grow without bound

**File:** `src/commonMain/kotlin/tech/arnav/twofac/App.kt` (lines 75–82)

The bottom-bar label resolution is a `when` over a `TopLevelDestination` enum. Currently 3 entries, but every new top-level route requires a parallel `when` arm in at least two places (label and potentially icon). Consider making `TopLevelDestination` a data class that carries its own `labelRes` and `icon`, eliminating both `when` expressions:

```kotlin
private enum class TopLevelDestination(
    val icon: ImageVector,
    @StringRes val labelRes: StringResource,
) {
    HOME(Icons.Rounded.Home, Res.string.nav_home),
    ACCOUNTS(Icons.Rounded.ManageAccounts, Res.string.nav_accounts),
    SETTINGS(Icons.Rounded.Settings, Res.string.nav_settings),
}
```

---

## 8. LOW — `DesktopSettingsManager` parallel to `StoreBackedAppPreferencesRepository`

**Files:**  
- `commonMain/.../settings/AppPreferencesStore.kt` — defines `StoreBackedAppPreferencesRepository`  
- `desktopMain/.../settings/DesktopSettingsManager.kt` — custom implementation

Desktop uses a custom `DesktopSettingsManager` rather than the common `StoreBackedAppPreferencesRepository` + `createPlatformAppPreferencesRepository()` path. This is the only platform that diverges from the KStore-backed pattern. If the reason is historical, consider aligning desktop with the common path to eliminate 121 LOC of divergent logic.

---

## 9. LOW — `BackupAction` and `EncryptedImportRequest` are `private` types inside a screen

**File:** `src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt` (lines 64–75)

```kotlin
private sealed interface BackupAction { ... }
private data class EncryptedImportRequest( ... )
```

These are currently screen-private types that model the settings domain state. Once a `SettingsViewModel` is introduced (see issue #1), they should become part of the ViewModel's public state model, accessible from tests.

---

## Summary Table

| # | Severity | File(s) | Opportunity | Estimated LOC saved |
|---|----------|---------|-------------|-------------------|
| 1 | **HIGH** | `SettingsScreen.kt` | Extract `SettingsViewModel`; split into sub-composables | ~300 |
| 2 | **HIGH** | `AccountsViewModel.kt` | Extract `OtpRefreshCoordinator`; move enrollment to `SecureSessionManager` | ~60 |
| 3 | **MEDIUM** | `AccountsViewModel.kt`, screens | Stop exposing QR readers as `val` on ViewModel | ~10 |
| 4 | **MEDIUM** | `OTPCard.kt` | Extract `rememberOtpTimerState`; remove dead `elapsedDuration`; use string resource for "NEXT" | ~30 |
| 5 | **MEDIUM** | `OnboardingProgressStore.jvm.kt` | Remove duplicate `ensureOnboardingFileExists` | ~8 |
| 6 | **LOW** | `*Modules.kt` (3 files) | Extract `secureSessionManagerBindings()` helper | ~6 |
| 7 | **LOW** | `App.kt` | Move label/icon into `TopLevelDestination` enum | ~10 |
| 8 | **LOW** | `DesktopSettingsManager.kt` | Align desktop with common KStore-backed preferences path | ~80 |
| 9 | **LOW** | `SettingsScreen.kt` | Promote `BackupAction` / `EncryptedImportRequest` to ViewModel state | (part of #1) |

**Total estimated reduction: ~500 LOC (~15% of current composeApp LOC)**

---

## What Is Working Well (Do Not Change)

- **Session Manager hierarchy** — Clean interface segregation (`SessionManager` → `SecureSessionManager` → `BiometricSessionManager`) is well-designed and easy to extend.
- **Type-safe navigation** — Serializable route objects with Compose Navigation are idiomatic and future-proof.
- **Koin DI** — `getOrNull<T>()` for optional platform services and `getAll<T>()` for multi-binding contributors is flexible and correct.
- **Backup transport abstraction** — `BackupTransport` interface + `BackupTransportRegistry` + named Koin bindings is a clean open/closed pattern.
- **`OnboardingViewModel`** — Already follows good ViewModel patterns: single `StateFlow<OnboardingUiState>`, all side-effects delegated to repositories.
- **Expect/actual for storage** — Platform `createOnboardingProgressStore()` / `createPlatformAppPreferencesRepository()` is the right pattern; keep it.
