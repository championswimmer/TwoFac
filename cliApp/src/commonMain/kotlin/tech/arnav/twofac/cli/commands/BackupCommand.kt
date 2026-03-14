package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.mordant.terminal.prompt
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
    private val encrypted by option(
        "--encrypted",
        help = "Keep account data encrypted in the backup file"
    ).flag(default = false)
    private val passkey by option("-p", "--passkey", help = "Passkey to decrypt accounts").prompt(
        "Enter your current app passkey",
        hideInput = true
    )

    override fun run() = runBlocking {
        twoFacLib.unlock(passkey)

        val dir = outputDir?.let { Path(it) } ?: AppDirUtils.getBackupDirPath(forceCreate = true)
        val service = localBackupService(twoFacLib, dir)

        when (val result = service.createBackup(LOCAL_PROVIDER_ID, encrypted = encrypted)) {
            is BackupResult.Success -> {
                val descriptor = result.value
                echo(
                    "✓ Exported ${twoFacLib.getAllAccounts().size} account(s) " +
                        "as ${if (encrypted) "an encrypted" else "a plaintext"} backup to ${Path(dir, descriptor.id)}"
                )
            }
            is BackupResult.Failure -> {
                throw UsageError("Error: Export failed: ${result.message}")
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
    private val currentPasskey by option(
        "-p", "--passkey",
        help = "Current app passkey to save restored accounts to storage"
    )
    private val backupPasskey by option(
        "--backup-passkey",
        help = "Passkey that was used when the encrypted backup was created"
    )

    override fun run() = runBlocking {
        val dir = inputDir?.let { Path(it) } ?: AppDirUtils.getBackupDirPath()
        val service = localBackupService(twoFacLib, dir)

        // Determine which backup to restore
        val resolvedBackupId = backupFile ?: run {
            val listResult = service.listBackups(LOCAL_PROVIDER_ID)
            if (listResult is BackupResult.Failure) {
                throw UsageError("Error: Failed to list backups: ${listResult.message}")
            }
            val backups = (listResult as BackupResult.Success).value
            if (backups.isEmpty()) {
                throw UsageError("Error: No backup files found in $dir")
            }
            backups.maxBy { it.createdAt }.id
        }

        val inspectionResult = service.inspectBackup(LOCAL_PROVIDER_ID, resolvedBackupId)
        val payload = when (inspectionResult) {
            is BackupResult.Success -> inspectionResult.value
            is BackupResult.Failure -> throw UsageError("Error: Import failed: ${inspectionResult.message}")
        }

        val resolvedBackupPasskey: String?
        val resolvedCurrentPasskey: String
        if (payload.encrypted) {
            resolvedBackupPasskey = backupPasskey ?: terminal.prompt(
                "This backup contains encrypted accounts.\nEnter the passkey used when this backup was created (to decrypt)",
                hideInput = true,
            )
            if (resolvedBackupPasskey.isNullOrBlank()) {
                throw UsageError("Error: Backup passkey is required.")
            }
            val isBackupPasskeyValid = payload.encryptedAccounts.firstOrNull()?.let { account ->
                runCatching {
                    twoFacLib.decryptEncryptedBackupAccount(account, resolvedBackupPasskey)
                }.isSuccess
            } ?: true
            if (!isBackupPasskeyValid) {
                throw UsageError("Error: Incorrect passkey — could not decrypt the backup accounts.")
            }
            resolvedCurrentPasskey = currentPasskey ?: terminal.prompt(
                "Enter your current app passkey (to save restored accounts to storage)",
                hideInput = true,
            ) ?: ""
            if (resolvedCurrentPasskey.isBlank()) {
                throw UsageError("Error: Current app passkey is required.")
            }
            twoFacLib.unlock(resolvedCurrentPasskey)
        } else {
            resolvedBackupPasskey = null
            resolvedCurrentPasskey = currentPasskey ?: terminal.prompt(
                "Enter your current app passkey (to save restored accounts to storage)",
                hideInput = true,
            ) ?: ""
            if (resolvedCurrentPasskey.isBlank()) {
                throw UsageError("Error: Current app passkey is required.")
            }
            twoFacLib.unlock(resolvedCurrentPasskey)
        }

        when (val result = service.restoreBackup(
            LOCAL_PROVIDER_ID,
            resolvedBackupId,
            backupPasskey = resolvedBackupPasskey,
            currentPasskey = resolvedCurrentPasskey,
        )) {
            is BackupResult.Success -> echo("✓ Imported ${result.value} account(s) from $resolvedBackupId")
            is BackupResult.Failure -> throw UsageError("Error: Import failed: ${result.message}")
        }
    }
}
