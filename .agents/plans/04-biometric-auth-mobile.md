---
task: Implement Biometric Authentication for Mobile Apps (Android & iOS)
status: Not Started
progress:
  - "[ ] Phase 0 — Common SessionManager groundwork"
  - "[ ] Phase 1 — Android BiometricSessionManager"
  - "[ ] Phase 2 — iOS BiometricSessionManager"
  - "[ ] Phase 3 — Shared UI for biometric opt-in"
  - "[ ] Phase 4 — Testing and validation"
---
# Biometric Authentication Plan (Android & iOS)

## Goal

Allow users to **opt in** to biometric authentication (fingerprint / Face ID / Touch ID) on mobile devices. Biometric auth protects the stored passkey: when the user authenticates with biometrics, the passkey is retrieved from secure storage and used to unlock the account vault — identical to the existing manual passkey flow. Users who do not opt in continue using the passkey dialog as before.

---

## Design Principles

1. **Opt-in only** — Biometric auth is never forced. Users can continue entering the passkey manually.
2. **Passkey remains the source of truth** — Biometrics protect *access to* the passkey; they do not replace it. The underlying unlock mechanism (`twoFacLib.unlock(passkey)`) is unchanged.
3. **Fallback to passkey** — If biometric authentication fails (cancelled, locked out, not enrolled), the user is shown the standard passkey dialog.
4. **sharedLib stays platform-agnostic** — The core library has no knowledge of biometrics. All biometric logic lives in platform-specific `composeApp` source sets.
5. **Bring Your Own SessionManager** — Each platform provides its own `SessionManager` implementation via Koin. The common code consumes the interface without knowing the backing storage mechanism.
6. **Secure storage** — The passkey is stored in platform-provided secure enclaves (Android Keystore / iOS Keychain) and is only accessible after successful biometric authentication.

---

## Current Architecture

### SessionManager interface (`composeApp/commonMain`)

```kotlin
interface SessionManager {
    fun isAvailable(): Boolean
    fun isRememberPasskeyEnabled(): Boolean
    fun setRememberPasskey(enabled: Boolean)
    fun getSavedPasskey(): String?
    fun savePasskey(passkey: String)
    fun clearPasskey()
}
```

### Existing implementations

| Platform | Implementation | Storage |
|---|---|---|
| Browser (wasmJs) | `BrowserSessionManager` | `localStorage` (plaintext, opt-in) |
| Android | *None* | — |
| iOS | *None* | — |
| Desktop | *None* | — |

### Unlock flow (common)

1. `HomeScreen` → `LaunchedEffect` checks `viewModel.getSavedPasskey()`
2. If a saved passkey exists → auto-unlock via `viewModel.loadAccountsWithOtps(passkey)`
3. If no saved passkey → show `PasskeyDialog`
4. On successful unlock → `sessionManager?.savePasskey(passkey)` (if opt-in enabled)
5. On auto-unlock failure → `clearSavedPasskey()` + show `PasskeyDialog`

### DI wiring

```kotlin
// commonMain — modules.kt
single<AccountsViewModel> {
    AccountsViewModel(
        twoFacLib = get(),
        companionSyncCoordinator = getOrNull<CompanionSyncCoordinator>(),
        sessionManager = getOrNull(),  // null on platforms without a SessionManager
    )
}

// wasmJsMain — WasmModules.kt
val wasmSessionModule = module {
    single<SessionManager> { BrowserSessionManager() }
}
```

---

## High-Level Architecture for Biometric Auth

```
┌──────────────────────────────────────────────────────────────┐
│                    composeApp / commonMain                    │
│                                                              │
│  SessionManager (interface)                                  │
│       ▲                                                      │
│       │ getOrNull<SessionManager>()                          │
│       │                                                      │
│  AccountsViewModel                                           │
│    • getSavedPasskey()    → SessionManager?.getSavedPasskey() │
│    • savePasskey(pk)      → SessionManager?.savePasskey(pk)  │
│    • clearSavedPasskey()  → SessionManager?.clearPasskey()   │
│                                                              │
│  HomeScreen                                                  │
│    • Auto-unlock flow (unchanged — just calls ViewModel)     │
│                                                              │
│  SettingsScreen                                              │
│    • "Remember Passkey" toggle (unchanged)                   │
│    • NEW: "Biometric Unlock" toggle (only if available)      │
└──────────────────────────────────────────────────────────────┘
         ▲                              ▲
         │                              │
┌────────┴────────────┐    ┌────────────┴──────────────┐
│  androidMain        │    │  iosMain                   │
│                     │    │                            │
│  BiometricSession   │    │  BiometricSession          │
│    Manager          │    │    Manager                 │
│                     │    │                            │
│  Uses:              │    │  Uses:                     │
│  • AndroidKeystore  │    │  • iOS Keychain            │
│  • BiometricPrompt  │    │    (kSecAttrAccessControl   │
│  • EncryptedShared  │    │     biometryCurrentSet)    │
│    Preferences      │    │  • LAContext               │
│                     │    │    (LocalAuthentication)   │
│  Koin:              │    │                            │
│  androidBiometric   │    │  Koin:                     │
│    Module           │    │  iosBiometricModule        │
└─────────────────────┘    └────────────────────────────┘
```

