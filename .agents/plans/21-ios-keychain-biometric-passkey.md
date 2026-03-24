---
name: iOS Keychain Biometric Passkey Storage
status: Done
progress:
  - "[x] Phase 0 — Keychain helper utility for generic password CRUD"
  - "[x] Phase 1 — Rewrite IosBiometricSessionManager to use Keychain"
  - "[x] Phase 2 — Remove NSUserDefaults passkey storage and legacy code"
  - "[x] Phase 3 — Build and validate on iOS simulator"
---

# iOS Keychain Biometric Passkey Storage

## Problem

The iOS `IosBiometricSessionManager` currently stores the vault passkey **in plaintext** in `NSUserDefaults`. The methods are misleadingly named `saveToKeychain` / `readFromKeychain` but actually write to `userDefaults.setObject(...)` and read with `userDefaults.stringForKey(...)`. The `requireBiometric` parameter is accepted but completely ignored.

Face ID is used only as a **UI gate** — it prompts the user via `LAContext.evaluatePolicy`, and if they pass, returns the plaintext passkey from NSUserDefaults. There is no encryption at rest.

On a jailbroken or compromised device, the passkey is trivially extractable from the app's UserDefaults plist.

### Current broken flow

```
savePasskey(passkey) → userDefaults.setObject(passkey, PREFS_SAVED_PASSKEY)  // plaintext!
getSavedPasskey()    → LAContext.evaluatePolicy (Face ID gate)
                     → userDefaults.stringForKey(PREFS_SAVED_PASSKEY)        // plaintext read
```

### Android reference (correct implementation)

Android uses AES-256-GCM encryption with a key stored in the Android Keystore. The key is configured with `.setUserAuthenticationRequired(true)` and `AUTH_BIOMETRIC_STRONG`, meaning the hardware-backed key can only be used after biometric authentication. The passkey is encrypted at rest and the encrypted blob + IV are stored in SharedPreferences.

## Goal

Replace NSUserDefaults plaintext storage with **iOS Keychain** using `SecAccessControl` with biometric protection flags. This gives us hardware-backed encryption (via the Secure Enclave) with Face ID/Touch ID required to read the passkey — directly equivalent to Android's Keystore approach.

No migration from NSUserDefaults is required — the app is not yet released to the public.

## Design

### Why Keychain with biometric access control (not manual encryption)

On iOS, the **Keychain is already hardware-encrypted by the Secure Enclave**. When you store an item with `kSecAccessControlBiometryCurrentSet`, the Secure Enclave holds the encryption key and only releases it after successful Face ID/Touch ID authentication. This is the iOS-native equivalent of Android Keystore + AES-GCM.

There is no need to implement our own AES encryption layer on iOS — the Keychain handles it at the hardware level.

### Target flow

```
saveToKeychain(passkey)    → SecItemAdd with kSecAccessControlBiometryCurrentSet
                           → passkey encrypted at rest by Secure Enclave

readFromKeychain()         → SecItemCopyMatching (system auto-prompts Face ID)
                           → on success, returns decrypted passkey
                           → on cancel/fail, returns null

deleteFromKeychain()       → SecItemDelete (no biometric required)
```

### Keychain item attributes

| Attribute | Value |
|-----------|-------|
| `kSecClass` | `kSecClassGenericPassword` |
| `kSecAttrService` | `"tech.arnav.twofac"` |
| `kSecAttrAccount` | `"vault_passkey"` |
| `kSecAttrAccessControl` | `SecAccessControl(biometryCurrentSet, whenUnlockedThisDeviceOnly)` |
| `kSecValueData` | passkey encoded as UTF-8 NSData |

### Access control flags

- **`kSecAttrAccessibleWhenUnlockedThisDeviceOnly`** — item only accessible when device is unlocked, never synced to other devices via iCloud Keychain
- **`kSecAccessControlBiometryCurrentSet`** — requires Face ID / Touch ID authentication using the currently enrolled biometrics; if the user re-enrolls a different face/fingerprint, the item becomes inaccessible (must re-enroll passkey)

## Security comparison after fix

| Aspect | iOS (after) | Android (current) |
|--------|-------------|-------------------|
| Passkey storage | Keychain (Secure Enclave encrypted) | SharedPreferences (AES-GCM encrypted blob) |
| Key protection | Secure Enclave hardware | Android Keystore hardware |
| Biometric requirement | `kSecAccessControlBiometryCurrentSet` | `AUTH_BIOMETRIC_STRONG` + `setUserAuthenticationRequired(true)` |
| Invalidation | Re-enrollment invalidates item | `setInvalidatedByBiometricEnrollment(true)` |
| At-rest encryption | ✅ Hardware-backed | ✅ Hardware-backed |

---

## Implementation Phases

### Phase 0 — Keychain helper utility for generic password CRUD

Create a focused Kotlin/Native helper class that wraps the low-level `platform.Security.*` cinterop calls for Keychain generic password operations. This isolates the C-interop complexity from the session manager.

**File:** `composeApp/src/iosMain/kotlin/tech/arnav/twofac/session/KeychainHelper.kt`

