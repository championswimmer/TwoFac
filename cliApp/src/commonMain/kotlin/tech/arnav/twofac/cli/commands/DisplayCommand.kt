package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt

class DisplayCommand : CliktCommand() {
    override fun help(context: Context): String {
        return "(default) Displays all OTPs of registered accounts."
    }

    val passkey by option("-p", "--passkey", help = "Passkey to display")
        .prompt("Enter passkey", hideInput = true)


    override fun run() {
        echo("display command executed with passkey: ${passkey.take(3)}...")
    }
}
