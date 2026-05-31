package tech.arnav.twofac.session

/**
 * Session-scoped cache for a decrypted vault passkey.
 *
 * The backend defines what "current session" means. For installed apps this can
 * be process lifetime; for browser extensions it can be the browser session.
 */
interface SessionPasskeyCache {
    suspend fun read(): String?
    fun write(passkey: String)
    fun clear()
}

class InMemorySessionPasskeyCache(
    initialPasskey: String? = null,
) : SessionPasskeyCache {
    private var cachedPasskey: String? = initialPasskey

    override suspend fun read(): String? = cachedPasskey

    override fun write(passkey: String) {
        cachedPasskey = passkey
    }

    override fun clear() {
        cachedPasskey = null
    }
}
