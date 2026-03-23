package tech.arnav.twofac.session

import kotlinx.coroutines.test.runTest
import platform.Foundation.NSUserDefaults
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
        runTest {
            val manager = createManager()
            manager.setRememberPasskey(false)
            assertNull(manager.getSavedPasskey())
        }
    }

    @Test
    fun rememberEnabledWithoutBiometricCanRoundTripPasskey() {
        runTest {
            val manager = createManager()
            manager.setBiometricEnabled(false)
            manager.setRememberPasskey(true)

            manager.savePasskey("test-passkey")

            assertEquals("test-passkey", manager.getSavedPasskey())
        }
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

    @Test
    fun secureUnlockReadinessReflectsStoredPasskey() {
        runTest {
            val manager = createManager()
            manager.setBiometricEnabled(false)
            manager.setRememberPasskey(true)
            assertFalse(manager.isSecureUnlockReady())

            manager.savePasskey("test-passkey")
            assertFalse(manager.isSecureUnlockReady())
        }
    }
}
