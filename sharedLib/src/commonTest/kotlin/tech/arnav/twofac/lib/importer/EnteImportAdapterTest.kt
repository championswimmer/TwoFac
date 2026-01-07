package tech.arnav.twofac.lib.importer

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.importer.adapters.EnteImportAdapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnteImportAdapterTest {

    @Test
    fun testParsePlaintextExport() = runTest {
        val adapter = EnteImportAdapter()

        val exportText = """
        otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub
        otpauth://totp/Google:test@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=Google&period=30
        """.trimIndent()

        val result = adapter.parse(exportText)

        assertTrue(result is ImportResult.Success, "Import should succeed")
        val uris = (result as ImportResult.Success).otpAuthUris
        assertEquals(2, uris.size, "Should parse 2 URIs")

        // Verify URIs
        assertTrue(uris[0].contains("secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"), "First URI should contain correct secret")
        assertTrue(uris[1].contains("secret=JBSWY3DPEHPK3PXP"), "Second URI should contain correct secret")
    }

    @Test
    fun testParsePlaintextWithWhitespace() = runTest {
        val adapter = EnteImportAdapter()

        val exportText = """
        
        otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub
        
        otpauth://totp/Google:test@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=Google
        
        """.trimIndent()

        val result = adapter.parse(exportText)

        assertTrue(result is ImportResult.Success, "Import should succeed with whitespace")
        val uris = (result as ImportResult.Success).otpAuthUris
        assertEquals(2, uris.size, "Should parse 2 URIs, ignoring empty lines")
    }

    @Test
    fun testParseEncryptedExport() = runTest {
        val adapter = EnteImportAdapter()

        val encryptedExport = """
        {
          "version": 1,
          "kdfParams": {
            "memLimit": 4096,
            "opsLimit": 3,
            "salt": "dGVzdHNhbHQ="
          },
          "encryptedData": "ZW5jcnlwdGVkZGF0YQ==",
          "encryptionNonce": "bm9uY2U="
        }
        """.trimIndent()

        val result = adapter.parse(encryptedExport, "password")

        assertTrue(result is ImportResult.Failure, "Encrypted import should fail with explanation")
        assertTrue(
            (result as ImportResult.Failure).message.contains("not yet supported"),
            "Error message should explain encrypted exports aren't supported"
        )
    }

    @Test
    fun testParseEmptyPlaintext() = runTest {
        val adapter = EnteImportAdapter()

        val exportText = """
        
        
        """.trimIndent()

        val result = adapter.parse(exportText)

        assertTrue(result is ImportResult.Failure, "Import should fail for empty plaintext")
    }

    @Test
    fun testParseInvalidPlaintext() = runTest {
        val adapter = EnteImportAdapter()

        val exportText = """
        some random text
        not an otpauth URI
        """.trimIndent()

        val result = adapter.parse(exportText)

        assertTrue(result is ImportResult.Failure, "Import should fail for invalid plaintext")
    }

    @Test
    fun testGetName() {
        val adapter = EnteImportAdapter()
        assertEquals("Ente Auth", adapter.getName())
    }

    @Test
    fun testRequiresPassword() {
        val adapter = EnteImportAdapter()
        assertEquals(false, adapter.requiresPassword(), "Plaintext exports don't require password")
    }
}
