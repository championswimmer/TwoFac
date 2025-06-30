package tech.arnav.twofac.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import tech.arnav.twofac.cli.commands.DisplayCommand
import tech.arnav.twofac.cli.commands.InfoCommand

class MainCommand(val args: Array<String>) : CliktCommand() {
    val passkey by option("--pass", "-p", help = "Passkey for accessing encrypted data")
        .prompt("Enter passkey", hideInput = true)

    override val invokeWithoutSubcommand = true
    override fun run() {
        // If a subcommand is invoked, skip the main command logic
        if (currentContext.invokedSubcommands.isNotEmpty()) return
        // Else, run the display logic directly here
        echo("display command executed with passkey: ${passkey.take(3)}...")
    }
}

fun main(args: Array<String>) = MainCommand(args)
    .subcommands(
        DisplayCommand(),
        InfoCommand(),
    )
    .main(args)
