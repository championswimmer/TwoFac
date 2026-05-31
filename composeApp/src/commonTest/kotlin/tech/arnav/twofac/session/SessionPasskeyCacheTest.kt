package tech.arnav.twofac.session

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionPasskeyCacheTest {
    @Test
    fun `in-memory cache stores and clears passkey`() = runTest {
        val cache = InMemorySessionPasskeyCache()

        assertNull(cache.read())
        cache.write("pass-123")
        assertEquals("pass-123", cache.read())

        cache.clear()
        assertNull(cache.read())
    }

    @Test
    fun `retention helpers ignore cache while prompt every time is active`() = runTest {
        val cache = InMemorySessionPasskeyCache("cached-passkey")
        val manager = FakeRetentionCapableSecureSessionManager(
            retentionPolicy = SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
        )

        assertNull(manager.readRetainedPasskey(cache))

        manager.writeRetainedPasskey(cache, "next-passkey")
        assertNull(cache.read())
    }

    @Test
    fun `retention helpers read and write cache when current-session retention is enabled`() = runTest {
        val cache = InMemorySessionPasskeyCache()
        val manager = FakeRetentionCapableSecureSessionManager(
            retentionPolicy = SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION,
        )

        manager.writeRetainedPasskey(cache, "pass-123")
        assertEquals("pass-123", manager.readRetainedPasskey(cache))

        manager.clearRetainedPasskey(cache)
        assertNull(cache.read())
    }

    @Test
    fun `unsupported managers ignore retained-passkey writes`() = runTest {
        val cache = InMemorySessionPasskeyCache()
        val manager = FakeRetentionCapableSecureSessionManager(
            retentionPolicy = SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION,
            supportsRetention = false,
        )

        manager.writeRetainedPasskey(cache, "pass-123")

        assertNull(cache.read())
        assertNull(manager.readRetainedPasskey(cache))
    }
}

private class FakeRetentionCapableSecureSessionManager(
    private val retentionPolicy: SecureUnlockRetentionPolicy,
    private val supportsRetention: Boolean = true,
) : SessionRetentionCapableSecureSessionManager {
    override fun isAvailable(): Boolean = true
    override fun isRememberPasskeyEnabled(): Boolean = true
    override fun setRememberPasskey(enabled: Boolean) = Unit
    override suspend fun getSavedPasskey(): String? = null
    override fun savePasskey(passkey: String) = Unit
    override fun clearPasskey() = Unit
    override fun isSecureUnlockAvailable(): Boolean = true
    override fun isSecureUnlockEnabled(): Boolean = true
    override fun isSecureUnlockReady(): Boolean = true
    override fun setSecureUnlockEnabled(enabled: Boolean) = Unit
    override suspend fun enrollPasskey(passkey: String): Boolean = true
    override fun supportsSessionRetention(): Boolean = supportsRetention
    override fun getSecureUnlockRetentionPolicy(): SecureUnlockRetentionPolicy = retentionPolicy
    override fun setSecureUnlockRetentionPolicy(policy: SecureUnlockRetentionPolicy) = Unit
}
