package tech.arnav.twofac.session

/**
 * A no-op backend used when the current platform doesn't support secure unlock.
 */
class UnsupportedDesktopSecureUnlockBackend : DesktopSecureUnlockBackend {
    override fun isAvailable(): Boolean = false
    override fun supportsStrongUserPresence(): Boolean = false
    override fun currentMode(): DesktopSecureUnlockMode = DesktopSecureUnlockMode.NONE
    override suspend fun getSavedPasskey(): String? = null
    override fun savePasskey(passkey: String) { /* no-op */ }
    override fun clearPasskey() { /* no-op */ }
    override fun hasStoredPasskey(): Boolean = false
    override suspend fun enrollPasskey(passkey: String): Boolean = false
}