The key insight is that `getSavedPasskey()` on mobile will:
1. Check if biometric is enabled
2. Prompt the user for biometric authentication
3. On success, decrypt and return the passkey from secure storage
4. On failure, return `null` (which triggers the manual passkey dialog)

Since `getSavedPasskey()` is called from a coroutine in `HomeScreen`'s `LaunchedEffect`, we can make it a `suspend` function (or use a callback/continuation) to accommodate the async biometric prompt.

---

## SessionManager Interface Evolution

The current `SessionManager` interface has synchronous methods. Biometric authentication is inherently asynchronous (user interaction required). We need to handle this.

### Option A: Make `getSavedPasskey()` a suspend function

```kotlin
interface SessionManager {
    fun isAvailable(): Boolean
    fun isRememberPasskeyEnabled(): Boolean
    fun setRememberPasskey(enabled: Boolean)
    suspend fun getSavedPasskey(): String?   // ← now suspend
    fun savePasskey(passkey: String)
    fun clearPasskey()
}
```

**Pros:** Clean coroutine integration, natural async flow.
**Cons:** Breaking change for `BrowserSessionManager` (minor — just add `suspend` keyword).

### Option B: Separate biometric-specific method

```kotlin
interface SessionManager {
    // ... existing methods unchanged ...
    fun getSavedPasskey(): String?  // Returns immediately (non-biometric path)
}

interface BiometricSessionManager : SessionManager {
    fun isBiometricAvailable(): Boolean
    fun isBiometricEnabled(): Boolean
    fun setBiometricEnabled(enabled: Boolean)
    suspend fun authenticateAndGetPasskey(): String?  // Biometric prompt
}
```

**Pros:** No breaking changes, explicit biometric capability check.
**Cons:** Slightly more complex.

### Recommendation: Option A (suspend `getSavedPasskey`)

