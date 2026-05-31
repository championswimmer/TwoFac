package tech.arnav.twofac.session

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
class IosBiometricSessionManager(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
    private val sessionPasskeyCache: SessionPasskeyCache = InMemorySessionPasskeyCache(),
) : BiometricSessionManager, SessionRetentionCapableSecureSessionManager {

    companion object {
        private const val PREFS_BIOMETRIC_ENABLED = "twofac_biometric_enabled"
        private const val PREFS_RETENTION_POLICY = "twofac_secure_unlock_retention_policy"
        private const val KEYCHAIN_SERVICE = "tech.arnav.twofac"
        private const val KEYCHAIN_ACCOUNT = "vault_passkey"
    }

    override fun isAvailable(): Boolean = isBiometricAvailable()

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
        return isBiometricEnabled() && KeychainHelper.exists(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT)
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, forKey = PREFS_BIOMETRIC_ENABLED)
        if (!enabled) {
            clearPasskey()
        }
    }

    override fun isRememberPasskeyEnabled(): Boolean {
        // On iOS, "remember passkey" is synonymous with biometric unlock.
        return isBiometricEnabled()
    }

    override fun setRememberPasskey(enabled: Boolean) {
        // Delegate to biometric toggle — the two are synonymous on secure platforms.
        setBiometricEnabled(enabled)
    }

    override fun supportsSessionRetention(): Boolean = true

    override fun getSecureUnlockRetentionPolicy(): SecureUnlockRetentionPolicy {
        val storedValue = userDefaults.stringForKey(PREFS_RETENTION_POLICY)
        return storedValue
            ?.let { value -> SecureUnlockRetentionPolicy.entries.firstOrNull { it.name == value } }
            ?: SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME
    }

    override fun setSecureUnlockRetentionPolicy(policy: SecureUnlockRetentionPolicy) {
        userDefaults.setObject(policy.name, forKey = PREFS_RETENTION_POLICY)
        if (policy != SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION) {
            clearRetainedPasskey(sessionPasskeyCache)
        }
    }

    override suspend fun getSavedPasskey(): String? {
        if (!isBiometricEnabled()) return null

        readRetainedPasskey(sessionPasskeyCache)?.let { return it }

        val passkey = authenticateAndRetrieve() ?: return null
        writeRetainedPasskey(sessionPasskeyCache, passkey)
        return passkey
    }

    override fun savePasskey(passkey: String) {
        if (!isBiometricEnabled()) return
        writeRetainedPasskey(sessionPasskeyCache, passkey)
    }

    override fun clearPasskey() {
        clearRetainedPasskey(sessionPasskeyCache)
        KeychainHelper.delete(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT)
    }

    override suspend fun enrollPasskey(passkey: String): Boolean {
        if (!isBiometricAvailable()) {
            println("IosBiometricSessionManager: biometric not available, cannot enroll")
            return false
        }
        val saved = KeychainHelper.save(
            service = KEYCHAIN_SERVICE,
            account = KEYCHAIN_ACCOUNT,
            value = passkey,
            requireBiometric = true,
        )
        if (saved) {
            writeRetainedPasskey(sessionPasskeyCache, passkey)
        }
        println("IosBiometricSessionManager: enrollPasskey save result=$saved")
        return saved
    }

    /**
     * Authenticate with Face ID / Touch ID, then read the passkey from Keychain.
     *
     * We explicitly evaluate LAContext before reading from the Keychain because
     * the iOS Simulator does not enforce biometric access control on Keychain
     * items (known Apple bug r. 82890873). On a real device the Keychain would
     * prompt Face ID automatically, but the explicit LAContext gate ensures
     * consistent behavior on both simulator and device.
     *
     * The authenticated LAContext is passed to KeychainHelper.read() via
     * kSecUseAuthenticationContext so that real devices do not double-prompt.
     */
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
                // Pass the pre-authenticated LAContext to avoid double Face ID prompt on real devices.
                continuation.resume(KeychainHelper.read(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT, context))
            } else {
                continuation.resume(null)
            }
        }
    }
}
