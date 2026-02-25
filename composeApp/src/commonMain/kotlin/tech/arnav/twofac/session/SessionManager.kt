package tech.arnav.twofac.session

/**
 * Manages passkey session persistence.
 *
 * On the browser-extension target this stores the passkey so the user
 * does not have to re-enter it every time the extension popup is opened.
 */
interface SessionManager {
    /** Whether this platform supports session persistence at all. */
    fun isAvailable(): Boolean

    /** Whether the user has opted in to remembering the passkey. */
    fun isRememberPasskeyEnabled(): Boolean

    /** Toggle the "remember passkey" preference. Disabling also clears any saved passkey. */
    fun setRememberPasskey(enabled: Boolean)

    /** Return the previously saved passkey, or null if none is stored or the feature is disabled. */
    fun getSavedPasskey(): String?

    /** Persist the passkey (only effective when the feature is enabled). */
    fun savePasskey(passkey: String)

    /** Remove any stored passkey. */
    fun clearPasskey()
}
