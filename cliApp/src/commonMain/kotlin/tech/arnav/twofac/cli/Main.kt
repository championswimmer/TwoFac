package tech.arnav.twofac.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import tech.arnav.twofac.cli.commands.DisplayCommand
import tech.arnav.twofac.cli.commands.InfoCommand

class MainCommand(val args: Array<String>) : CliktCommand() {

    override val invokeWithoutSubcommand = true
    override fun run() {
        // If a subcommand is invoked, skip the main command logic
        if (currentContext.invokedSubcommands.isNotEmpty()) return
        // Else, run the display logic directly here
        DisplayCommand().main(args)
    }
}

fun main(args: Array<String>) = MainCommand(args).subcommands(
    DisplayCommand(),
    InfoCommand(),
).main(args)
