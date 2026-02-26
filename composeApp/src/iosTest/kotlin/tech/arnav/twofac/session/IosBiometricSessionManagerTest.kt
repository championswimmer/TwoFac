package tech.arnav.twofac.session

import platform.Foundation.NSUserDefaults
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosBiometricSessionManagerTest {

    private fun createManager(): IosBiometricSessionManager {
        val defaults = NSUserDefaults(suiteName = "IosBiometricSessionManagerTest")
        defaults?.removePersistentDomainForName("IosBiometricSessionManagerTest")
        return IosBiometricSessionManager(defaults ?: NSUserDefaults.standardUserDefaults)
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        // This helper is used only for tests where the suspend function returns immediately
        // (without asynchronous callbacks). It intentionally avoids adding coroutine test deps.
        var result: Result<T>? = null
        block.startCoroutine(
            object : Continuation<T> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(resumeResult: Result<T>) {
                    result = resumeResult
                }
            }
        )
        return requireNotNull(result) {
            "Suspend block did not complete synchronously in this test helper"
        }.getOrThrow()
    }

    @Test
    fun isAvailableReturnsTrue() {
        val manager = createManager()
        assertTrue(manager.isAvailable())
    }

    @Test
    fun rememberPasskeyPreferenceRoundTrips() {
        val manager = createManager()
        assertFalse(manager.isRememberPasskeyEnabled())

        manager.setRememberPasskey(true)
        assertTrue(manager.isRememberPasskeyEnabled())

        manager.setRememberPasskey(false)
        assertFalse(manager.isRememberPasskeyEnabled())
    }

    @Test
    fun getSavedPasskeyReturnsNullWhenRememberDisabled() {
        val manager = createManager()
        manager.setRememberPasskey(false)
        assertNull(runSuspend { manager.getSavedPasskey() })
    }

    @Test
    fun rememberEnabledWithoutBiometricCanRoundTripPasskey() {
        val manager = createManager()
        manager.setBiometricEnabled(false)
        manager.setRememberPasskey(true)

        manager.savePasskey("test-passkey")

        assertEquals("test-passkey", runSuspend { manager.getSavedPasskey() })
    }

    @Test
    fun setBiometricEnabledWithFalseKeepsBiometricDisabled() {
        val manager = createManager()
        manager.setBiometricEnabled(false)
        assertFalse(manager.isBiometricEnabled())
    }

    @Test
    fun biometricEnablementRespectsAvailability() {
        val manager = createManager()
        val availability = manager.isBiometricAvailable()
        manager.setBiometricEnabled(true)
        assertEquals(availability, manager.isBiometricEnabled())
    }
}
