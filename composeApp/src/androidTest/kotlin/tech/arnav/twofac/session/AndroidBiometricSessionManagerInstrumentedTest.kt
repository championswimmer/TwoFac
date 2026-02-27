package tech.arnav.twofac.session

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import tech.arnav.twofac.MainActivity

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
            manager.setRememberPasskey(false)
            assertFalse(manager.isRememberPasskeyEnabled())

            manager.setRememberPasskey(true)
            manager.savePasskey("test-passkey")
            manager.clearPasskey()

            manager.setRememberPasskey(false)
            val savedPasskey = runBlocking { manager.getSavedPasskey() }
            assertNull(savedPasskey)
        }
    }
}
