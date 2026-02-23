package tech.arnav.twofac.lib.backup

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.MemoryStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupServiceTest {

    private val sampleUris = listOf(
        "otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub",
        "otpauth://totp/Google:test@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=Google",
    )

    private fun buildLib(): TwoFacLib {
        return TwoFacLib.initialise(MemoryStorage(), "test-passkey")
    }

    /** In-memory transport for testing */
    private class FakeTransport : BackupTransport {
        override val id = "fake"
        val store = mutableMapOf<String, BackupBlob>()

        override suspend fun isAvailable() = true

        override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> =
            BackupResult.Success(store.values.map { it.descriptor })

        override suspend fun upload(
            content: ByteArray,
            descriptor: BackupDescriptor
        ): BackupResult<BackupDescriptor> {
            store[descriptor.id] = BackupBlob(content, descriptor)
            return BackupResult.Success(descriptor)
        }

        override suspend fun download(backupId: String): BackupResult<BackupBlob> {
            val blob = store[backupId]
                ?: return BackupResult.Failure("Backup not found: $backupId")
            return BackupResult.Success(blob)
        }

        override suspend fun delete(backupId: String): BackupResult<Unit> {
            store.remove(backupId)
            return BackupResult.Success(Unit)
        }
    }

    @Test
    fun testCreateAndRestoreBackup() = runTest {
        val lib = buildLib()
        lib.unlock("test-passkey")
        sampleUris.forEach { lib.addAccount(it) }

        val transport = FakeTransport()
        val service = BackupService(lib)

        val createResult = service.createBackup(transport)
        assertTrue(createResult is BackupResult.Success, "createBackup should succeed")
        assertEquals(1, transport.store.size, "One backup file should be stored")

        // Restore into a fresh lib
        val freshLib = buildLib()
        freshLib.unlock("test-passkey")
        val freshService = BackupService(freshLib)

        val backupId = (createResult as BackupResult.Success).value.id
        val restoreResult = freshService.restoreBackup(transport, backupId)

        assertTrue(restoreResult is BackupResult.Success)
        assertEquals(sampleUris.size, (restoreResult as BackupResult.Success).value)
        assertEquals(sampleUris.size, freshLib.getAllAccounts().size)
    }

    @Test
    fun testListBackups() = runTest {
        val lib = buildLib()
        lib.unlock("test-passkey")
        sampleUris.forEach { lib.addAccount(it) }

        val transport = FakeTransport()
        val service = BackupService(lib)

        service.createBackup(transport)
        service.createBackup(transport)

        val listResult = service.listBackups(transport)
        assertTrue(listResult is BackupResult.Success)
        assertEquals(2, (listResult as BackupResult.Success).value.size)
    }

    @Test
    fun testRestoreFromMissingBackupReturnsFailure() = runTest {
        val lib = buildLib()
        lib.unlock("test-passkey")
        val service = BackupService(lib)
        val transport = FakeTransport()

        val result = service.restoreBackup(transport, "nonexistent.json")
        assertTrue(result is BackupResult.Failure)
    }

    @Test
    fun testExportAccountURIsRequiresUnlock() = runTest {
        val lib = buildLib()
        // Not unlocked
        try {
            lib.exportAccountURIs()
            error("Should have thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("not unlocked") == true)
        }
    }
}