**Responsibilities:**
- `save(service, account, data, accessControl)` → `SecItemDelete` (old) + `SecItemAdd`
- `read(service, account)` → `SecItemCopyMatching` with `kSecReturnData`
- `delete(service, account)` → `SecItemDelete`
- `createBiometricAccessControl()` → `SecAccessControlCreateWithFlags` with `kSecAccessControlBiometryCurrentSet`

**Key Kotlin/Native interop details:**
- Use `@OptIn(ExperimentalForeignApi::class)` for cinterop
- Use `memScoped { }` blocks for native memory management
- Convert passkey string to `NSData` via `string.encodeToByteArray()` → `NSData` for `kSecValueData`
- Convert retrieved `NSData` back to Kotlin `String` for the return value
- Build query dictionaries as Kotlin maps and bridge them to `CFDictionaryRef` via `CFDictionaryCreateMutable` + `CFDictionaryAddValue`
- Handle `OSStatus` return codes: `errSecSuccess`, `errSecItemNotFound`, `errSecUserCanceled`, `errSecAuthFailed`

**Imports required:**
```kotlin
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.NSData
import platform.Security.*
```

### Phase 1 — Rewrite IosBiometricSessionManager to use Keychain

Replace the NSUserDefaults passkey storage with calls to the KeychainHelper.

**File:** `composeApp/src/iosMain/kotlin/tech/arnav/twofac/session/IosBiometricSessionManager.kt`

**Changes:**

1. **`saveToKeychain(passkey, requireBiometric)`** — call `keychainHelper.save()` with biometric access control when `requireBiometric` is true, or without for remember-only mode
2. **`readFromKeychain(context)`** — call `keychainHelper.read()` which triggers system Face ID prompt automatically via `SecItemCopyMatching`; the LAContext parameter becomes unnecessary (system handles it)
3. **`deleteFromKeychain()`** — call `keychainHelper.delete()`
4. **`isSecureUnlockReady()`** — check if a Keychain item exists (use `SecItemCopyMatching` without `kSecReturnData`, just check status)
5. **`authenticateAndRetrieve()`** — simplify: just call `readFromKeychain()` since the Keychain itself triggers Face ID; remove the separate `LAContext.evaluatePolicy` wrapper

**Key simplification:** On iOS, `SecItemCopyMatching` on a biometric-protected item automatically presents the Face ID/Touch ID system prompt. We do NOT need to manually call `LAContext.evaluatePolicy` first — the Keychain handles this. This means `authenticateAndRetrieve()` collapses into a direct `readFromKeychain()` call.

**Keep unchanged:**
- `NSUserDefaults` usage for the boolean preferences (`PREFS_BIOMETRIC_ENABLED`, `PREFS_REMEMBER_ENABLED`) — these are non-sensitive settings, not credentials
- The `BiometricSessionManager` interface contract
- `isBiometricAvailable()` using `LAContext.canEvaluatePolicy` (still needed for feature-gating in UI)

### Phase 2 — Remove NSUserDefaults passkey storage and legacy code

1. Remove `PREFS_SAVED_PASSKEY` constant
2. Remove `userDefaults.setObject(...)` / `userDefaults.stringForKey(...)` calls for passkey
3. Remove the `LAContext` parameter from `readFromKeychain()` (no longer needed)
4. Simplify `authenticateAndRetrieve()` — remove the `evaluatePolicy` wrapper since Keychain handles Face ID
5. Clean up imports: remove `suspendCoroutine` if no longer used, add `platform.Security.*` imports

### Phase 3 — Build and validate on iOS simulator

1. Run `./gradlew --no-daemon :composeApp:compileKotlinIosSimulatorArm64` to verify compilation
2. Run `./gradlew --no-daemon :composeApp:compileKotlinMetadata` to check cross-platform metadata
3. Verify no regressions in `./gradlew --no-daemon :composeApp:desktopTest`

**Note:** Keychain APIs with biometric access control cannot be tested in unit tests — they require a real device or simulator with Face ID enrolled. Manual testing on a simulator with Face ID enabled (`Features > Face ID > Enrolled` in Simulator menu) is required.

---

## Files changed

| File | Change |
|------|--------|
| `composeApp/src/iosMain/kotlin/tech/arnav/twofac/session/KeychainHelper.kt` | **New** — Keychain CRUD helper |
| `composeApp/src/iosMain/kotlin/tech/arnav/twofac/session/IosBiometricSessionManager.kt` | **Rewrite** — replace NSUserDefaults with KeychainHelper calls |

## Risks and considerations

1. **Simulator limitations** — Face ID works in Simulator (`Features > Face ID`) but Keychain behavior may differ from real hardware. On-device testing is recommended before release.
2. **Biometric re-enrollment** — `kSecAccessControlBiometryCurrentSet` invalidates the Keychain item if the user adds/removes a fingerprint or re-enrolls Face ID. The app should handle `errSecAuthFailed` / item-not-found gracefully by prompting the user to re-enter their passkey and re-enroll.
3. **No-biometric fallback** — When `isRememberPasskeyEnabled()` is true but `isBiometricEnabled()` is false (remember without biometric), the passkey should still be stored in Keychain but without the biometric access control flag (using just `kSecAttrAccessibleWhenUnlockedThisDeviceOnly`).
4. **Thread safety** — Keychain calls are synchronous and may block briefly; they should not be called on the main thread for reads (which trigger the Face ID prompt). The current `suspend` wrapper via `suspendCoroutine` handles this.
