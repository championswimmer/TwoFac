package tech.arnav.twofac.session

interface BiometricSessionManager : SecureSessionManager {
    /** Whether biometric authentication is available on this platform/device. */
    fun isBiometricAvailable(): Boolean

    /** Whether biometric authentication is enabled by user preference. */
    fun isBiometricEnabled(): Boolean

    /** Toggle biometric authentication. */
    fun setBiometricEnabled(enabled: Boolean)

    override fun isSecureUnlockAvailable(): Boolean = isBiometricAvailable()

    override fun isSecureUnlockEnabled(): Boolean = isBiometricEnabled()

    override fun setSecureUnlockEnabled(enabled: Boolean) = setBiometricEnabled(enabled)

    /**
     * Enroll the passkey for biometric-based auto-unlock.
     *
     * On Android: triggers BiometricPrompt to authenticate the KeyStore key,
     * then encrypts and saves the passkey.
     * On iOS: saves the passkey to the Keychain with biometric access control.
     *
     * @return true if enrollment succeeded, false if cancelled or failed.
     */
    override suspend fun enrollPasskey(passkey: String): Boolean
}
