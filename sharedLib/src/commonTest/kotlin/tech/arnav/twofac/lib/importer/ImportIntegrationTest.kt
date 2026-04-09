package tech.arnav.twofac.lib.importer

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.importer.adapters.AuthyImportAdapter
import tech.arnav.twofac.lib.importer.adapters.EnteImportAdapter
import tech.arnav.twofac.lib.importer.adapters.TwoFasImportAdapter
import tech.arnav.twofac.lib.storage.MemoryStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class ImportIntegrationTest {

    @Test
    fun testImportFromTwoFas() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage(), "test-password")
        lib.unlock("test-password")

        val twoFasExport = """
        {
          "services": [
            {
              "name": "GitHub",
              "secret": "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
              "account": "user@example.com",
              "digits": 6,
              "period": 30
            },
            {
              "name": "Google",
              "secret": "JBSWY3DPEHPK3PXP",
              "account": "test@gmail.com"
            }
          ]
        }
        """.trimIndent()

        val adapter = TwoFasImportAdapter()
        val result = lib.importAccounts(adapter, twoFasExport)

        assertTrue(result is ImportResult.Success, "Import should succeed")
        val accounts = lib.getAllAccounts()
        assertEquals(2, accounts.size, "Should have 2 accounts imported")
    }

    @Test
    fun testImportFromAuthy() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage(), "test-password")
        lib.unlock("test-password")

        val authyExport = """
        [
          {
            "secret": "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            "digits": 6,
            "name": "GitHub",
            "issuer": "GitHub",
            "type": "totp",
            "period": 30
          }
        ]
        """.trimIndent()

        val adapter = AuthyImportAdapter()
        val result = lib.importAccounts(adapter, authyExport)

        assertTrue(result is ImportResult.Success, "Import should succeed")
        val accounts = lib.getAllAccounts()
        assertEquals(1, accounts.size, "Should have 1 account imported")
    }

    @Test
    fun testImportFromEntePlaintext() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage(), "test-password")
        lib.unlock("test-password")

        val enteExport = """
        otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub
        otpauth://totp/Google:test@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=Google
        """.trimIndent()

        val adapter = EnteImportAdapter()
        val result = lib.importAccounts(adapter, enteExport)

        assertTrue(result is ImportResult.Success, "Import should succeed")
        val accounts = lib.getAllAccounts()
        assertEquals(2, accounts.size, "Should have 2 accounts imported")
    }

    @Test
    fun testImportWithoutUnlock() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage())
        // Don't unlock the library

        val twoFasExport = """
        {
          "services": [
            {
              "name": "GitHub",
              "secret": "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
              "account": "user@example.com"
            }
          ]
        }
        """.trimIndent()

        val adapter = TwoFasImportAdapter()

        try {
            lib.importAccounts(adapter, twoFasExport)
            fail("Should have thrown IllegalStateException")
        } catch (ex: IllegalStateException) {
            val msg = ex.message ?: ""
            assertTrue(
                msg.contains("No account store found"),
                "Unexpected message: $msg"
            )
        }
    }

    @Test
    fun testImportGeneratesValidOTPs() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage(), "test-password")
        lib.unlock("test-password")

        val twoFasExport = """
        {
          "services": [
            {
              "name": "TestService",
              "secret": "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
              "account": "test@example.com",
              "digits": 6,
              "period": 30
            }
          ]
        }
        """.trimIndent()

        val adapter = TwoFasImportAdapter()
        val importResult = lib.importAccounts(adapter, twoFasExport)

        assertTrue(importResult is ImportResult.Success, "Import should succeed")

        // Verify we can generate OTPs
        val accountOTPs = lib.getAllAccountOTPs(nextOtpShownDuration = -1)
        assertEquals(1, accountOTPs.size, "Should have 1 account with OTP")

        val (account, otp) = accountOTPs[0]
        assertEquals(6, otp.currentOTP.length, "OTP should be 6 digits")
        assertEquals(null, otp.nextOTP, "Next OTP should be null")
        assertTrue(otp.currentOTP.all { it.isDigit() }, "OTP should contain only digits")
        assertTrue(otp.nextOTP == null || otp.nextOTP.all { it.isDigit() }, "Next OTP should be null or contain only digits")

    }

    @Test
    fun testNextOTPGeneration() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage(), "test-password")
        lib.unlock("test-password")

        val twoFasExport = """
        {
          "services": [
            {
              "name": "TestService",
              "secret": "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
              "account": "test@example.com",
              "digits": 6,
              "period": 30
            }
          ]
        }
        """.trimIndent()
        val adapter = TwoFasImportAdapter()
        val importResult = lib.importAccounts(adapter, twoFasExport)

        assertTrue(importResult is ImportResult.Success, "Import should succeed")

        // Verify we can generate OTPs
        val accountOTPs = lib.getAllAccountOTPs(nextOtpShownDuration = 20)
        println(accountOTPs)
        assertEquals(1, accountOTPs.size, "Should have 1 account with OTP")

        val (account, otp) = accountOTPs[0]
        assertEquals(6, otp.currentOTP.length, "OTP should be 6 digits")
        assertNotNull( otp.nextOTP, "Next OTP should not be null")
        assertTrue(otp.currentOTP.all { it.isDigit() }, "OTP should contain only digits")
        assertTrue(otp.nextOTP.all { it.isDigit() }, "Next OTP should contain only digits")


    }
}
