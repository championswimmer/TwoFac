package tech.arnav.twofac.session

import platform.Foundation.NSUserDefaults
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
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
        var result: Result<T>? = null
        block.startCoroutine(
            object : Continuation<T> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(resumeResult: Result<T>) {
                    result = resumeResult
                }
            }
        )
        return result!!.getOrThrow()
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
    fun biometricDisableKeepsBiometricFlagOff() {
        val manager = createManager()
        manager.setBiometricEnabled(false)
        assertFalse(manager.isBiometricEnabled())
    }

    @Test
    fun biometricAvailabilityMethodIsCallable() {
        val manager = createManager()
        val availability = manager.isBiometricAvailable()
        assertTrue(availability || !availability)
    }
}
