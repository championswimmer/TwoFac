package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.lib.TwoFacLib

class RemoveCommand : CliktCommand(name = "remove"), KoinComponent {
    override fun help(context: Context): String = "Remove an account by account ID"

    private val twoFacLib: TwoFacLib by inject()

    private val accountId by argument(help = "Account ID to remove")
    private val passkey by option("-p", "--passkey", help = "Passkey to unlock account store").prompt(
        "Enter passkey",
        hideInput = true,
    )

    override fun run() = runBlocking {
        twoFacLib.unlock(passkey)
        val deleted = twoFacLib.deleteAccount(accountId)
        if (!deleted) {
            throw UsageError("Error: Account not found for account ID '$accountId'.")
        }
        echo("Account removed successfully")
    }
}
