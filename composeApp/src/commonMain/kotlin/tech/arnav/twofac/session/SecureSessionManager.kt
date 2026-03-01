package tech.arnav.twofac.session

/**
 * Session manager contract for secure unlock capable platforms.
 */
interface SecureSessionManager : SessionManager {
    /** Whether secure unlock is available on this platform/device. */
    fun isSecureUnlockAvailable(): Boolean

    /** Whether secure unlock is enabled by user preference. */
    fun isSecureUnlockEnabled(): Boolean

    /** Toggle secure unlock. */
    fun setSecureUnlockEnabled(enabled: Boolean)

    /**
     * Enroll the passkey for secure auto-unlock.
     *
     * @return true if enrollment succeeded, false if cancelled or failed.
     */
    suspend fun enrollPasskey(passkey: String): Boolean
}
