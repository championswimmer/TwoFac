package tech.arnav.twofac.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import org.koin.core.context.startKoin
import tech.arnav.twofac.cli.commands.AccountsCommand
import tech.arnav.twofac.cli.commands.DisplayCommand
import tech.arnav.twofac.cli.commands.InfoCommand
import tech.arnav.twofac.cli.commands.StorageCommand
import tech.arnav.twofac.cli.di.appModule
import tech.arnav.twofac.cli.di.storageModule
import tech.arnav.twofac.cli.runtime.CliMode
import tech.arnav.twofac.cli.runtime.CliModeResolver
import tech.arnav.twofac.cli.runtime.DefaultCliModeResolver
import tech.arnav.twofac.cli.tui.TuiApp

class MainCommand(
    private val modeResolver: CliModeResolver = DefaultCliModeResolver,
    private val runInteractiveMode: () -> Unit = { TuiApp().run() },
) : CliktCommand() {

    override val invokeWithoutSubcommand = true

    override fun run() {
        if (currentContext.invokedSubcommands.isNotEmpty()) return

        when (
            modeResolver.resolve(
                inputInteractive = terminal.terminalInfo.inputInteractive,
                outputInteractive = terminal.terminalInfo.outputInteractive,
            )
        ) {
            CliMode.INTERACTIVE -> runInteractiveMode()
            CliMode.NON_INTERACTIVE -> throw PrintHelpMessage(currentContext)
        }
    }
}

fun main(args: Array<String>) {
    startKoin {
        modules(storageModule, appModule)
    }

    MainCommand().subcommands(
        DisplayCommand(),
        InfoCommand(),
        AccountsCommand(),
        StorageCommand(),
    ).main(args)
}
