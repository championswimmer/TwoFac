package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.lib.TwoFacLib

class StorageCommand : CliktCommand(name = "storage") {
    override fun help(context: Context): String = "Manage account storage"

    init {
        subcommands(StorageDeleteCommand())
    }

    override fun run() = Unit
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
        if (!yes) {
            echo("This deletes all existing accounts and cannot be undone unless you have a backup.")
            echo("A fresh storage file will be created on next run/use.")
            echo("Type DELETE to confirm:")
            val confirmation = readlnOrNull()?.trim()
            if (confirmation != "DELETE") {
                echo("Deletion cancelled.")
                return@runBlocking
            }
        }

        val deleted = twoFacLib.deleteAllAccountsFromStorage()
        if (deleted) {
            echo("✓ Deleted all accounts from storage.")
            echo("A fresh storage file will be created on next run/use.")
        } else {
            echo("✗ Failed to delete accounts from storage.", err = true)
        }
    }
}
