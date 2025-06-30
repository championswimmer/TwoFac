package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class DisplayCommand : CliktCommand() {
    override fun help(context: Context): String {
        return "(default) Displays all OTPs of registered accounts."
    }

    override fun run() {
        echo("display command executed")
    }
}