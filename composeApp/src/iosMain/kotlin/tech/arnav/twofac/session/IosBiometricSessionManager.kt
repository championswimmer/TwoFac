package tech.arnav.twofac.session

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics

@OptIn(ExperimentalForeignApi::class)
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
        return isBiometricEnabled() && KeychainHelper.exists(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT)
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
        // For biometric-protected items, SecItemCopyMatching automatically
        // triggers the Face ID / Touch ID system prompt.
        // For non-biometric items, it returns the value directly.
        return KeychainHelper.read(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT)
    }

    override fun savePasskey(passkey: String) {
        if (!isRememberPasskeyEnabled()) return
        KeychainHelper.save(
            service = KEYCHAIN_SERVICE,
            account = KEYCHAIN_ACCOUNT,
            value = passkey,
            requireBiometric = isBiometricEnabled(),
        )
    }

    override fun clearPasskey() {
        KeychainHelper.delete(KEYCHAIN_SERVICE, KEYCHAIN_ACCOUNT)
    }

    override suspend fun enrollPasskey(passkey: String): Boolean {
        if (!isBiometricAvailable()) return false
        return KeychainHelper.save(
            service = KEYCHAIN_SERVICE,
            account = KEYCHAIN_ACCOUNT,
            value = passkey,
            requireBiometric = true,
        )
    }
}
