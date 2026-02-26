package tech.arnav.twofac.session

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDefaults
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAccessControlBiometryCurrentSet
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecUseAuthenticationContext
import platform.Security.kSecValueData
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
class IosBiometricSessionManager(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : SessionManager {

    companion object {
        private const val SERVICE_NAME = "tech.arnav.twofac"
        private const val PASSKEY_ACCOUNT = "twofac_session_passkey"
        private const val PREFS_BIOMETRIC_ENABLED = "twofac_biometric_enabled"
        private const val PREFS_REMEMBER_ENABLED = "twofac_remember_passkey"
    }

    override fun isAvailable(): Boolean = true

    override fun isBiometricAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null,
        )
    }

    override fun isBiometricEnabled(): Boolean {
        return userDefaults.boolForKey(PREFS_BIOMETRIC_ENABLED) && isBiometricAvailable()
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, forKey = PREFS_BIOMETRIC_ENABLED)
        if (!enabled) {
            clearPasskey()
        }
    }

    override fun isRememberPasskeyEnabled(): Boolean {
        return isBiometricEnabled() || userDefaults.boolForKey(PREFS_REMEMBER_ENABLED)
    }

    override fun setRememberPasskey(enabled: Boolean) {
        userDefaults.setBool(enabled, forKey = PREFS_REMEMBER_ENABLED)
        if (!enabled) {
            clearPasskey()
        }
    }

    override suspend fun getSavedPasskey(): String? {
        if (!isRememberPasskeyEnabled() || !isBiometricEnabled()) return null
        return authenticateAndRetrieve()
    }

    override fun savePasskey(passkey: String) {
        if (!isRememberPasskeyEnabled()) return
        saveToKeychain(passkey)
    }

    override fun clearPasskey() {
        deleteFromKeychain()
    }

    private fun saveToKeychain(passkey: String) {
        deleteFromKeychain()

        val passkeyData = (passkey as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        val accessControl = SecAccessControlCreateWithFlags(
            allocator = kCFAllocatorDefault,
            protection = kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            flags = kSecAccessControlBiometryCurrentSet,
            error = null,
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

    private suspend fun authenticateAndRetrieve(): String? = suspendCoroutine { continuation ->
        val context = LAContext()
        context.localizedFallbackTitle = "Use passkey instead"

        if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)) {
            continuation.resume(null)
            return@suspendCoroutine
        }

        context.evaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = "Unlock TwoFac to access your 2FA codes",
        ) { success, _ ->
            if (success) {
                continuation.resume(readFromKeychain(context))
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
                return NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
            }
        }
        return null
    }
}
