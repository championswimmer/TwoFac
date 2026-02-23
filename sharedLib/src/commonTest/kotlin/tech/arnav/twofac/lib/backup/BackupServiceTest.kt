package tech.arnav.twofac.lib.backup

import dev.whyoleg.cryptography.CryptographyProvider
import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.crypto.DefaultCryptoTools
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.storage.StorageUtils.toStoredAccount
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.lib.uri.OtpAuthURI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupServiceTest {

    private val passkey = "test-passkey"
    private val cryptoTools = DefaultCryptoTools(CryptographyProvider.Default)

    @Test
    fun `create and restore plaintext backup`() = runTest {
        val storage = InMemoryStorage()
        val otp = OtpAuthURI.parse(
            "otpauth://totp/Test:demo?secret=JBSWY3DPEHPK3PXP&issuer=Test"
        )
        val storedAccount = otp.toStoredAccount(cryptoTools.createSigningKey(passkey))
        storage.saveAccount(storedAccount)

        val transportStore = InMemoryBackupStore()
        val transport = LocalBackupTransport(transportStore)
        val backupService = BackupService(storage)

        val exportResult = backupService.createPlaintextBackup(
            passkey = passkey,
            transport = transport,
            fileName = "backup.json",
            appVersion = "1.0.0",
        )
        assertTrue(exportResult is BackupResult.Success)

        val restoreStorage = InMemoryStorage()
        val restoreService = BackupService(restoreStorage)
        val restoreResult = restoreService.restorePlaintextBackup(
            passkey = passkey,
            transport = transport,
            backupId = exportResult.value.id,
        )
        assertTrue(restoreResult is BackupResult.Success)
        assertEquals(1, restoreStorage.getAccountList().size)
    }

    private class InMemoryStorage : Storage {
        private val accounts = mutableListOf<StoredAccount>()

        override suspend fun getAccountList(): List<StoredAccount> = accounts.toList()

        override suspend fun getAccount(accountLabel: String): StoredAccount? =
            accounts.find { it.accountLabel == accountLabel }

        override suspend fun getAccount(accountID: kotlin.uuid.Uuid): StoredAccount? =
            accounts.find { it.accountID == accountID }

        override suspend fun saveAccount(account: StoredAccount): Boolean {
            val existing = accounts.indexOfFirst { it.accountID == account.accountID }
            if (existing >= 0) {
                accounts[existing] = account
            } else {
                accounts.add(account)
            }
            return true
        }
    }

    private class InMemoryBackupStore : LocalBackupStore {
        private val files = linkedMapOf<String, ByteArray>()

        override suspend fun list(): List<LocalBackupFile> = files.map { (name, bytes) ->
            LocalBackupFile(
                id = name,
                name = name,
                updatedAtMillis = 0L,
                sizeBytes = bytes.size.toLong(),
            )
        }

        override suspend fun write(name: String, bytes: ByteArray): LocalBackupFile {
            files[name] = bytes
            return LocalBackupFile(
                id = name,
                name = name,
                updatedAtMillis = 0L,
                sizeBytes = bytes.size.toLong(),
            )
        }

        override suspend fun read(id: String): ByteArray? = files[id]

        override suspend fun delete(id: String): Boolean = files.remove(id) != null
    }
}
