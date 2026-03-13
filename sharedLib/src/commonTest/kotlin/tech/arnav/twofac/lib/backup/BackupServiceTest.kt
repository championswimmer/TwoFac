package tech.arnav.twofac.lib.backup

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.MemoryStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackupServiceTest {

    private companion object {
        const val TEST_PASSKEY = "test-passkey"
        const val BACKUP_PASSKEY = "backup-passkey"
        const val CURRENT_PASSKEY = "current-passkey"
    }

    private val sampleUris = listOf(
        "otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub",
        "otpauth://totp/Google:test@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=Google",
    )

    private fun buildLib(passkey: String = TEST_PASSKEY): TwoFacLib {
        return TwoFacLib.initialise(MemoryStorage(), passkey)
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
        lib.unlock(TEST_PASSKEY)
        sampleUris.forEach { lib.addAccount(it) }

        val transport = FakeTransport()
        val service = BackupService(lib, BackupTransportRegistry(listOf(transport)))

        val createResult = service.createBackup(transport.id)
        assertTrue(createResult is BackupResult.Success, "createBackup should succeed")
        assertEquals(1, transport.store.size, "One backup file should be stored")

        // Restore into a fresh lib
        val freshLib = buildLib()
        freshLib.unlock(TEST_PASSKEY)
        val freshService = BackupService(freshLib, BackupTransportRegistry(listOf(transport)))

        val backupId = createResult.value.id
        val restoreResult = freshService.restoreBackup(transport.id, backupId)

        assertTrue(restoreResult is BackupResult.Success)
        assertEquals(sampleUris.size, restoreResult.value)
        assertEquals(sampleUris.size, freshLib.getAllAccounts().size)
    }

    @Test
    fun testCreatePlaintextBackupWritesPlaintextAccounts() = runTest {
        val lib = buildLib()
        lib.unlock(TEST_PASSKEY)
        sampleUris.forEach { lib.addAccount(it) }

        val transport = FakeTransport()
        val service = BackupService(lib, BackupTransportRegistry(listOf(transport)))

        val createResult = service.createBackup(transport.id, encrypted = false)

        assertTrue(createResult is BackupResult.Success)
        val payload = BackupPayloadCodec.decode(transport.store.getValue(createResult.value.id).content)
        assertFalse(payload.encrypted)
        assertEquals(lib.exportAccountsPlaintext(), payload.accounts)
        assertTrue(payload.encryptedAccounts.isEmpty())
    }

    @Test
    fun testCreateEncryptedBackupWritesEncryptedAccounts() = runTest {
        val lib = buildLib()
        lib.unlock(TEST_PASSKEY)
        sampleUris.forEach { lib.addAccount(it) }

        val transport = FakeTransport()
        val service = BackupService(lib, BackupTransportRegistry(listOf(transport)))

        val createResult = service.createBackup(transport.id, encrypted = true)

        assertTrue(createResult is BackupResult.Success)
        val payload = BackupPayloadCodec.decode(transport.store.getValue(createResult.value.id).content)
        assertTrue(payload.encrypted)
        assertTrue(payload.accounts.isEmpty())
        assertEquals(lib.exportAccountsEncrypted().map { it.accountLabel }, payload.encryptedAccounts.map { it.accountLabel })
        assertEquals(lib.exportAccountsEncrypted().map { it.salt }, payload.encryptedAccounts.map { it.salt })
        assertEquals(lib.exportAccountsEncrypted().map { it.encryptedURI }, payload.encryptedAccounts.map { it.encryptedURI })
    }

    @Test
    fun testListBackups() = runTest {
        val lib = buildLib()
        lib.unlock(TEST_PASSKEY)
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
        lib.unlock(TEST_PASSKEY)
        val transport = FakeTransport()
        val service = BackupService(lib, BackupTransportRegistry(listOf(transport)))

        val result = service.restoreBackup(transport.id, "nonexistent.json")
        assertTrue(result is BackupResult.Failure)
    }

    @Test
    fun testInspectBackupReportsEncryptedPayload() = runTest {
        val lib = buildLib(BACKUP_PASSKEY)
        lib.unlock(BACKUP_PASSKEY)
        sampleUris.forEach { lib.addAccount(it) }

        val transport = FakeTransport()
        val service = BackupService(lib, BackupTransportRegistry(listOf(transport)))
        val createResult = service.createBackup(transport.id, encrypted = true)

        assertTrue(createResult is BackupResult.Success)
        val inspectResult = service.inspectBackup(transport.id, createResult.value.id)

        assertTrue(inspectResult is BackupResult.Success)
        assertTrue(inspectResult.value.encrypted)
        assertEquals(sampleUris.size, inspectResult.value.encryptedAccounts.size)
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
        sourceLib.unlock(TEST_PASSKEY)
        sampleUris.forEach { sourceLib.addAccount(it) }

        val transport = FakeTransport()
        val sourceService = BackupService(sourceLib, BackupTransportRegistry(listOf(transport)))
        val createResult = sourceService.createBackup(transport.id)
        assertTrue(createResult is BackupResult.Success)

        val freshLib = buildLib()
        freshLib.unlock(TEST_PASSKEY)
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
        lib.unlock(TEST_PASSKEY)
        val service = BackupService(lib, BackupTransportRegistry())

        val result = service.createBackup("unknown")
        assertTrue(result is BackupResult.Failure)
    }

    @Test
    fun testExportAccountsRequireUnlock() = runTest {
        val lib = TwoFacLib.initialise(MemoryStorage())
        // Not unlocked
        try {
            lib.exportAccountsPlaintext()
            error("Should have thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("not unlocked") == true)
        }

        try {
            lib.exportAccountsEncrypted()
            error("Should have thrown")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("not unlocked") == true)
        }
    }

    @Test
    fun testRestoreEncryptedBackupDecryptsWithBackupPasskeyAndReencryptsWithCurrentPasskey() = runTest {
        val sourceLib = buildLib(BACKUP_PASSKEY)
        sourceLib.unlock(BACKUP_PASSKEY)
        sampleUris.forEach { sourceLib.addAccount(it) }

        val transport = FakeTransport()
        val sourceService = BackupService(sourceLib, BackupTransportRegistry(listOf(transport)))
        val createResult = sourceService.createBackup(transport.id, encrypted = true)

        assertTrue(createResult is BackupResult.Success)
        val originalEncryptedAccounts = sourceLib.exportAccountsEncrypted()

        val freshLib = buildLib(CURRENT_PASSKEY)
        freshLib.unlock(CURRENT_PASSKEY)
        val restoreService = BackupService(freshLib, BackupTransportRegistry(listOf(transport)))

        val restoreResult = restoreService.restoreBackup(
            providerId = transport.id,
            backupId = createResult.value.id,
            backupPasskey = BACKUP_PASSKEY,
            currentPasskey = CURRENT_PASSKEY,
        )

        assertTrue(restoreResult is BackupResult.Success)
        assertEquals(sampleUris.size, restoreResult.value)
        assertEquals(sourceLib.exportAccountsPlaintext(), freshLib.exportAccountsPlaintext())
        assertEquals(sampleUris.size, freshLib.getAllAccounts().size)
        assertTrue(
            originalEncryptedAccounts.zip(freshLib.exportAccountsEncrypted())
                .any { (before, after) -> before.salt != after.salt || before.encryptedURI != after.encryptedURI }
        )
    }

    @Test
    fun testRestoreEncryptedBackupRejectsWrongBackupPasskey() = runTest {
        val sourceLib = buildLib(BACKUP_PASSKEY)
        sourceLib.unlock(BACKUP_PASSKEY)
        sampleUris.forEach { sourceLib.addAccount(it) }

        val transport = FakeTransport()
        val sourceService = BackupService(sourceLib, BackupTransportRegistry(listOf(transport)))
        val createResult = sourceService.createBackup(transport.id, encrypted = true)

        assertTrue(createResult is BackupResult.Success)

        val freshLib = buildLib(CURRENT_PASSKEY)
        freshLib.unlock(CURRENT_PASSKEY)
        val restoreService = BackupService(freshLib, BackupTransportRegistry(listOf(transport)))

        val restoreResult = restoreService.restoreBackup(
            providerId = transport.id,
            backupId = createResult.value.id,
            backupPasskey = "wrong-passkey",
            currentPasskey = CURRENT_PASSKEY,
        )

        assertTrue(restoreResult is BackupResult.Failure)
        assertEquals(
            "Incorrect backup passkey — could not decrypt the backup accounts.",
            restoreResult.message,
        )
        assertEquals(0, freshLib.getAllAccounts().size)
    }

    @Test
    fun testRestoreEncryptedBackupSkipsExistingEquivalentAccounts() = runTest {
        val sourceLib = buildLib(BACKUP_PASSKEY)
        sourceLib.unlock(BACKUP_PASSKEY)
        sampleUris.forEach { sourceLib.addAccount(it) }

        val transport = FakeTransport()
        val sourceService = BackupService(sourceLib, BackupTransportRegistry(listOf(transport)))
        val createResult = sourceService.createBackup(transport.id, encrypted = true)

        assertTrue(createResult is BackupResult.Success)

        val freshLib = buildLib(CURRENT_PASSKEY)
        freshLib.unlock(CURRENT_PASSKEY)
        freshLib.addAccount(
            "otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub&algorithm=SHA256"
        )
        val restoreService = BackupService(freshLib, BackupTransportRegistry(listOf(transport)))

        val restoreResult = restoreService.restoreBackup(
            providerId = transport.id,
            backupId = createResult.value.id,
            backupPasskey = BACKUP_PASSKEY,
            currentPasskey = CURRENT_PASSKEY,
        )

        assertTrue(restoreResult is BackupResult.Success)
        assertEquals(1, restoreResult.value)
        assertEquals(2, freshLib.getAllAccounts().size)
    }
}
