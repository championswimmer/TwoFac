package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.cli.storage.AppDirUtils
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.LocalBackupTransport

class BackupCommand : CliktCommand(help = "Manage local TwoFac backups."), KoinComponent {
    init {
        subcommands(BackupExportCommand(), BackupImportCommand())
    }

    override fun run() = Unit
}

private class BackupExportCommand : CliktCommand(
    name = "export",
    help = "Export accounts to a plaintext JSON backup in the app data directory."
), KoinComponent {
    private val backupService: BackupService by inject()
    private val transport: LocalBackupTransport by inject()

    private val passkey by option("-p", "--passkey", help = "Passkey to decrypt accounts")
        .prompt("Enter passkey", hideInput = true)

    private val fileName by option(
        "-o",
        "--output",
        help = "Backup file name (stored in ${AppDirUtils.getBackupDir().toString()})"
    ).prompt("Backup file name", default = AppDirUtils.DEFAULT_BACKUP_FILE_NAME)

    override fun help(context: Context): String = "Export accounts to JSON for manual backup."

    override fun run() {
        if (passkey.isBlank()) {
            echo("Passkey cannot be blank")
            return
        }
        runBlocking {
            when (val result = backupService.createPlaintextBackup(
                passkey = passkey,
                transport = transport,
                fileName = fileName,
            )) {
                is BackupResult.Success -> echo("Backup saved as $fileName in ${AppDirUtils.getBackupDir()}")
                is BackupResult.Failure -> echo("Backup failed: ${result.error.message}")
            }
        }
    }
}

private class BackupImportCommand : CliktCommand(
    name = "import",
    help = "Import accounts from a plaintext JSON backup stored locally."
), KoinComponent {
    private val backupService: BackupService by inject()
    private val transport: LocalBackupTransport by inject()

    private val passkey by option("-p", "--passkey", help = "Passkey to encrypt imported accounts")
        .prompt("Enter passkey", hideInput = true)

    private val fileName by option(
        "-i",
        "--input",
        help = "Backup file name (read from ${AppDirUtils.getBackupDir()})"
    ).prompt("Backup file name", default = AppDirUtils.DEFAULT_BACKUP_FILE_NAME)

    override fun help(context: Context): String = "Import accounts from a local plaintext backup."

    override fun run() {
        if (passkey.isBlank()) {
            echo("Passkey cannot be blank")
            return
        }
        runBlocking {
            when (val result = backupService.restorePlaintextBackup(
                passkey = passkey,
                transport = transport,
                backupId = fileName,
            )) {
                is BackupResult.Success -> echo("Imported ${result.value.imported} accounts (failed: ${result.value.failed})")
                is BackupResult.Failure -> echo("Import failed: ${result.error.message}")
            }
        }
    }
}
