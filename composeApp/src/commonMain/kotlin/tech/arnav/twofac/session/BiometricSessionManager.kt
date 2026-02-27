package tech.arnav.twofac.session

interface BiometricSessionManager : SessionManager {
    /** Whether biometric authentication is available on this platform/device. */
    fun isBiometricAvailable(): Boolean

    /** Whether biometric authentication is enabled by user preference. */
    fun isBiometricEnabled(): Boolean

    /** Toggle biometric authentication. */
    fun setBiometricEnabled(enabled: Boolean)
}
