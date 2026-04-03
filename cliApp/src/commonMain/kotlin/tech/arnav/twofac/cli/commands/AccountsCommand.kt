package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

class AccountsCommand : CliktCommand(name = "accounts") {
    override fun help(context: Context): String = "Manage accounts"

    init {
        subcommands(AddCommand(), RemoveCommand())
    }

    override fun run() = Unit
}
