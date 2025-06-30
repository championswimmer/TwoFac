package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import tech.arnav.twofac.cli.getPlatform
import tech.arnav.twofac.lib.libPlatform

class InfoCommand : CliktCommand("info") {
    override fun help(context: Context): String {
        return "Shows app information, platform and data directory."
    }

    override fun run() {

        terminal.println(table {
            borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
            header {
                style = TextStyles.bold + TextColors.yellow
                cellBorders = Borders.NONE
                row("TwoFac CLI Info")
            }
            body {
                column(0) {
                    style = TextStyles.bold + TextColors.green
                }
                row("Platform", getPlatform().name)
                row("Library", libPlatform()) // TODO: add version via buildkonfig
                row("Data Directory", getPlatform().appDirs.getUserDataDir())
            }
        })
    }
}