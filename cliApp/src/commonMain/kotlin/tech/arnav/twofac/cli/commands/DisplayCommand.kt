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
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.warning
import com.github.ajalt.mordant.widgets.Padding
import com.github.ajalt.mordant.widgets.ProgressBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.cli.viewmodels.AccountsViewModel
import tech.arnav.twofac.cli.viewmodels.DisplayAccountsStatic
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
        if (passkey.isBlank()) {
            echo("Passkey cannot be blank")
            return
        }
        // Unlock the library with the provided passkey
        accountsViewModel.unlock(passkey)

        // Fetch and display all account OTPs
        val animation = terminal.animation<DisplayAccountsStatic> { displayAccounts ->
            createTable(displayAccounts)
        }
        echo("\n")
        runBlocking {
            repeat(2.minutes.inWholeSeconds.toInt()) {
                animation.update(accountsViewModel.showAllAccountOTPs())
                delay(1.seconds.inWholeMilliseconds)
            }
            terminal.warning("Exiting display command after 2 minutes of inactivity.")
            return@runBlocking
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun createTable(displayAccounts: DisplayAccountsStatic) = table {
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
            displayAccounts.forEach { (account, otp) ->
                val currentTimeSec = Clock.System.now().epochSeconds
                val elapsedTimeSec = currentTimeSec - (account.nextCodeAt - 30)
                val leftTimeSec = 30 - elapsedTimeSec
                row {
                    cell(account.accountLabel)
                    cell(otp.split("").joinToString(" "))
                    cell(
                        ProgressBar(
                            total = 30L,
                            completed = elapsedTimeSec.coerceIn(0L..30L),
                            width = 15,
                            separatorChar = "$leftTimeSec"
                        )
                    )
                }
            }
        }
        footer {
            cellBorders = Borders.NONE
            style = TextStyles.dim + TextColors.brightGreen
            row("Press Ctrl-C key to exit")
        }
    }

}
