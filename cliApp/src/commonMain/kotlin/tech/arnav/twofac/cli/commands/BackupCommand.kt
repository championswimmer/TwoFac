package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.cli.backup.LocalFileBackupTransport
import tech.arnav.twofac.cli.storage.AppDirUtils
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.BackupTransportRegistry

private const val LOCAL_PROVIDER_ID = "local"

private fun localBackupService(twoFacLib: TwoFacLib, dir: Path): BackupService {
    return BackupService(
        twoFacLib = twoFacLib,
        transportRegistry = BackupTransportRegistry(
            transports = listOf(LocalFileBackupTransport(dir)),
        ),
    )
}

class BackupCommand : CliktCommand(name = "backup") {
    override fun help(context: Context) = "Backup and restore accounts via local JSON files"

    init {
        subcommands(ExportCommand(), ImportCommand())
    }

    override fun run() = Unit
}

class ExportCommand : CliktCommand(name = "export"), KoinComponent {
    override fun help(context: Context) = "Export all accounts to a local backup file"

    private val twoFacLib: TwoFacLib by inject()

    private val outputDir by option(
        "-o", "--output-dir",
        help = "Directory where the backup file will be written (defaults to app backup dir)"
    )
    private val passkey by option("-p", "--passkey", help = "Passkey to decrypt accounts").prompt(
        "Enter passkey",
        hideInput = true
    )

    override fun run() = runBlocking {
        twoFacLib.unlock(passkey)

        val dir = outputDir?.let { Path(it) } ?: AppDirUtils.getBackupDirPath(forceCreate = true)
        val service = localBackupService(twoFacLib, dir)

        when (val result = service.createBackup(LOCAL_PROVIDER_ID)) {
            is BackupResult.Success -> {
                val descriptor = result.value
                echo("✓ Exported ${twoFacLib.getAllAccounts().size} account(s) to ${Path(dir, descriptor.id)}")
            }
            is BackupResult.Failure -> {
                echo("✗ Export failed: ${result.message}", err = true)
            }
        }
    }
}

class ImportCommand : CliktCommand(name = "import"), KoinComponent {
    override fun help(context: Context) = "Restore accounts from a local backup file"

    private val twoFacLib: TwoFacLib by inject()

    private val inputDir by option(
        "-i", "--input-dir",
        help = "Directory to search for backup files (defaults to app backup dir)"
    )
    private val backupFile by option(
        "-f", "--file",
        help = "Specific backup file name to restore (defaults to most recent backup)"
    )
    private val passkey by option("-p", "--passkey", help = "Passkey to encrypt imported accounts").prompt(
        "Enter passkey",
        hideInput = true
    )

    override fun run() = runBlocking {
        twoFacLib.unlock(passkey)

        val dir = inputDir?.let { Path(it) } ?: AppDirUtils.getBackupDirPath()
        val service = localBackupService(twoFacLib, dir)

        // Determine which backup to restore
        val resolvedBackupId = backupFile ?: run {
            val listResult = service.listBackups(LOCAL_PROVIDER_ID)
            if (listResult is BackupResult.Failure) {
                echo("✗ Failed to list backups: ${listResult.message}", err = true)
                return@runBlocking
            }
            val backups = (listResult as BackupResult.Success).value
            if (backups.isEmpty()) {
                echo("✗ No backup files found in $dir", err = true)
                return@runBlocking
            }
            backups.maxBy { it.createdAt }.id
        }

        when (val result = service.restoreBackup(LOCAL_PROVIDER_ID, resolvedBackupId)) {
            is BackupResult.Success -> echo("✓ Imported ${result.value} account(s) from $resolvedBackupId")
            is BackupResult.Failure -> echo("✗ Import failed: ${result.message}", err = true)
        }
    }
}
