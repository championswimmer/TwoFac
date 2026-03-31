package tech.arnav.twofac.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import org.koin.core.context.startKoin
import tech.arnav.twofac.cli.commands.AddCommand
import tech.arnav.twofac.cli.commands.BackupCommand
import tech.arnav.twofac.cli.commands.DisplayCommand
import tech.arnav.twofac.cli.commands.InfoCommand
import tech.arnav.twofac.cli.commands.StorageCommand
import tech.arnav.twofac.cli.di.appModule
import tech.arnav.twofac.cli.di.storageModule

class MainCommand : CliktCommand() {


    override val invokeWithoutSubcommand = true
    override fun run() {
        // If a subcommand is invoked, skip the main command logic
        if (currentContext.invokedSubcommands.isNotEmpty()) return
        // Else, run the display logic (passkey will be prompted)
        DisplayCommand().main(emptyArray())
    }
}

fun main(args: Array<String>) {
    startKoin {
        modules(storageModule, appModule)
    }

    MainCommand().subcommands(
        DisplayCommand(),
        InfoCommand(),
        AddCommand(),
        BackupCommand(),
        StorageCommand(),
    ).main(args)
}
