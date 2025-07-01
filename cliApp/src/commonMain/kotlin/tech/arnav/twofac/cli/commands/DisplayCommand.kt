package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.Table
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.ProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.cli.viewmodels.AccountsViewModel
import kotlin.system.exitProcess
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class DisplayCommand : CliktCommand(), KoinComponent {
    override fun help(context: Context): String {
        return "(default) Displays all OTPs of registered accounts."
    }

    val accountsViewModel: AccountsViewModel by inject()

    val passkey by option("-p", "--passkey", help = "Passkey to display").prompt("Enter passkey", hideInput = true)


    @OptIn(ExperimentalTime::class)
    override fun run() = runBlocking {
        // Unlock the library with the provided passkey
        accountsViewModel.unlock(passkey)

        // Fetch and display all account OTPs
        val accountOTPs = accountsViewModel.showAllAccountOTPs()
        fun getTable() = table {
            borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
            header {
                style = TextStyles.dim + TextColors.brightBlue
                row("Account", "OTP", "Validity")
            }
            body {
                column(1) {
                    style = TextStyles.bold + TextColors.brightCyan
                    cellBorders = Borders.ALL
                    padding = Padding(0, 1, 0, 1) // Add padding to the cell
                }
                accountOTPs.forEach { (account, otp) ->
                    val currentTimeSec = Clock.System.now().epochSeconds
                    val elapsedTimeSec = currentTimeSec - (account.nextCodeAt - 30)
                    val leftTimeSec = 30 - elapsedTimeSec
                    row {
                        cell(account.accountLabel)
                        cell(otp.split("").joinToString(" "))
                        cell(
                            ProgressBar(
                                total = 30L,
                                completed = elapsedTimeSec,
                                width = 15,
                                separatorChar = "$leftTimeSec"
                            )
                        )
                    }
                }
            }
        }

        val animation = terminal.animation<Table> {
            getTable()
        }

        repeat(30) {
            animation.update(getTable())
            delay(1.seconds.inWholeMilliseconds)
        }
        exitProcess(0)
    }
}
