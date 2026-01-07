package tech.arnav.twofac.lib.importer

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.importer.adapters.AuthyImportAdapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthyImportAdapterTest {

    @Test
    fun testParseAuthyExport() = runTest {
        val adapter = AuthyImportAdapter()

        val exportJson = """
        [
          {
            "secret": "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            "digits": 6,
            "name": "GitHub",
            "issuer": "GitHub",
            "type": "totp",
            "period": 30
          },
          {
            "secret": "JBSWY3DPEHPK3PXP",
            "digits": 6,
            "name": "Google",
            "issuer": "Google",
            "type": "totp",
            "period": 30
          }
        ]
        """.trimIndent()

        val result = adapter.parse(exportJson)

        assertTrue(result is ImportResult.Success, "Import should succeed")
        val uris = (result as ImportResult.Success).otpAuthUris
        assertEquals(2, uris.size, "Should parse 2 tokens")

        // Verify first URI
        assertTrue(uris[0].contains("secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"), "First URI should contain correct secret")
        assertTrue(uris[0].contains("issuer=GitHub"), "First URI should contain issuer")

        // Verify second URI
        assertTrue(uris[1].contains("secret=JBSWY3DPEHPK3PXP"), "Second URI should contain correct secret")
        assertTrue(uris[1].contains("issuer=Google"), "Second URI should contain issuer")
    }

    @Test
    fun testParseAuthyExportWithDefaults() = runTest {
        val adapter = AuthyImportAdapter()

        val exportJson = """
        [
          {
            "secret": "GEZDGNBVGY3TQOJQ",
            "name": "TestAccount"
          }
        ]
        """.trimIndent()

        val result = adapter.parse(exportJson)

        assertTrue(result is ImportResult.Success, "Import should succeed with defaults")
        val uris = (result as ImportResult.Success).otpAuthUris
        assertEquals(1, uris.size, "Should parse 1 token")

        // Should use defaults
        assertTrue(uris[0].contains("secret=GEZDGNBVGY3TQOJQ"), "URI should contain secret")
        // Default digits is 6, won't be in URI (only non-default shown)
        // Default period is 30, won't be in URI
    }

    @Test
    fun testParseAuthyExportWithSHA256() = runTest {
        val adapter = AuthyImportAdapter()

        val exportJson = """
        [
          {
            "secret": "GEZDGNBVGY3TQOJQ",
            "digits": 8,
            "name": "SecureService",
            "issuer": "SecureService",
            "type": "totp",
            "period": 60,
            "algorithm": "SHA256"
          }
        ]
        """.trimIndent()

        val result = adapter.parse(exportJson)

        assertTrue(result is ImportResult.Success, "Import should succeed")
        val uris = (result as ImportResult.Success).otpAuthUris
        assertEquals(1, uris.size, "Should parse 1 token")

        // Verify custom settings
        assertTrue(uris[0].contains("algorithm=SHA256"), "URI should contain SHA256 algorithm")
        assertTrue(uris[0].contains("digits=8"), "URI should contain 8 digits")
        assertTrue(uris[0].contains("period=60"), "URI should contain period 60")
    }

    @Test
    fun testParseInvalidJson() = runTest {
        val adapter = AuthyImportAdapter()

        val invalidJson = "not a valid json"

        val result = adapter.parse(invalidJson)

        assertTrue(result is ImportResult.Failure, "Import should fail for invalid JSON")
    }

    @Test
    fun testParseEmptyArray() = runTest {
        val adapter = AuthyImportAdapter()

        val exportJson = "[]"

        val result = adapter.parse(exportJson)

        assertTrue(result is ImportResult.Failure, "Import should fail for empty array")
    }

    @Test
    fun testGetName() {
        val adapter = AuthyImportAdapter()
        assertEquals("Authy", adapter.getName())
    }
}
