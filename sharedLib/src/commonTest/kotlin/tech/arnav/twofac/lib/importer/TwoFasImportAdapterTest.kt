package tech.arnav.twofac.lib.importer

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.importer.adapters.TwoFasImportAdapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TwoFasImportAdapterTest {

    @Test
    fun testParseTwoFasExport() = runTest {
        val adapter = TwoFasImportAdapter()

        val exportJson = """
        {
          "services": [
            {
              "name": "GitHub",
              "secret": "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
              "account": "user@example.com",
              "digits": 6,
              "period": 30,
              "algorithm": "SHA1"
            },
            {
              "name": "Google",
              "secret": "JBSWY3DPEHPK3PXP",
              "account": "test@gmail.com"
            }
          ]
        }
        """.trimIndent()

        val result = adapter.parse(exportJson)

        assertTrue(result is ImportResult.Success, "Import should succeed")
        val uris = (result as ImportResult.Success).otpAuthUris
        assertEquals(2, uris.size, "Should parse 2 services")

        // Verify first URI
        assertTrue(uris[0].contains("secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"), "First URI should contain correct secret")
        assertTrue(uris[0].contains("issuer=GitHub"), "First URI should contain issuer")

        // Verify second URI
        assertTrue(uris[1].contains("secret=JBSWY3DPEHPK3PXP"), "Second URI should contain correct secret")
        assertTrue(uris[1].contains("issuer=Google"), "Second URI should contain issuer")
    }

    @Test
    fun testParseTwoFasExportWithOtpSection() = runTest {
        val adapter = TwoFasImportAdapter()

        val exportJson = """
        {
          "services": [
            {
              "name": "TestService",
              "secret": "GEZDGNBVGY3TQOJQ",
              "otp": {
                "account": "otp-user@example.com",
                "digits": 8,
                "period": 60,
                "algorithm": "SHA256"
              }
            }
          ]
        }
        """.trimIndent()

        val result = adapter.parse(exportJson)

        assertTrue(result is ImportResult.Success, "Import should succeed")
        val uris = (result as ImportResult.Success).otpAuthUris
        assertEquals(1, uris.size, "Should parse 1 service")

        // Verify URI with otp section values
        assertTrue(uris[0].contains("digits=8"), "URI should use digits from otp section")
        assertTrue(uris[0].contains("period=60"), "URI should use period from otp section")
        assertTrue(uris[0].contains("algorithm=SHA256"), "URI should use algorithm from otp section")
    }

    @Test
    fun testParseInvalidJson() = runTest {
        val adapter = TwoFasImportAdapter()

        val invalidJson = "not a valid json"

        val result = adapter.parse(invalidJson)

        assertTrue(result is ImportResult.Failure, "Import should fail for invalid JSON")
    }

    @Test
    fun testParseEmptyServices() = runTest {
        val adapter = TwoFasImportAdapter()

        val exportJson = """
        {
          "services": []
        }
        """.trimIndent()

        val result = adapter.parse(exportJson)

        assertTrue(result is ImportResult.Failure, "Import should fail for empty services")
    }

    @Test
    fun testGetName() {
        val adapter = TwoFasImportAdapter()
        assertEquals("2FAS Authenticator", adapter.getName())
    }
}
