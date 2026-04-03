package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.cli.storage.AppDirUtils
import tech.arnav.twofac.cli.storage.CliConfig
import tech.arnav.twofac.cli.storage.CliConfigStore
import tech.arnav.twofac.cli.storage.CliStorageBackend
import tech.arnav.twofac.lib.TwoFacLib

class StorageCommand : CliktCommand(name = "storage") {
    override fun help(context: Context): String = "Manage account storage"

    override val invokeWithoutSubcommand: Boolean = true

    private val selectedBackend by option(
        "--use-backend",
        help = "Select storage backend for CLI mode",
    ).choice("standalone", "common")

    init {
        subcommands(
            StorageCleanCommand(),
            StorageDeleteCommand(),
            StorageReinitializeCommand(),
            BackupCommand(),
        )
    }

    override fun run() {
        selectedBackend?.let { backendValue ->
            val backend = CliStorageBackend.fromCliValue(backendValue)
                ?: throw UsageError("Unsupported backend '$backendValue'")
            if (!CliConfigStore.write(CliConfig(storageBackend = backend))) {
                throw UsageError("Failed to persist backend selection")
            }
            echo("✓ Storage backend set to ${backend.cliValue}")
        }

        if (currentContext.invokedSubcommands.isEmpty() && selectedBackend == null) {
            throw PrintHelpMessage(currentContext)
        }
    }
}

class StorageDeleteCommand : CliktCommand(name = "delete"), KoinComponent {
    override fun help(context: Context): String = "Delete all stored accounts"

    private val twoFacLib: TwoFacLib by inject()
    private val yes by option(
        "-y",
        "--yes",
        help = "Skip interactive confirmation and delete immediately"
    ).flag(default = false)

    override fun run() = runBlocking {
        if (!confirmDestructiveOperation(yes)) return@runBlocking

        val deleted = twoFacLib.deleteAllAccountsFromStorage()
        if (deleted) {
            echo("✓ Deleted all accounts from storage.")
            echo("A fresh storage file will be created on next run/use.")
        } else {
            echo("✗ Failed to delete accounts from storage.", err = true)
        }
    }
}

class StorageCleanCommand : CliktCommand(name = "clean"), KoinComponent {
    override fun help(context: Context): String = "Delete all accounts and leave an empty storage file"

    private val twoFacLib: TwoFacLib by inject()
    private val yes by option(
        "-y",
        "--yes",
        help = "Skip interactive confirmation and clean immediately"
    ).flag(default = false)

    override fun run() = runBlocking {
        if (!confirmDestructiveOperation(yes)) return@runBlocking

        val deleted = twoFacLib.deleteAllAccountsFromStorage()
        if (!deleted) {
            echo("✗ Failed to clean storage.", err = true)
            return@runBlocking
        }

        AppDirUtils.initializeEmptyStorageFile()
        echo("✓ Storage cleaned. Empty storage file re-created.")
    }
}

class StorageReinitializeCommand : CliktCommand(name = "reinitialize"), KoinComponent {
    override fun help(context: Context): String = "Recreate storage from scratch"

    private val twoFacLib: TwoFacLib by inject()
    private val yes by option(
        "-y",
        "--yes",
        help = "Skip interactive confirmation and reinitialize immediately"
    ).flag(default = false)

    override fun run() = runBlocking {
        if (!confirmDestructiveOperation(yes)) return@runBlocking

        val deleted = twoFacLib.deleteAllAccountsFromStorage()
        if (!deleted) {
            echo("✗ Failed to reset storage.", err = true)
            return@runBlocking
        }

        AppDirUtils.initializeEmptyStorageFile()
        echo("✓ Storage reinitialized successfully.")
    }
}

private fun CliktCommand.confirmDestructiveOperation(skipConfirmation: Boolean): Boolean {
    if (skipConfirmation) return true

    echo("This operation modifies all existing accounts and cannot be undone unless you have a backup.")
    echo("Type DELETE to confirm:")
    val confirmation = readlnOrNull()?.trim()
    if (confirmation != "DELETE") {
        echo("Operation cancelled.")
        return false
    }

    return true
}
