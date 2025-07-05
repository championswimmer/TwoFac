package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.cli.viewmodels.AccountsViewModel

class AddCommand : CliktCommand(name = "add"), KoinComponent {
    override fun help(context: Context): String {
        return "Adds new accounts to the database. Provide an otpauth://... URI"
    }

    private val accountsViewModel: AccountsViewModel by inject()

    private val uri by argument(help = "otpauth:// URI")
    private val passkey by option("-p", "--passkey", help = "Passkey to add account").prompt(
        "Enter passkey",
        hideInput = true
    )

    override fun run() = runBlocking {
        accountsViewModel.unlock(passkey)
        accountsViewModel.addAccount(uri)
        echo("Account added successfully")
    }
}
