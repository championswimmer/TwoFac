package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.testing.test
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tech.arnav.twofac.cli.backup.LocalFileBackupTransport
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.BackupTransportRegistry
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.storage.Storage
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupCommandTest {
    private companion object {
        const val BACKUP_PASSKEY = "backup-passkey"
        const val CURRENT_PASSKEY = "current-passkey"
    }

    private lateinit var koinApp: KoinApplication
    private lateinit var twoFacLib: TwoFacLib

    @BeforeTest
    fun setup() {
        stopKoin()
        koinApp = startKoin {
            modules(
                module {
                    single<Storage> { MemoryStorage() }
                    single<TwoFacLib> { TwoFacLib.initialise(storage = get(), passKey = CURRENT_PASSKEY) }
                }
            )
        }
        twoFacLib = koinApp.koin.get()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testExportEncryptedWritesEncryptedPayload() = runBlocking {
        twoFacLib.addAccount(
            "otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
        )
        val backupDir = uniqueBackupDir()

        val result = ExportCommand().test(
            "--output-dir=$backupDir",
            "--passkey=$CURRENT_PASSKEY",
            "--encrypted",
        )

        assertEquals(0, result.statusCode)
        assertContains(result.output, "encrypted backup")

        val service = buildBackupService(twoFacLib, backupDir)
        val backups = (service.listBackups("local") as BackupResult.Success).value
        assertEquals(1, backups.size)
        val payload = (service.inspectBackup("local", backups.single().id) as BackupResult.Success).value
        assertTrue(payload.encrypted)
        assertEquals(1, payload.encryptedAccounts.size)
        assertTrue(payload.accounts.isEmpty())
    }

    @Test
    fun testImportEncryptedPromptsForBackupAndCurrentPasskeys() = runBlocking {
        val backupDir = uniqueBackupDir()
        val backupId = createEncryptedBackupFile(backupDir)

        val result = ImportCommand().test(
            "--input-dir=$backupDir",
            "--file=$backupId",
            stdin = "$BACKUP_PASSKEY\n$CURRENT_PASSKEY\n",
        )

        assertEquals(0, result.statusCode)
        assertContains(
            result.output,
            "This backup contains encrypted accounts.\nEnter the passkey used when this backup was created (to decrypt)"
        )
        assertContains(
            result.output,
            "Enter your current app passkey (to save restored accounts to storage)"
        )
        assertContains(result.output, "✓ Imported 2 account(s) from $backupId")
        assertEquals(2, twoFacLib.getAllAccounts().size)
    }

    @Test
    fun testImportEncryptedWithWrongBackupPasskeyFailsNonZero() = runBlocking {
        val backupDir = uniqueBackupDir()
        val backupId = createEncryptedBackupFile(backupDir)

        val result = ImportCommand().test(
            "--input-dir=$backupDir",
            "--file=$backupId",
            stdin = "wrong-passkey\n",
        )

        assertEquals(1, result.statusCode)
        assertContains(
            result.output,
            "This backup contains encrypted accounts.\nEnter the passkey used when this backup was created (to decrypt)"
        )
        assertContains(
            result.output,
            "Error: Incorrect passkey — could not decrypt the backup accounts."
        )
        assertEquals(0, twoFacLib.storage.getAccountList().size)
    }

    private fun buildBackupService(lib: TwoFacLib, directory: String): BackupService {
        return BackupService(
            twoFacLib = lib,
            transportRegistry = BackupTransportRegistry(
                listOf(LocalFileBackupTransport(Path(directory)))
            ),
        )
    }

    private suspend fun createEncryptedBackupFile(directory: String): String {
        val sourceLib = TwoFacLib.initialise(MemoryStorage(), BACKUP_PASSKEY)
        sourceLib.unlock(BACKUP_PASSKEY)
        sourceLib.addAccount(
            "otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
        )
        sourceLib.addAccount(
            "otpauth://totp/Google:bob@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=Google"
        )
        val result = buildBackupService(sourceLib, directory).createBackup("local", encrypted = true)
        assertTrue(result is BackupResult.Success)
        return result.value.id
    }

    private fun uniqueBackupDir(): String {
        return "build/test-backups/${Random.nextInt(0, Int.MAX_VALUE)}"
    }
}
