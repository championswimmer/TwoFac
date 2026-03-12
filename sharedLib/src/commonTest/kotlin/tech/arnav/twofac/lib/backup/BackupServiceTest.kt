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
        val service = BackupService(lib, BackupTransportRegistry(listOf(transport)))

        val createResult = service.createBackup(transport.id)
        assertTrue(createResult is BackupResult.Success, "createBackup should succeed")
        assertEquals(1, transport.store.size, "One backup file should be stored")

        // Restore into a fresh lib
        val freshLib = buildLib()
        freshLib.unlock("test-passkey")
        val freshService = BackupService(freshLib, BackupTransportRegistry(listOf(transport)))

        val backupId = createResult.value.id
        val restoreResult = freshService.restoreBackup(transport.id, backupId)

        assertTrue(restoreResult is BackupResult.Success)
        assertEquals(sampleUris.size, restoreResult.value)
        assertEquals(sampleUris.size, freshLib.getAllAccounts().size)
    }

    @Test
    fun testListBackups() = runTest {
        val lib = buildLib()
        lib.unlock("test-passkey")
        sampleUris.forEach { lib.addAccount(it) }

        val transport = FakeTransport()
        val service = BackupService(lib, BackupTransportRegistry(listOf(transport)))

        service.createBackup(transport.id)
        service.createBackup(transport.id)

        val listResult = service.listBackups(transport.id)
        assertTrue(listResult is BackupResult.Success)
        assertEquals(2, listResult.value.size)
    }

    @Test
    fun testRestoreFromMissingBackupReturnsFailure() = runTest {
        val lib = buildLib()
        lib.unlock("test-passkey")
        val transport = FakeTransport()
        val service = BackupService(lib, BackupTransportRegistry(listOf(transport)))

        val result = service.restoreBackup(transport.id, "nonexistent.json")
        assertTrue(result is BackupResult.Failure)
    }

    @Test
    fun testRestoreValidatesPayloadBeforeMutatingVault() = runTest {
        val lib = buildLib()
        lib.unlock("test-passkey")
        sampleUris.take(1).forEach { lib.addAccount(it) }

        val transport = FakeTransport()
        val descriptor = BackupDescriptor(
            id = "corrupt.json",
            transportId = transport.id,
            createdAt = 0,
            byteSize = 0,
        )
        val corruptPayload = BackupPayload(
            createdAt = 0,
            accounts = listOf("this-is-not-an-otpauth-uri"),
        )
        transport.store[descriptor.id] = BackupBlob(
            content = BackupPayloadCodec.encode(corruptPayload),
            descriptor = descriptor,
        )

        val service = BackupService(lib, BackupTransportRegistry(listOf(transport)))
        val result = service.restoreBackup(transport.id, descriptor.id)

        assertTrue(result is BackupResult.Failure)
        assertEquals(1, lib.getAllAccounts().size)
    }

    @Test
    fun testRestoreSkipsAccountsAlreadyPresentByIssuerAccountDigitsSecretAndTimeInterval() = runTest {
        val sourceLib = buildLib()
        sourceLib.unlock("test-passkey")
        sampleUris.forEach { sourceLib.addAccount(it) }

        val transport = FakeTransport()
        val sourceService = BackupService(sourceLib, BackupTransportRegistry(listOf(transport)))
        val createResult = sourceService.createBackup(transport.id)
        assertTrue(createResult is BackupResult.Success)

        val freshLib = buildLib()
        freshLib.unlock("test-passkey")
        freshLib.addAccount(
            "otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub&algorithm=SHA256"
        )

        val restoreService = BackupService(freshLib, BackupTransportRegistry(listOf(transport)))
        val restoreResult = restoreService.restoreBackup(transport.id, createResult.value.id)

        assertTrue(restoreResult is BackupResult.Success)
        assertEquals(1, restoreResult.value, "Only new accounts should be imported from backup")
        assertEquals(2, freshLib.getAllAccounts().size, "Duplicate account should be skipped")
    }

    @Test
    fun testUnknownProviderReturnsFailure() = runTest {
        val lib = buildLib()
        lib.unlock("test-passkey")
        val service = BackupService(lib, BackupTransportRegistry())

        val result = service.createBackup("unknown")
        assertTrue(result is BackupResult.Failure)
    }

    @Test
    fun testExportAccountURIsRequiresUnlock() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage())
        // Not unlocked
        try {
            lib.exportAccountURIs()
            error("Should have thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("not unlocked") == true)
        }
    }
}
