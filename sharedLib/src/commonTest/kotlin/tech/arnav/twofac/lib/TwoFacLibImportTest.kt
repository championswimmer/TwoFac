package tech.arnav.twofac.lib

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.importer.ImportAdapter
import tech.arnav.twofac.lib.importer.ImportResult
import tech.arnav.twofac.lib.storage.MemoryStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TwoFacLibImportTest {

    private class MockAdapter(val success: Boolean, val uris: List<String> = emptyList()) : ImportAdapter {
        override suspend fun parse(fileContent: String, password: String?): ImportResult {
            return if (success) {
                ImportResult.Success(uris)
            } else {
                ImportResult.Failure("Failed to parse")
            }
        }
        override fun getName(): String = "MockAdapter"
    }

    @Test
    fun testImportAccountsSuccess() = runTest {
        val lib = TwoFacLib.initialise(storage = MemoryStorage(), passKey = "testpasskey")
        lib.unlock("testpasskey")
        val uris = listOf(
            "otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub",
            "otpauth://totp/Google:test@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=Google"
        )
        val adapter = MockAdapter(success = true, uris = uris)
        
        val result = lib.importAccounts(adapter, "dummy_content")
        
        assertTrue(result is ImportResult.Success)
        assertEquals(2, result.otpAuthUris.size)
        assertEquals(2, lib.getAllAccounts().size)
    }

    @Test
    fun testImportAccountsFailure() = runTest {
        val lib = TwoFacLib.initialise(storage = MemoryStorage(), passKey = "testpasskey")
        lib.unlock("testpasskey")
        val adapter = MockAdapter(success = false)
        
        val result = lib.importAccounts(adapter, "dummy_content")
        
        assertTrue(result is ImportResult.Failure)
        assertEquals(0, lib.getAllAccounts().size)
    }

    @Test
    fun testImportAccountsPartialSuccess() = runTest {
        val lib = TwoFacLib.initialise(storage = MemoryStorage(), passKey = "testpasskey")
        lib.unlock("testpasskey")
        val uris = listOf(
            "otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub",
            "invalid_uri"
        )
        val adapter = MockAdapter(success = true, uris = uris)
        
        val result = lib.importAccounts(adapter, "dummy_content")
        
        assertTrue(result is ImportResult.Failure)
        assertEquals(1, lib.getAllAccounts().size)
    }
}
