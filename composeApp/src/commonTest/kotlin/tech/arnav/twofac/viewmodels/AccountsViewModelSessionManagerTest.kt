package tech.arnav.twofac.viewmodels

import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.session.WebAuthnSessionManager
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AccountsViewModelSessionManagerTest {
    @Test
    fun sessionManagerForPostUnlockEnrollmentReturnsNullForNonSecureManager() {
        val manager = FakeSessionManager()
        val result = sessionManagerForPostUnlockEnrollment(manager, fromAutoUnlock = false)
        assertNull(result)
    }

    @Test
    fun sessionManagerForPostUnlockEnrollmentReturnsNullForAutoUnlock() {
        val manager = FakeSecureSessionManager(secureEnabled = true, secureAvailable = true)
        val result = sessionManagerForPostUnlockEnrollment(manager, fromAutoUnlock = true)
        assertNull(result)
    }

    @Test
    fun sessionManagerForPostUnlockEnrollmentReturnsNullWhenSecureUnlockDisabled() {
        val manager = FakeSecureSessionManager(secureEnabled = false, secureAvailable = true)
        val result = sessionManagerForPostUnlockEnrollment(manager, fromAutoUnlock = false)
        assertNull(result)
    }

    @Test
    fun sessionManagerForPostUnlockEnrollmentReturnsNullWhenSecureUnlockUnavailable() {
        val manager = FakeSecureSessionManager(secureEnabled = true, secureAvailable = false)
        val result = sessionManagerForPostUnlockEnrollment(manager, fromAutoUnlock = false)
        assertNull(result)
    }

    @Test
    fun sessionManagerForPostUnlockEnrollmentReturnsSecureManagerWhenManualUnlockAndEnabled() {
        val manager = FakeSecureSessionManager(secureEnabled = true, secureAvailable = true)
        val result = sessionManagerForPostUnlockEnrollment(manager, fromAutoUnlock = false)
        assertNotNull(result)
    }

    @Test
    fun sessionManagerForPostUnlockEnrollmentReturnsNullWhenWebAuthnAlreadyEnrolled() {
        // If WebAuthn is already enrolled, do NOT re-enroll on every manual unlock.
        val manager = FakeWebAuthnSessionManager(enrolled = true)
        val result = sessionManagerForPostUnlockEnrollment(manager, fromAutoUnlock = false)
        assertNull(result)
    }

    @Test
    fun sessionManagerForPostUnlockEnrollmentReturnsManagerWhenWebAuthnEnabledButNotYetEnrolled() {
        // If WebAuthn is enabled but enrollment is absent, allow enrollment after manual unlock.
        val manager = FakeWebAuthnSessionManager(enrolled = false)
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
) : BaseFakeSessionManager(), SecureSessionManager {
    override fun isRememberPasskeyEnabled(): Boolean = secureEnabled
    override fun isSecureUnlockAvailable(): Boolean = secureAvailable
    override fun isSecureUnlockEnabled(): Boolean = secureEnabled
    override fun setSecureUnlockEnabled(enabled: Boolean) = Unit
    override suspend fun enrollPasskey(passkey: String): Boolean = true
}

private class FakeWebAuthnSessionManager(
    private val enrolled: Boolean,
) : BaseFakeSessionManager(), WebAuthnSessionManager {
    override fun isRememberPasskeyEnabled(): Boolean = true
    override fun isSecureUnlockAvailable(): Boolean = true
    override fun isSecureUnlockEnabled(): Boolean = true
    override fun setSecureUnlockEnabled(enabled: Boolean) = Unit
    override suspend fun enrollPasskey(passkey: String): Boolean = true
    override fun isPasskeyEnrolled(): Boolean = enrolled
}
