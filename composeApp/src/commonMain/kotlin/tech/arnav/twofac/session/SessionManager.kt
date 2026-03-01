package tech.arnav.twofac.session

/**
 * Manages passkey session persistence.
 *
 * This contract stays stable across platforms and remains the primary
 * integration seam for unlock flows.
 *
 * Platform implementations may choose to disable persisted passkeys when secure
 * platform capabilities are unavailable (manual passkey entry only).
 */
interface SessionManager {
    /** Whether this platform supports session persistence at all. */
    fun isAvailable(): Boolean

    /** Whether the user has opted in to remembering the passkey. */
    fun isRememberPasskeyEnabled(): Boolean

    /** Toggle the "remember passkey" preference. Disabling also clears any saved passkey. */
    fun setRememberPasskey(enabled: Boolean)

    /** Return the previously saved passkey, or null if none is stored or the feature is disabled. */
    suspend fun getSavedPasskey(): String?

    /** Persist the passkey (only effective when the feature is enabled). */
    fun savePasskey(passkey: String)

    /** Remove any stored passkey. */
    fun clearPasskey()
}
