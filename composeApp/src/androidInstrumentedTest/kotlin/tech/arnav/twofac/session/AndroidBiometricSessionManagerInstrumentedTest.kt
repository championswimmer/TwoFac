package tech.arnav.twofac.session

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.arnav.twofac.app.MainActivity

@RunWith(AndroidJUnit4::class)
class AndroidBiometricSessionManagerInstrumentedTest {

    @Test
    fun basicSessionMethodsAreCallable() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var manager: AndroidBiometricSessionManager
            scenario.onActivity { activity ->
                manager = AndroidBiometricSessionManager(
                    context = ApplicationProvider.getApplicationContext(),
                    activityProvider = { activity },
                )
            }

            assertTrue(manager.isAvailable())
            val biometricAvailable = manager.isBiometricAvailable()
            manager.setBiometricEnabled(true)
            if (biometricAvailable) {
                assertTrue(manager.isBiometricEnabled())
            } else {
                assertFalse(manager.isBiometricEnabled())
            }
            manager.setBiometricEnabled(false)
            assertFalse(manager.isBiometricEnabled())
            manager.setRememberPasskey(false)
            assertFalse(manager.isRememberPasskeyEnabled())

            assertTrue(manager.supportsSessionRetention())
            assertEquals(
                SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
                manager.getSecureUnlockRetentionPolicy(),
            )
            manager.setSecureUnlockRetentionPolicy(SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION)
            assertEquals(
                SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION,
                manager.getSecureUnlockRetentionPolicy(),
            )

            manager.setRememberPasskey(true)
            manager.savePasskey("test-passkey")
            manager.clearPasskey()

            manager.setRememberPasskey(false)
            val savedPasskey = runBlocking { manager.getSavedPasskey() }
            assertNull(savedPasskey)
        }
    }
}
