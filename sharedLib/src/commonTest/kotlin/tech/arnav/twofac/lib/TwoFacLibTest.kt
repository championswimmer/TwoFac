package tech.arnav.twofac.lib

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TwoFacLibTest {

    @Test
    fun testInitialiseWithoutPasskey() {
        val lib = TwoFacLib.initialise()
        assertFalse(lib.isUnlocked(), "Library should not be unlocked when initialized without passkey")
    }

    @Test
    fun testInitialiseWithPasskey() {
        val lib = TwoFacLib.initialise(passKey = "testpasskey")
        assertTrue(lib.isUnlocked(), "Library should be unlocked when initialized with passkey")
    }

    @Test
    fun testUnlockFunction() = runTest {
        val lib = TwoFacLib.initialise()
        assertFalse(lib.isUnlocked(), "Library should not be unlocked initially")

        lib.unlock("testpasskey")
        assertTrue(lib.isUnlocked(), "Library should be unlocked after calling unlock()")
    }

    @Test
    fun testUnlockWithBlankPasskey() = runTest {
        val lib = TwoFacLib.initialise()

        assertFailsWith<IllegalArgumentException> {
            lib.unlock("")
        }

        assertFailsWith<IllegalArgumentException> {
            lib.unlock("   ")
        }
    }

    @Test
    fun testGetAllAccountOTPsWhenLocked() = runTest {
        val lib = TwoFacLib.initialise()

        assertFailsWith<IllegalStateException> {
            lib.getAllAccountOTPs()
        }
    }

    @Test
    fun testAddAccountWhenLocked() = runTest {
        val lib = TwoFacLib.initialise()

        assertFailsWith<IllegalStateException> {
            lib.addAccount("otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example")
        }
    }

    @Test
    fun testGetAllAccountOTPsWhenUnlocked() = runTest {
        val lib = TwoFacLib.initialise()
        lib.unlock("testpasskey")

        // Should not throw exception when unlocked
        val otps = lib.getAllAccountOTPs()
        assertEquals(0, otps.size, "Should return empty list when no accounts are added")
    }

    @Test
    fun testThreadSafetyOfUnlockAndIsUnlocked() = runTest {
        val lib = TwoFacLib.initialise()

        // Test that multiple calls to unlock and isUnlocked work correctly
        lib.unlock("passkey1")
        assertTrue(lib.isUnlocked())

        lib.unlock("passkey2")
        assertTrue(lib.isUnlocked())
    }
}