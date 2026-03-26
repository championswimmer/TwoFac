package tech.arnav.twofac.viewmodels

import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AccountsViewModelSessionManagerTest {
    @Test
    fun returnsNullForNonSecureManager() {
        val manager = FakeSessionManager()
        val result = sessionManagerForPostUnlockEnrollment(manager, fromAutoUnlock = false)
        assertNull(result)
    }

    @Test
    fun returnsNullWhenAutoUnlock() {
        val manager = FakeSecureSessionManager(secureEnabled = true, secureAvailable = true)
        val result = sessionManagerForPostUnlockEnrollment(manager, fromAutoUnlock = true)
        assertNull(result)
    }

    @Test
    fun returnsNullWhenAnySecureConditionNotMet() {
        val cases = listOf(
            FakeSecureSessionManager(secureEnabled = false, secureAvailable = true),
            FakeSecureSessionManager(secureEnabled = true, secureAvailable = false),
            FakeSecureSessionManager(secureEnabled = true, secureAvailable = true, secureReady = true),
        )
        cases.forEach { manager ->
            assertNull(sessionManagerForPostUnlockEnrollment(manager, fromAutoUnlock = false))
        }
    }

    @Test
    fun returnsSecureManagerWhenAllConditionsMet() {
        val manager = FakeSecureSessionManager(
            secureEnabled = true,
            secureAvailable = true,
            secureReady = false,
        )
        val result = sessionManagerForPostUnlockEnrollment(manager, fromAutoUnlock = false)
        assertNotNull(result)
    }
}

private open class BaseFakeSessionManager : SessionManager {
    override fun isAvailable(): Boolean = true
    override fun isRememberPasskeyEnabled(): Boolean = false
    override fun setRememberPasskey(enabled: Boolean) = Unit
    override suspend fun getSavedPasskey(): String? = null
    override fun savePasskey(passkey: String) = Unit
    override fun clearPasskey() = Unit
}

private class FakeSessionManager : BaseFakeSessionManager()

private class FakeSecureSessionManager(
    private val secureEnabled: Boolean,
    private val secureAvailable: Boolean,
    private val secureReady: Boolean = false,
) : BaseFakeSessionManager(), SecureSessionManager {
    override fun isRememberPasskeyEnabled(): Boolean = secureEnabled
    override fun isSecureUnlockAvailable(): Boolean = secureAvailable
    override fun isSecureUnlockEnabled(): Boolean = secureEnabled
    override fun isSecureUnlockReady(): Boolean = secureReady
    override fun setSecureUnlockEnabled(enabled: Boolean) = Unit
    override suspend fun enrollPasskey(passkey: String): Boolean = true
}
