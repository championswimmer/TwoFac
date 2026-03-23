package tech.arnav.twofac.session

/**
 * Session manager contract for secure unlock capable platforms.
 */
interface SecureSessionManager : SessionManager {
    /** Whether secure unlock is available on this platform/device. */
    fun isSecureUnlockAvailable(): Boolean

    /** Whether secure unlock is enabled by user preference. */
    fun isSecureUnlockEnabled(): Boolean

    /**
     * Whether secure unlock is ready to be attempted immediately.
     *
     * Readiness is stronger than enabled+available and implies required
     * secure enrollment material has already been persisted.
     */
    fun isSecureUnlockReady(): Boolean

    /** Toggle secure unlock. */
    fun setSecureUnlockEnabled(enabled: Boolean)

    /**
     * Enroll the passkey for secure auto-unlock.
     *
     * @return true if enrollment succeeded, false if cancelled or failed.
     */
    suspend fun enrollPasskey(passkey: String): Boolean
}
