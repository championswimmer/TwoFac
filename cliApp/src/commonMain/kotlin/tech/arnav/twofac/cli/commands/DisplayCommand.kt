package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import tech.arnav.twofac.cli.MainCommand

class DisplayCommand : CliktCommand() {
    override fun help(context: Context): String {
        return "(default) Displays all OTPs of registered accounts."
    }

    override fun run() {
        val mainCommand = currentContext.findRoot().command as MainCommand
        val passkey = mainCommand.passkey
        echo("display command executed with passkey: ${passkey.take(3)}...")
    }
}
