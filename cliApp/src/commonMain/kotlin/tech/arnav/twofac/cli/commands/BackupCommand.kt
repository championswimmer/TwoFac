package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.lib.backup.BackupTransportRegistry

class BackupCommand : CliktCommand(name = "backup") {
    override fun help(context: Context) = "Backup and restore accounts"

    init {
        subcommands(ExportCommand(), ImportCommand())
    }

    override fun run() = Unit
}

class ExportCommand : CliktCommand(name = "export"), KoinComponent {
    override fun help(context: Context) = "Export all accounts to a backup file"

    private val twoFacLib: TwoFacLib by inject()
    private val registry: BackupTransportRegistry by inject()

    private val provider by option(
        "--provider",
        help = "Backup provider to use (defaults to local)"
    ).default("local")
    private val passkey by option("-p", "--passkey", help = "Passkey to decrypt accounts").prompt(
        "Enter passkey",
        hideInput = true
    )

    override fun run() = runBlocking {
        twoFacLib.unlock(passkey)

        val transport = registry.transport(provider)
        if (transport == null) {
            val available = registry.providers().joinToString(", ") { it.id }
            echo("✗ Unknown backup provider '$provider'. Available: $available", err = true)
            return@runBlocking
        }

        val service = BackupService(twoFacLib)
        when (val result = service.createBackup(transport)) {
            is BackupResult.Success -> {
                val descriptor = result.value
                echo("✓ Exported ${twoFacLib.getAllAccounts().size} account(s) via '$provider': ${descriptor.id}")
            }
            is BackupResult.Failure -> {
                echo("✗ Export failed: ${result.message}", err = true)
            }
        }
    }
}

class ImportCommand : CliktCommand(name = "import"), KoinComponent {
    override fun help(context: Context) = "Restore accounts from a backup file"

    private val twoFacLib: TwoFacLib by inject()
    private val registry: BackupTransportRegistry by inject()

    private val provider by option(
        "--provider",
        help = "Backup provider to use (defaults to local)"
    ).default("local")
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

        val transport = registry.transport(provider)
        if (transport == null) {
            val available = registry.providers().joinToString(", ") { it.id }
            echo("✗ Unknown backup provider '$provider'. Available: $available", err = true)
            return@runBlocking
        }

        // Determine which backup to restore
        val resolvedBackupId = backupFile ?: run {
            val listResult = transport.listBackups()
            if (listResult is BackupResult.Failure) {
                echo("✗ Failed to list backups: ${listResult.message}", err = true)
                return@runBlocking
            }
            val backups = (listResult as BackupResult.Success).value
            if (backups.isEmpty()) {
                echo("✗ No backup files found for provider '$provider'", err = true)
                return@runBlocking
            }
            backups.maxBy { it.createdAt }.id
        }

        val service = BackupService(twoFacLib)
        when (val result = service.restoreBackup(transport, resolvedBackupId)) {
            is BackupResult.Success -> echo("✓ Imported ${result.value} account(s) from $resolvedBackupId")
            is BackupResult.Failure -> echo("✗ Import failed: ${result.message}", err = true)
        }
    }
}
