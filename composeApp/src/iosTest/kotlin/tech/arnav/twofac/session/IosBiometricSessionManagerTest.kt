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
        defaults.removePersistentDomainForName("IosBiometricSessionManagerTest")
        return IosBiometricSessionManager(defaults)
    }

    @Test
    fun isAvailableMatchesBiometricAvailability() {
        val manager = createManager()
        assertEquals(manager.isBiometricAvailable(), manager.isAvailable())
    }

    @Test
    fun rememberPasskeyDelegatesToBiometric() {
        val manager = createManager()
        // isRememberPasskeyEnabled is synonymous with isBiometricEnabled
        assertFalse(manager.isRememberPasskeyEnabled())
        assertEquals(manager.isBiometricEnabled(), manager.isRememberPasskeyEnabled())
    }

    @Test
    fun getSavedPasskeyReturnsNullWhenBiometricDisabled() {
        runTest {
            val manager = createManager()
            manager.setBiometricEnabled(false)
            assertNull(manager.getSavedPasskey())
        }
    }

    @Test
    fun savePasskeyIsNoOp() {
        runTest {
            val manager = createManager()
            // savePasskey is a no-op on secure platforms — passkeys only stored via enrollPasskey
            manager.savePasskey("test-passkey")
            assertNull(manager.getSavedPasskey())
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
    fun secureUnlockReadinessRequiresBiometric() {
        runTest {
            val manager = createManager()
            // Without biometric, secure unlock is never ready
            manager.setBiometricEnabled(false)
            assertFalse(manager.isSecureUnlockReady())
        }
    }

    @Test
    fun sessionRetentionDefaultsToPromptEveryTime() {
        val manager = createManager()

        assertTrue(manager.supportsSessionRetention())
        assertEquals(
            SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
            manager.getSecureUnlockRetentionPolicy(),
        )
    }

    @Test
    fun sessionRetentionPolicyRoundTripsThroughUserDefaults() {
        val manager = createManager()

        manager.setSecureUnlockRetentionPolicy(SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION)
        assertEquals(
            SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION,
            manager.getSecureUnlockRetentionPolicy(),
        )

        manager.setSecureUnlockRetentionPolicy(SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME)
        assertEquals(
            SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
            manager.getSecureUnlockRetentionPolicy(),
        )
    }
}
