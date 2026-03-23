package tech.arnav.twofac.session

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUserDefaults
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
class IosBiometricSessionManager(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : BiometricSessionManager {

    companion object {
        private const val PREFS_BIOMETRIC_ENABLED = "twofac_biometric_enabled"
        private const val PREFS_REMEMBER_ENABLED = "twofac_remember_passkey"
        private const val PREFS_SAVED_PASSKEY = "twofac_saved_passkey"
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
        return isBiometricEnabled() && !readFromKeychain().isNullOrBlank()
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
        return if (isBiometricEnabled()) {
            authenticateAndRetrieve()
        } else {
            readFromKeychain()
        }
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

    private fun saveToKeychain(passkey: String, requireBiometric: Boolean) {
        userDefaults.setObject(passkey, forKey = PREFS_SAVED_PASSKEY)
    }

    private fun deleteFromKeychain() {
        userDefaults.removeObjectForKey(PREFS_SAVED_PASSKEY)
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

    private fun readFromKeychain(context: LAContext? = null): String? {
        return userDefaults.stringForKey(PREFS_SAVED_PASSKEY)
    }
}
