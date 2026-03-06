package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.ProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.cli.viewmodels.AccountsViewModel
import tech.arnav.twofac.cli.viewmodels.DisplayAccountsStatic
import tech.arnav.twofac.cli.theme.CliTheme
import tech.arnav.twofac.cli.theme.CliThemeStyles
import tech.arnav.twofac.lib.theme.timerStateByRemainingProgress
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

class DisplayCommand : CliktCommand(), KoinComponent {
    override fun help(context: Context): String {
        return "(default) Displays all OTPs of registered accounts."
    }

    val accountsViewModel: AccountsViewModel by inject()

    val passkey by option("-p", "--passkey", help = "Passkey to display").prompt("Enter passkey", hideInput = true)

    override fun run() {
        val styles = CliTheme.styles(terminal)
        if (passkey.isBlank()) {
            echo(styles.danger("Passkey cannot be blank"))
            return
        }

        val isInteractive = terminal.terminalInfo.inputInteractive && terminal.terminalInfo.outputInteractive

        runBlocking {
            accountsViewModel.unlock(passkey)
            val displayAccounts = accountsViewModel.showAllAccountOTPs()

            if (isInteractive) {
                val animation = terminal.animation<DisplayAccountsStatic> { displayAccounts ->
                    createTable(displayAccounts, styles)
                }
                echo("\n")
                repeat(2.minutes.inWholeSeconds.toInt()) {
                    animation.update(accountsViewModel.showAllAccountOTPs())
                    delay(1.seconds.inWholeMilliseconds)
                }
                echo(styles.footer("Exiting display command after 2 minutes of inactivity."))
            } else {
                echo(createTable(displayAccounts, styles))
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun createTable(
        displayAccounts: DisplayAccountsStatic,
        styles: CliThemeStyles
    ) = table {
        borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
        header {
            style = styles.header
            row("Account", "OTP", "Validity")
        }
        body {
            column(0) {
                style = styles.key
            }
            column(1) {
                style = styles.otp
                cellBorders = Borders.ALL
                padding = Padding(0, 1, 0, 1) // Add padding to the cell
            }
            displayAccounts.forEach { (account, otp) ->
                val currentTimeSec = Clock.System.now().epochSeconds
                val elapsedTimeSec = currentTimeSec - (account.nextCodeAt - 30)
                val leftTimeSec = 30 - elapsedTimeSec
                val remainingProgress = (leftTimeSec.toFloat() / 30f).coerceIn(0f, 1f)
                val timerStyle = styles.timer(timerStateByRemainingProgress(remainingProgress))
                row {
                    cell(account.accountLabel)
                    cell(otp.split("").joinToString(" "))
                    cell(
                        ProgressBar(
                            total = 30L,
                            completed = elapsedTimeSec.coerceIn(0L..30L),
                            width = 15,
                            separatorChar = "$leftTimeSec",
                            pendingStyle = styles.timerTrack,
                            separatorStyle = timerStyle,
                            completeStyle = timerStyle,
                            finishedStyle = timerStyle,
                        )
                    )
                }
            }
        }
        footer {
            cellBorders = Borders.NONE
            style = styles.footer
            row("Press Ctrl-C key to exit")
        }
    }

}
