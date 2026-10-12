package tech.arnav.twofac.lib

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.importer.ImportAdapter
import tech.arnav.twofac.lib.importer.ImportResult
import tech.arnav.twofac.lib.storage.MemoryStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import tech.arnav.twofac.lib.storage.StoredAccount
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
    fun bulkImportValidatesBeforeWriting() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage(), "123456")
        lib.unlock("123456")
        val error = assertFailsWith<IllegalArgumentException> {
            lib.addAccounts(listOf("otpauth://totp/Example:alice?secret=JBSWY3DPEHPK3PXP&issuer=Example", "invalid secret data"))
        }
        assertEquals("Invalid account import data", error.message)
        assertTrue(lib.getAllAccounts().isEmpty())
    }

    @Test
    fun bulkImportWithWrongPasswordDoesNotWriteAndRelocksVault() = runTest {
        val storage = MemoryStorage()
        val writer = TwoFacLib.initialise(storage, "123456")
        writer.unlock("123456")
        val uri = "otpauth://totp/Example:alice?secret=JBSWY3DPEHPK3PXP&issuer=Example"
        writer.addAccount(uri)
        val importer = TwoFacLib.initialise(storage)
        importer.unlock("incorrect")
        assertFailsWith<IllegalStateException> { importer.addAccounts(listOf(uri)) }
        assertEquals(false, importer.isUnlocked())
        assertEquals(1, storage.getAccountList().size)
        importer.unlock("123456")
        assertEquals(1, importer.addAccounts(listOf(uri)).importedIndices.size)
    }

    @Test
    fun bulkImportReportsIndicesForFailedWritesAndCanRetryOnlyFailures() = runTest {
        val memory = MemoryStorage()
        var writes = 0
        val storage = object : tech.arnav.twofac.lib.storage.Storage by memory {
            override suspend fun saveAccount(account: StoredAccount): Boolean {
                writes++
                if (writes == 2) return false
                return memory.saveAccount(account)
            }
        }
        val lib = TwoFacLib.initialise(storage, "123456")
        lib.unlock("123456")
        val uris = listOf("alice", "bob", "carol").map {
            "otpauth://totp/Example:$it?secret=JBSWY3DPEHPK3PXP&issuer=Example"
        }
        val result = lib.addAccounts(uris)
        assertEquals(listOf(0, 2), result.importedIndices)
        assertEquals(listOf(1), result.failedIndices)
        assertEquals(2, lib.getAllAccounts().size)
        lib.addAccounts(result.failedIndices.map(uris::get))
        assertEquals(3, lib.getAllAccounts().size)
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
