package tech.arnav.twofac.session

/**
 * Platform-specific backend for desktop secure unlock.
 *
 * Each desktop OS (macOS, Windows, Linux) provides its own implementation
 * that handles the OS-specific secure storage and authentication mechanisms.
 *
 * This interface is intentionally desktop-only and should not leak into
 * commonMain. The rest of the app accesses secure unlock through the
 * platform-agnostic [BiometricSessionManager] interface.
 */
interface DesktopSecureUnlockBackend {

    /** Whether this backend is available on the current system. */
    fun isAvailable(): Boolean

    /** Whether secure unlock with strong user presence is available. */
    fun supportsStrongUserPresence(): Boolean

    /** The current secure unlock mode provided by this backend. */
    fun currentMode(): DesktopSecureUnlockMode

    /**
     * Retrieve the saved passkey from secure storage.
     *
     * On macOS with biometric protection, this will prompt for Touch ID.
     * On other platforms, this reads from the secure store without prompting.
     *
     * @return The saved passkey, or null if not stored or retrieval failed.
     */
    suspend fun getSavedPasskey(): String?

    /**
     * Save the passkey to secure storage.
     *
     * @param passkey The passkey to securely store.
     */
    fun savePasskey(passkey: String)

    /**
     * Remove the passkey from secure storage.
     */
    fun clearPasskey()

    /**
     * Check if a passkey is currently stored.
     *
     * This should NOT trigger any authentication prompts.
     * Used to determine if secure unlock is "ready" (enrolled).
     */
    fun hasStoredPasskey(): Boolean

    /**
     * Enroll a passkey for secure auto-unlock.
     *
     * On macOS, this stores the passkey with biometric access control.
     * On Windows, this stores with DPAPI protection.
     * On Linux, this stores via Secret Service.
     *
     * @param passkey The passkey to enroll.
     * @return true if enrollment succeeded, false if cancelled or failed.
     */
    suspend fun enrollPasskey(passkey: String): Boolean
}
