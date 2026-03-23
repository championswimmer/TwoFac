package tech.arnav.twofac.session

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.Security.SecAccessControlCreateWithFlags
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessControl
import platform.Security.kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecAccessControlBiometryCurrentSet
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecUseOperationPrompt
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class, BetaInteropApi::class)
class IosBiometricSessionManager(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : BiometricSessionManager {

    companion object {
        private const val PREFS_BIOMETRIC_ENABLED = "twofac_biometric_enabled"
        private const val PREFS_REMEMBER_ENABLED = "twofac_remember_passkey"

        private const val KEYCHAIN_SERVICE = "tech.arnav.twofac"
        private const val KEYCHAIN_ACCOUNT = "vault_passkey"
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

    override fun isSecureUnlockReady(): Boolean {
        return isBiometricEnabled() && keychainItemExists()
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
        if (!isRememberPasskeyEnabled()) return null
        // SecItemCopyMatching automatically triggers Face ID / Touch ID when the
        // Keychain item has biometric access control — no separate LAContext needed.
        return readFromKeychain()
    }

    override fun savePasskey(passkey: String) {
        if (!isRememberPasskeyEnabled()) return
        saveToKeychain(passkey, requireBiometric = isBiometricEnabled())
    }

    override fun clearPasskey() {
        deleteFromKeychain()
    }

    override suspend fun enrollPasskey(passkey: String): Boolean {
        if (!isBiometricAvailable()) return false
        saveToKeychain(passkey, requireBiometric = true)
        return true
    }

    // ── Keychain helpers ──────────────────────────────────────────────────

    private fun saveToKeychain(passkey: String, requireBiometric: Boolean) {
        // Always delete first — SecAccessControl can't be changed on an existing item
        deleteFromKeychain()

        @Suppress("CAST_NEVER_SUCCEEDS")
        val passkeyData = (passkey as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        memScoped {
            val cfService = CFBridgingRetain(KEYCHAIN_SERVICE)
            val cfAccount = CFBridgingRetain(KEYCHAIN_ACCOUNT)
            val cfData = CFBridgingRetain(passkeyData)

            try {
                if (requireBiometric) {
                    val accessControl = SecAccessControlCreateWithFlags(
                        allocator = kCFAllocatorDefault,
                        protection = kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
                        flags = kSecAccessControlBiometryCurrentSet,
                        error = null,
                    ) ?: return

                    try {
                        val query = cfDictionaryOf(
                            kSecClass to kSecClassGenericPassword,
                            kSecAttrService to cfService,
                            kSecAttrAccount to cfAccount,
                            kSecValueData to cfData,
                            kSecAttrAccessControl to accessControl,
                        )
                        SecItemAdd(query, null)
                        CFBridgingRelease(query)
                    } finally {
                        CFRelease(accessControl)
                    }
                } else {
                    val cfProtection = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
                    val query = cfDictionaryOf(
                        kSecClass to kSecClassGenericPassword,
                        kSecAttrService to cfService,
                        kSecAttrAccount to cfAccount,
                        kSecValueData to cfData,
                    )
                    SecItemAdd(query, null)
                    CFBridgingRelease(query)
                }
            } finally {
                CFBridgingRelease(cfService)
                CFBridgingRelease(cfAccount)
                CFBridgingRelease(cfData)
            }
        }
    }

    private fun readFromKeychain(): String? = memScoped {
        val cfService = CFBridgingRetain(KEYCHAIN_SERVICE)
        val cfAccount = CFBridgingRetain(KEYCHAIN_ACCOUNT)
        val cfPrompt = CFBridgingRetain("Unlock TwoFac to access your 2FA codes")

        try {
            val query = cfDictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to cfService,
                kSecAttrAccount to cfAccount,
                kSecReturnData to kCFBooleanTrue,
                kSecMatchLimit to kSecMatchLimitOne,
                kSecUseOperationPrompt to cfPrompt,
            )

            val resultRef = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, resultRef.ptr)
            CFBridgingRelease(query)

            if (status != errSecSuccess) return@memScoped null

            val nsData = CFBridgingRelease(resultRef.value) as? NSData ?: return@memScoped null
            NSString.create(nsData, NSUTF8StringEncoding)?.toString()
        } finally {
            CFBridgingRelease(cfService)
            CFBridgingRelease(cfAccount)
            CFBridgingRelease(cfPrompt)
        }
    }

    private fun deleteFromKeychain() = memScoped {
        val cfService = CFBridgingRetain(KEYCHAIN_SERVICE)
        val cfAccount = CFBridgingRetain(KEYCHAIN_ACCOUNT)

        try {
            val query = cfDictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to cfService,
                kSecAttrAccount to cfAccount,
            )
            SecItemDelete(query)
            CFBridgingRelease(query)
        } finally {
            CFBridgingRelease(cfService)
            CFBridgingRelease(cfAccount)
        }
    }

    private fun keychainItemExists(): Boolean = memScoped {
        val cfService = CFBridgingRetain(KEYCHAIN_SERVICE)
        val cfAccount = CFBridgingRetain(KEYCHAIN_ACCOUNT)

        try {
            val query = cfDictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to cfService,
                kSecAttrAccount to cfAccount,
                kSecMatchLimit to kSecMatchLimitOne,
            )
            val status = SecItemCopyMatching(query, null)
            CFBridgingRelease(query)
            status == errSecSuccess
        } finally {
            CFBridgingRelease(cfService)
            CFBridgingRelease(cfAccount)
        }
    }

    // ── CFDictionary construction ─────────────────────────────────────────

    private fun MemScope.cfDictionaryOf(
        vararg items: Pair<CFStringRef?, CFTypeRef?>
    ): CFDictionaryRef? {
        val keys = allocArrayOf(*items.map { it.first }.toTypedArray())
        val values = allocArrayOf(*items.map { it.second }.toTypedArray())
        return CFDictionaryCreate(
            kCFAllocatorDefault,
            keys.reinterpret(),
            values.reinterpret(),
            items.size.convert(),
            null,
            null,
        )
    }
}