**Rationale:**
- The call site (`HomeScreen.LaunchedEffect`) already runs in a coroutine scope
- The `BrowserSessionManager` change is trivial (add `suspend` keyword, no behavior change)
- Keeps the interface simple and consistent
- All other methods remain synchronous (they don't require user interaction)
- The `savePasskey()` method does not need to be async because on biometric-enabled platforms, the passkey is encrypted synchronously after the biometric auth already happened during `getSavedPasskey()`

Note: `ViewModel.getSavedPasskey()` will also become a suspend function (or return a `Flow`/`Deferred`). This requires minor refactoring of `HomeScreen`'s `LaunchedEffect`, which already runs in a coroutine context.

---

## Phase 0 — Common SessionManager Groundwork

### 0.1 Make `getSavedPasskey()` a suspend function

**File:** `composeApp/src/commonMain/kotlin/tech/arnav/twofac/session/SessionManager.kt`

```kotlin
interface SessionManager {
    fun isAvailable(): Boolean
    fun isRememberPasskeyEnabled(): Boolean
    fun setRememberPasskey(enabled: Boolean)
    suspend fun getSavedPasskey(): String?   // ← changed to suspend
    fun savePasskey(passkey: String)
    fun clearPasskey()
}
```

### 0.2 Update BrowserSessionManager

**File:** `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/session/BrowserSessionManager.kt`

Add `suspend` to `getSavedPasskey()` override. No behavior change.

### 0.3 Update AccountsViewModel

**File:** `composeApp/src/commonMain/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModel.kt`

Change `getSavedPasskey()` to a `suspend fun`:

```kotlin
suspend fun getSavedPasskey(): String? = sessionManager?.getSavedPasskey()
```

### 0.4 Update HomeScreen

**File:** `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/HomeScreen.kt`

The `LaunchedEffect` already runs in a coroutine, so calling `suspend getSavedPasskey()` works naturally. No structural change needed, just ensure the call is within a coroutine scope.

### 0.5 Add biometric helper methods to SessionManager (optional extension)

Add an optional interface extension for biometric capability detection:

```kotlin
// commonMain — SessionManager.kt
interface SessionManager {
    // ... existing methods ...

    /** Whether this platform supports biometric authentication. Default: false. */
    fun isBiometricAvailable(): Boolean = false

    /** Whether the user has enabled biometric unlock. Default: false. */
    fun isBiometricEnabled(): Boolean = false

    /** Toggle biometric unlock. Default: no-op. */
    fun setBiometricEnabled(enabled: Boolean) {}
}
```

This keeps a single interface while allowing platforms that support biometrics to override these methods. Platforms without biometrics (browser, desktop) simply inherit the defaults.

---

## Phase 1 — Android BiometricSessionManager

### Android Biometric Stack

| Component | Purpose |
|---|---|
| `androidx.biometric:biometric` | Unified biometric prompt API (fingerprint, face, iris) |
| `AndroidKeyStore` | Hardware-backed key storage |
| `EncryptedSharedPreferences` | AES-256 encrypted preferences (backed by Keystore) |
| `BiometricPrompt` | System UI for biometric authentication |
| `KeyGenParameterSpec` | Key generation with biometric binding |

### 1.1 Add dependency

**File:** `composeApp/build.gradle.kts`

```kotlin
androidMain.dependencies {
    // ... existing dependencies ...
    implementation("androidx.biometric:biometric:1.4.0-alpha05")
}
```

> **Note:** Use the `1.4.0-alpha05` version for `BiometricPrompt.AuthenticationCallback` with `CryptoObject` support and `Class 3` biometric requirement. Alternatively, use stable `1.1.0` which is sufficient for basic biometric prompt.

### 1.2 Create BiometricSessionManager

**File:** `composeApp/src/androidMain/kotlin/tech/arnav/twofac/session/AndroidBiometricSessionManager.kt`

```kotlin
package tech.arnav.twofac.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
```

#### Storage Strategy

Two-tier storage:
1. **Preference flag** (`SharedPreferences`): stores whether biometric/remember-passkey is enabled (boolean). Not sensitive.
2. **Encrypted passkey** (`EncryptedSharedPreferences` or manual KeyStore encryption): stores the passkey encrypted with a biometric-bound key.

#### Key Management

```
┌─────────────────────────────────────────────────┐
│ Android Keystore                                │
│                                                 │
│  Key alias: "twofac_biometric_key"              │
│  Algorithm: AES/GCM/NoPadding                   │
│  Purpose:   ENCRYPT | DECRYPT                   │
│  Auth:      setUserAuthenticationRequired(true)  │
│             setUserAuthenticationValidity...     │
│               (AUTH_VALIDITY_SECONDS)            │
│             setInvalidatedByBiometricEnrollment  │
│               (true)                             │
│                                                 │
│  → Key usable for 15 min after BiometricPrompt  │
│    authentication succeeds (no re-prompt)       │
└─────────────────────────────────────────────────┘
```

#### Core Implementation

```kotlin
class AndroidBiometricSessionManager(
    private val context: Context,
    private val activityProvider: () -> FragmentActivity,
) : SessionManager {

    companion object {
        private const val KEYSTORE_ALIAS = "twofac_biometric_key"
        private const val PREFS_NAME = "twofac_session_prefs"
        private const val KEY_REMEMBER_ENABLED = "remember_passkey_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_ENCRYPTED_PASSKEY = "encrypted_passkey"
        private const val KEY_PASSKEY_IV = "passkey_iv"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        /** Time-based validity window for biometric auth (seconds). After a successful
         *  biometric authentication, the Keystore key remains usable for this duration
         *  without re-prompting. Can be made a user setting later. */
        private const val AUTH_VALIDITY_SECONDS = 15 * 60  // 15 minutes
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun isAvailable(): Boolean = true

    override fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
                && isBiometricAvailable()
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
        if (!enabled) {
            clearPasskey()
            deleteKey()
        }
    }

    override fun isRememberPasskeyEnabled(): Boolean {
        // "Remember passkey" is implicitly enabled when biometric is enabled
        return isBiometricEnabled()
                || prefs.getBoolean(KEY_REMEMBER_ENABLED, false)
    }

    override fun setRememberPasskey(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ENABLED, enabled).apply()
        if (!enabled) clearPasskey()
    }

    override suspend fun getSavedPasskey(): String? {
        if (!isRememberPasskeyEnabled()) return null

        val encryptedData = prefs.getString(KEY_ENCRYPTED_PASSKEY, null)
            ?: return null
        val iv = prefs.getString(KEY_PASSKEY_IV, null)
            ?: return null

        if (isBiometricEnabled()) {
            // Biometric path: prompt user, then decrypt
            return authenticateAndDecrypt(encryptedData, iv)
        } else {
            // Non-biometric path: decrypt without prompt
            // (for future use — currently Android only uses biometric)
            return null
        }
    }

    override fun savePasskey(passkey: String) {
        if (!isRememberPasskeyEnabled()) return

        try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val encrypted = cipher.doFinal(passkey.toByteArray())
            val iv = cipher.iv

            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSKEY,
                    android.util.Base64.encodeToString(
                        encrypted, android.util.Base64.NO_WRAP))
                .putString(KEY_PASSKEY_IV,
                    android.util.Base64.encodeToString(
                        iv, android.util.Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            // Key invalidated or not available — clear and ignore
            clearPasskey()
        }
    }

    override fun clearPasskey() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_PASSKEY)
            .remove(KEY_PASSKEY_IV)
            .apply()
    }

    // ── Private helpers ──

    private suspend fun authenticateAndDecrypt(
        encryptedBase64: String,
        ivBase64: String,
    ): String? = suspendCoroutine { continuation ->
        try {
            val key = getOrCreateKey()
            val iv = android.util.Base64.decode(
                ivBase64, android.util.Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE, key,
                GCMParameterSpec(128, iv)
            )

            val activity = activityProvider()
            val executor = ContextCompat.getMainExecutor(context)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    try {
                        val resultCipher = result.cryptoObject?.cipher
                            ?: cipher
                        val encrypted = android.util.Base64.decode(
                            encryptedBase64, android.util.Base64.NO_WRAP)
                        val decrypted = resultCipher.doFinal(encrypted)
                        continuation.resume(String(decrypted))
                    } catch (e: Exception) {
                        clearPasskey()
                        continuation.resume(null)
                    }
                }

                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence
                ) {
                    // User cancelled, locked out, or no biometrics enrolled
                    continuation.resume(null)
                }

                override fun onAuthenticationFailed() {
                    // Single attempt failed — prompt stays open for retry
                    // (Android system handles retry UI automatically)
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock TwoFac")
                .setSubtitle("Authenticate to access your 2FA codes")
                .setNegativeButtonText("Use passkey instead")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                )
                .build()

            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(
                promptInfo,
                BiometricPrompt.CryptoObject(cipher)
            )
        } catch (e: Exception) {
            clearPasskey()
            continuation.resume(null)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        keyStore.getKey(KEYSTORE_ALIAS, null)?.let {
            return it as SecretKey
        }

        val keyGen = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT
                        or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(AUTH_VALIDITY_SECONDS)
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        return keyGen.generateKey()
    }

    private fun deleteKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(KEYSTORE_ALIAS)
        } catch (_: Exception) { }
    }
}
```

### 1.3 Register in Koin

**File:** `composeApp/src/androidMain/kotlin/tech/arnav/twofac/di/AndroidModules.kt`

```kotlin
fun androidBiometricModule(appContext: Context, activityProvider: () -> FragmentActivity) = module {
    single<SessionManager> {
        AndroidBiometricSessionManager(
            context = appContext,
            activityProvider = activityProvider,
        )
    }
}
```

### 1.4 Wire in Application/Activity

**File:** `composeApp/src/androidMain/kotlin/tech/arnav/twofac/TwoFacApplication.kt` (or `MainActivity.kt`)

When starting Koin, include the biometric module:

```kotlin
startKoin {
    // ... existing modules ...
    modules(
        androidBiometricModule(
            appContext = applicationContext,
            activityProvider = { currentActivity as FragmentActivity },
        ),
    )
}
```

### 1.5 Android-Specific Considerations

| Consideration | Detail |
|---|---|
| **Minimum API level** | `BiometricPrompt` requires API 28+ (Android 9.0). For API 23–27, `FingerprintManager` can be used but is deprecated. Recommend API 28+ as minimum for biometric features. |
| **Key invalidation** | Setting `setInvalidatedByBiometricEnrollment(true)` means the key (and stored passkey) is invalidated if the user adds a new fingerprint. This is a security best practice — re-enrollment clears the stored session. |
| **`BIOMETRIC_STRONG` vs `BIOMETRIC_WEAK`** | We use `BIOMETRIC_STRONG` (Class 3) which requires hardware-backed biometrics. This excludes face unlock on some devices but provides stronger security guarantees. |
| **CryptoObject binding** | The `BiometricPrompt` is bound to a `CryptoObject` (cipher), meaning the system verifies that the biometric authentication actually unlocked the key before allowing decryption. This prevents relay attacks. |
| **Activity lifecycle** | `BiometricPrompt` requires a `FragmentActivity`. The activity reference must be valid when the prompt is shown. Using an `activityProvider` lambda ensures we get the current activity. |
| **Negative button** | "Use passkey instead" as the negative button text gives users a clear escape hatch to the manual passkey flow. |
| **`setUserAuthenticationRequired(true)` + validity** | Biometric auth is required to use the key, with a 15-minute time-based validity window (`AUTH_VALIDITY_SECONDS = 15 * 60`). After a successful biometric prompt, the key remains usable for 15 minutes without re-prompting. This constant can be made a user-configurable setting later. |

---

## Phase 2 — iOS BiometricSessionManager

### iOS Biometric Stack

| Component | Purpose |
|---|---|
| `LocalAuthentication` (LAContext) | Face ID / Touch ID biometric prompt |
| `Security` (Keychain Services) | Secure encrypted storage |
| `kSecAttrAccessControl` | Access control policy (biometric binding) |
| `SecAccessControlCreateWithFlags` | Create ACL with `.biometryCurrentSet` |

### Important: Kotlin/Native Interop

Since `composeApp/iosMain` is Kotlin/Native code compiled into the `TwoFacKit` framework, we can call iOS frameworks directly via Kotlin/Native interop. The `platform.LocalAuthentication`, `platform.Security`, and `platform.Foundation` packages are available.

### 2.1 Create BiometricSessionManager

**File:** `composeApp/src/iosMain/kotlin/tech/arnav/twofac/session/IosBiometricSessionManager.kt`

```kotlin
package tech.arnav.twofac.session

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.LocalAuthentication.*
import platform.Security.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IosBiometricSessionManager : SessionManager {

    companion object {
        private const val SERVICE_NAME = "tech.arnav.twofac"
        private const val PASSKEY_ACCOUNT = "twofac_session_passkey"
        private const val PREFS_BIOMETRIC_ENABLED = "twofac_biometric_enabled"
        private const val PREFS_REMEMBER_ENABLED = "twofac_remember_passkey"
    }

    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun isAvailable(): Boolean = true

    override fun isBiometricAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null
        )
    }

    override fun isBiometricEnabled(): Boolean {
        return userDefaults.boolForKey(PREFS_BIOMETRIC_ENABLED)
                && isBiometricAvailable()
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, forKey = PREFS_BIOMETRIC_ENABLED)
        if (!enabled) {
            clearPasskey()
        }
    }

    override fun isRememberPasskeyEnabled(): Boolean {
        return isBiometricEnabled()
                || userDefaults.boolForKey(PREFS_REMEMBER_ENABLED)
    }

    override fun setRememberPasskey(enabled: Boolean) {
        userDefaults.setBool(enabled, forKey = PREFS_REMEMBER_ENABLED)
        if (!enabled) clearPasskey()
    }

    override suspend fun getSavedPasskey(): String? {
        if (!isRememberPasskeyEnabled()) return null

        if (isBiometricEnabled()) {
            return authenticateAndRetrieve()
        }
        return null
    }

    override fun savePasskey(passkey: String) {
        if (!isRememberPasskeyEnabled()) return
        saveToKeychain(passkey)
    }

    override fun clearPasskey() {
        deleteFromKeychain()
    }

    // ── Keychain operations ──

    private fun saveToKeychain(passkey: String) {
        // Delete existing entry first
        deleteFromKeychain()

        val passkeyData = (passkey as NSString)
            .dataUsingEncoding(NSUTF8StringEncoding) ?: return

        // Create access control requiring biometric auth
        val accessControl = SecAccessControlCreateWithFlags(
            kCFAllocatorDefault,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            kSecAccessControlBiometryCurrentSet,  // Invalidated on enrollment change
            null
        ) ?: return

        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to PASSKEY_ACCOUNT,
            kSecValueData to passkeyData,
            kSecAttrAccessControl to accessControl,
        )

        SecItemAdd(query as CFDictionaryRef, null)
    }

    private fun deleteFromKeychain() {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to PASSKEY_ACCOUNT,
        )
        SecItemDelete(query as CFDictionaryRef)
    }

    private suspend fun authenticateAndRetrieve(): String? =
        suspendCoroutine { continuation ->
            val context = LAContext()
            context.localizedFallbackTitle = "Use passkey instead"

            // Check if biometrics are available
            if (!context.canEvaluatePolicy(
                    LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                    error = null
                )) {
                continuation.resume(null)
                return@suspendCoroutine
            }

            context.evaluatePolicy(
                LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = "Unlock TwoFac to access your 2FA codes"
            ) { success, error ->
                if (success) {
                    val passkey = readFromKeychain(context)
                    continuation.resume(passkey)
                } else {
                    continuation.resume(null)
                }
            }
        }

    private fun readFromKeychain(context: LAContext): String? {
        val query = mapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE_NAME,
            kSecAttrAccount to PASSKEY_ACCOUNT,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne,
            kSecUseAuthenticationContext to context,
        )

        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)

            if (status == errSecSuccess) {
                val data = result.value as? NSData ?: return null
                return NSString.create(data, NSUTF8StringEncoding) as? String
            }
        }
        return null
    }
}
```

### 2.2 Register in Koin

**File:** `composeApp/src/iosMain/kotlin/tech/arnav/twofac/di/IosModules.kt`

Add to existing or create new module:

```kotlin
fun iosBiometricModule() = module {
    single<SessionManager> { IosBiometricSessionManager() }
}
```

### 2.3 Wire in iOS entry point

**File:** `composeApp/src/iosMain/kotlin/tech/arnav/twofac/MainViewController.kt`

When starting Koin, include the biometric module:

```kotlin
startKoin {
    // ... existing modules ...
    modules(iosBiometricModule())
}
```

### 2.4 iOS-Specific Considerations

| Consideration | Detail |
|---|---|
| **Face ID usage description** | `Info.plist` must include `NSFaceIDUsageDescription` key with a user-facing explanation. Without this, the app crashes when attempting Face ID. |
| **`kSecAccessControlBiometryCurrentSet`** | Keychain item is invalidated if biometric enrollment changes (e.g., new fingerprint added). More secure than `.biometryAny` which survives enrollment changes. |
| **`kSecAttrAccessibleWhenUnlockedThisDeviceOnly`** | Passkey is only accessible when the device is unlocked, and is not included in iCloud Keychain backups. This prevents the encrypted passkey from leaking to other devices. |
| **LAContext reuse** | The `LAContext` returned by `evaluatePolicy` is passed to `SecItemCopyMatching` via `kSecUseAuthenticationContext`. This proves to the Keychain that the user already authenticated — no double prompt. |
| **Fallback button** | `localizedFallbackTitle = "Use passkey instead"` replaces the default "Enter Password" fallback. When tapped, `evaluatePolicy` returns failure, which causes our code to return `null` and show the passkey dialog. |
| **Simulator testing** | Face ID/Touch ID can be simulated in Xcode: Features → Face ID → Enrolled, then trigger matching/non-matching face. |
| **Kotlin/Native interop** | All iOS framework calls use Kotlin/Native's C interop. `platform.Security.*`, `platform.LocalAuthentication.*`, and `platform.Foundation.*` are available without additional dependencies. |

### 2.5 Info.plist Requirement

**File:** `iosApp/iosApp/Info.plist` (or via Xcode build settings)

```xml
<key>NSFaceIDUsageDescription</key>
<string>TwoFac uses Face ID to securely unlock your 2FA codes without entering a passkey.</string>
```

---

## Phase 3 — Shared UI for Biometric Opt-In

### 3.1 SettingsScreen Updates

**File:** `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`

Add a "Biometric Unlock" toggle below the existing "Remember Passkey" toggle:

```kotlin
// Show biometric option only if the platform supports it
val sessionManager = koinInject<SessionManager?>()

if (sessionManager?.isBiometricAvailable() == true) {
    var isBiometricEnabled by remember {
        mutableStateOf(sessionManager.isBiometricEnabled())
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Biometric Unlock", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Use fingerprint or face recognition to unlock your vault",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = isBiometricEnabled,
            onCheckedChange = { enabled ->
                sessionManager.setBiometricEnabled(enabled)
                isBiometricEnabled = enabled
                // Enabling biometric implicitly enables "remember passkey"
                if (enabled) {
                    sessionManager.setRememberPasskey(true)
                }
            }
        )
    }
}
```

### 3.2 UI Behavior Matrix

| Biometric Available | Biometric Enabled | Remember Passkey | Behavior |
|:---:|:---:|:---:|---|
| ❌ | — | ❌ | Manual passkey entry every time |
| ❌ | — | ✅ | N/A (no biometric toggle shown) |
| ✅ | ❌ | ❌ | Manual passkey entry every time |
| ✅ | ❌ | ✅ | No auto-save — passkey remains in memory for current process lifecycle only (no SessionManager persists it without biometric) |
| ✅ | ✅ | (auto ✅) | Biometric prompt → auto-unlock; fail → passkey dialog |

### 3.3 HomeScreen Flow Update

The auto-unlock flow in `HomeScreen` needs minimal changes because the `SessionManager` interface abstracts the biometric prompt. When `getSavedPasskey()` is called:

- **Browser**: returns passkey from `localStorage` (instant)
- **Android**: shows `BiometricPrompt`, on success decrypts and returns passkey
- **iOS**: shows Face ID/Touch ID prompt, on success reads from Keychain

If any of these return `null`, the existing flow shows the `PasskeyDialog`.

The only change needed is ensuring `getSavedPasskey()` is called as a `suspend` function:

```kotlin
LaunchedEffect(isLoading, hasTriggeredUnlockFlow, isUnlocked) {
    if (!isLoading && !hasTriggeredUnlockFlow && !isUnlocked) {
        hasTriggeredUnlockFlow = true
        val savedPasskey = viewModel.getSavedPasskey()  // now suspend
        if (savedPasskey != null) {
            viewModel.loadAccountsWithOtps(savedPasskey)
            autoUnlockAttempted = true
        } else {
            showPasskeyDialog = true
        }
    }
}
```

---

## Phase 4 — Testing and Validation

### 4.1 Unit Tests

#### Common tests (platform-agnostic)
- Test that `AccountsViewModel` handles `null` session manager gracefully
- Test that `AccountsViewModel` calls `getSavedPasskey()` as suspend function
- Test that `SessionManager` default methods return expected values

#### Android tests
- Test `AndroidBiometricSessionManager.isAvailable()` returns true
- Test `isBiometricAvailable()` returns correct value based on `BiometricManager`
- Test `savePasskey()` + `clearPasskey()` lifecycle
- Test key invalidation when biometric enrollment changes
- Mock `BiometricPrompt` for `authenticateAndDecrypt` flow

#### iOS tests
- Test `IosBiometricSessionManager.isAvailable()` returns true
- Test Keychain save/delete lifecycle
- Test `isBiometricAvailable()` with LAContext mock

### 4.2 Integration Tests

| Scenario | Expected |
|---|---|
| Fresh install, biometric toggle OFF | Manual passkey dialog every app launch |
| Enable biometric, enter passkey, close app, reopen | Biometric prompt → auto-unlock |
| Enable biometric, enter passkey, fail biometric | Passkey dialog appears |
| Enable biometric, cancel biometric prompt | Passkey dialog appears |
| Enable biometric, add new fingerprint, reopen | Key invalidated → passkey dialog (Android); Keychain item invalidated → passkey dialog (iOS) |
| Disable biometric in settings | Stored passkey cleared, next launch shows passkey dialog |
| Device has no biometric hardware | Biometric toggle not shown in settings |
| Device has biometric hardware but none enrolled | Biometric toggle shown but disabled/grayed with hint to enroll |

### 4.3 Manual Testing Checklist

#### Android
- [ ] Test on device with fingerprint sensor
- [ ] Test on device with face unlock (if Class 3 supported)
- [ ] Test on emulator (no biometric → toggle hidden)
- [ ] Test biometric prompt cancellation
- [ ] Test "Use passkey instead" negative button
- [ ] Test with multiple fingerprints enrolled
- [ ] Test adding new fingerprint after enabling biometric auth (key invalidation)
- [ ] Test app backgrounding during biometric prompt

#### iOS
- [ ] Test on device with Face ID
- [ ] Test on device with Touch ID
- [ ] Test on simulator with simulated Face ID
- [ ] Test Face ID failure → passkey fallback
- [ ] Test "Use passkey instead" fallback button
- [ ] Test biometric enrollment change (add new face/finger)
- [ ] Test with Face ID disabled in Settings → toggle hidden
- [ ] Test app backgrounding during Face ID prompt

---

## Implementation Order (Step by Step)

### Step 1: Interface changes (Phase 0)
1. Make `getSavedPasskey()` a `suspend` function in `SessionManager`
2. Update `BrowserSessionManager` to match
3. Add `isBiometricAvailable()`, `isBiometricEnabled()`, `setBiometricEnabled()` default methods
4. Update `AccountsViewModel.getSavedPasskey()` to be a suspend function
5. Verify `HomeScreen` `LaunchedEffect` still compiles (it should — already in coroutine)
6. **Run tests**: `./gradlew :composeApp:test`

### Step 2: Android implementation (Phase 1)
1. Add `androidx.biometric` dependency
2. Create `AndroidBiometricSessionManager`
3. Create `androidBiometricModule` Koin module
4. Wire Koin module in `TwoFacApplication` / `MainActivity`
5. **Test on device/emulator**

### Step 3: iOS implementation (Phase 2)
1. Create `IosBiometricSessionManager`
2. Create `iosBiometricModule` Koin module
3. Wire Koin module in `MainViewController`
4. Add `NSFaceIDUsageDescription` to Info.plist
5. **Test on device/simulator**

### Step 4: UI updates (Phase 3)
1. Add biometric toggle to `SettingsScreen`
2. Ensure `HomeScreen` auto-unlock flow works with suspend `getSavedPasskey()`
3. **End-to-end test on both platforms**

### Step 5: Polish and edge cases (Phase 4)
1. Handle biometric enrollment changes gracefully
2. Handle concurrent biometric prompts (prevent double-prompt)
3. Add loading indicator during biometric prompt
4. Write unit tests for new code
5. **Full test suite pass**

---

## Security Model Summary

```
┌─────────────────────────────────────────────────────────┐
│                    Security Layers                       │
│                                                         │
│  Layer 1: Biometric Authentication                      │
│    Android: BiometricPrompt (Class 3, hardware-backed)  │
│    iOS: LAContext (Face ID / Touch ID)                   │
│                                                         │
│  Layer 2: Encrypted Storage                             │
│    Android: AES-256-GCM via AndroidKeyStore             │
│      → Key bound to biometric (setUserAuth...(true))    │
│      → Key invalidated on enrollment change             │
│    iOS: Keychain with kSecAccessControlBiometryCurrentSet│
│      → Item accessible only after biometric auth        │
│      → Item invalidated on enrollment change            │
│      → Not backed up to iCloud                          │
│                                                         │
│  Layer 3: Passkey-based Vault Encryption (sharedLib)    │
│    → twoFacLib.unlock(passkey) decrypts account store   │
│    → Unchanged by biometric feature                     │
│                                                         │
│  Threat Model:                                          │
│    ✅ Device theft (biometric required to decrypt)      │
│    ✅ New biometric enrollment (key invalidated)        │
│    ✅ Rooted/jailbroken device (hardware-backed keys)   │
│    ✅ Backup extraction (device-only storage, no cloud) │
│    ⚠️ Sophisticated hardware attack on secure enclave   │
│       (out of scope — OS-level threat)                  │
└─────────────────────────────────────────────────────────┘
```

---

## File Change Summary

| File | Change | Phase |
|---|---|---|
| `composeApp/src/commonMain/.../session/SessionManager.kt` | Add `suspend` to `getSavedPasskey()`, add biometric default methods | 0 |
| `composeApp/src/wasmJsMain/.../session/BrowserSessionManager.kt` | Add `suspend` to `getSavedPasskey()` override | 0 |
| `composeApp/src/commonMain/.../viewmodels/AccountsViewModel.kt` | Make `getSavedPasskey()` a suspend function | 0 |
| `composeApp/src/commonMain/.../screens/HomeScreen.kt` | Minor: ensure suspend call in LaunchedEffect | 0 |
| `composeApp/build.gradle.kts` | Add `androidx.biometric` dependency for androidMain | 1 |
| `composeApp/src/androidMain/.../session/AndroidBiometricSessionManager.kt` | **New file** — Android biometric implementation | 1 |
| `composeApp/src/androidMain/.../di/AndroidModules.kt` | Add `androidBiometricModule` | 1 |
| `composeApp/src/androidMain/.../MainActivity.kt` or `TwoFacApplication.kt` | Wire biometric Koin module | 1 |
| `composeApp/src/iosMain/.../session/IosBiometricSessionManager.kt` | **New file** — iOS biometric implementation | 2 |
| `composeApp/src/iosMain/.../di/IosModules.kt` | Add `iosBiometricModule` | 2 |
| `composeApp/src/iosMain/.../MainViewController.kt` | Wire biometric Koin module | 2 |
| `iosApp/iosApp/Info.plist` | Add `NSFaceIDUsageDescription` | 2 |
| `composeApp/src/commonMain/.../screens/SettingsScreen.kt` | Add biometric toggle UI | 3 |

---

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| `suspend` change breaks existing callers | Build failure | Low | Only one implementation (BrowserSessionManager) + ViewModel to update |
| BiometricPrompt requires FragmentActivity | Runtime crash if wrong Activity type | Low | Ensure `ComponentActivity` extends `FragmentActivity` (standard in modern Android) |
| iOS Keychain interop complexity | Compilation errors in K/N | Medium | Reference existing K/N Keychain libraries; test on simulator early |
| Key invalidated unexpectedly | User locked out of auto-unlock | Low | Graceful fallback to passkey dialog; clear stored data |
| Face ID prompt blocks UI thread | ANR or frozen UI | Low | `suspendCoroutine` + callback pattern keeps UI responsive |
| Biometric not available on test devices | Can't test | Low | Use Android emulator biometric simulation + Xcode Face ID simulation |

---

## Future Enhancements

1. **Desktop biometric support** — Windows Hello, macOS Touch ID via `desktopMain` SessionManager
2. **Biometric timeout** — Auto-lock after configurable inactivity period, require re-auth
3. **Multiple auth methods** — Support both PIN and biometric on the same device
4. **Wear OS / watchOS biometric** — Unlock companion apps with wrist detection + device authentication
5. **Passkey-less mode** — For users who only use biometric, consider a mode where the passkey is system-generated and never shown to the user (fully managed by biometric storage)

---

## References

### Android
- [BiometricPrompt API](https://developer.android.com/reference/androidx/biometric/BiometricPrompt)
- [Android Keystore System](https://developer.android.com/privacy-and-security/keystore)
- [KeyGenParameterSpec](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder)
- [Biometric authentication guide](https://developer.android.com/identity/sign-in/biometric-auth)
- [CryptoObject for biometric-bound keys](https://developer.android.com/reference/androidx/biometric/BiometricPrompt.CryptoObject)

### iOS
- [LocalAuthentication Framework](https://developer.apple.com/documentation/localauthentication)
- [Keychain Services](https://developer.apple.com/documentation/security/keychain_services)
- [SecAccessControl](https://developer.apple.com/documentation/security/secaccesscontrol)
- [Protecting keys with Face ID or Touch ID](https://developer.apple.com/documentation/localauthentication/accessing-keychain-items-with-face-id-or-touch-id)
- [LAPolicy](https://developer.apple.com/documentation/localauthentication/lapolicy)

### Kotlin Multiplatform
- [Kotlin/Native interop with iOS frameworks](https://kotlinlang.org/docs/native-objc-interop.html)
- [Kotlin coroutines on Native](https://kotlinlang.org/docs/native-concurrency.html)
- [Koin for KMP](https://insert-koin.io/docs/reference/koin-mp/kmp/)
