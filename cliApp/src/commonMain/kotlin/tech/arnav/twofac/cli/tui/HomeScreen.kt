package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.ColumnWidth.Companion.Fixed
import com.github.ajalt.mordant.table.table
import tech.arnav.twofac.cli.theme.CliThemeStyles
import com.github.ajalt.mordant.rendering.TextStyle

class HomeScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.HOME

    companion object {
        private const val PROGRESS_BAR_WIDTH = 15
        private const val TOTP_PERIOD = 30L

        private fun renderProgressBar(
            account: TuiOtpEntry,
            nowEpochSeconds: Long,
            styles: CliThemeStyles,
        ): String {
            val remaining = (account.nextCodeAt - nowEpochSeconds).coerceAtLeast(0)
            val elapsed = (TOTP_PERIOD - remaining).coerceIn(0L, TOTP_PERIOD)

            val sb = StringBuilder()
            sb.append("[")
            for (i in 0 until PROGRESS_BAR_WIDTH) {
                val blockStart = i * 2L
                val blockEnd = blockStart + 2L
                when {
                    blockEnd <= elapsed -> sb.append("█")  // completed, full
                    blockStart > elapsed -> sb.append("░")  // future, lightest
                    else -> {
                        val secInBlock = elapsed - blockStart  // 0 or 1
                        if (secInBlock == 0L) {
                            // First second of this block: lightest shade
                            sb.append("░")
                        } else {
                            // Second second of this block: medium shade
                            sb.append("▒")
                        }
                    }
                }
            }
            sb.append("]")

            val timer = "${remaining}s"

            return "${sb.toString()} $timer"
        }

        private fun progressStyle(
            remaining: Long,
            styles: CliThemeStyles,
        ): TextStyle = when {
            remaining <= 5 -> styles.timerCritical
            remaining <= 10 -> styles.timerWarning
            else -> styles.timerHealthy
        }
    }

    override fun render(state: TuiAppState, styles: CliThemeStyles) = table {
        borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
        column(0) {
            width = Fixed(1)
            cellBorders = Borders.NONE
        }

        column(4) {
            width = Fixed(24)
            cellBorders = Borders.ALL
        }

        header {
            row(
                styles.header(" "),
                styles.header("Account"),
                styles.header("Issuer"),
                styles.header("OTP"),
                styles.header("Timer"),
            )
        }

        body {
            val filteredAccounts = state.home.filteredAccounts()
            if (filteredAccounts.isEmpty()) {
                row("", styles.label("No matching accounts"), "", "", "")
            } else {
                filteredAccounts.forEachIndexed { index, account ->
                    val isSelected = index == state.home.selectedIndex
                    val selectedMarker = if (isSelected) styles.key(">") else " "
                    val otp = account.otp.currentOTP.chunked(3).joinToString(" ")
                    val otpFormatted = styles.otp(otp)
                    val accountLabel = if (isSelected) styles.key(account.accountLabel) else account.accountLabel
                    val issuerLabel = if (isSelected) styles.key(account.issuer ?: "-") else (account.issuer ?: "-")

                    val remaining = (account.nextCodeAt - state.nowEpochSeconds).coerceAtLeast(0)
                    val progressBar = renderProgressBar(account, state.nowEpochSeconds, styles)
                    val progressCol = progressStyle(remaining, styles)(progressBar)
                    row(selectedMarker, accountLabel, issuerLabel, otpFormatted, progressCol)
                }
            }
        }

        val filterPrefix = if (state.home.isFilterInputActive) styles.key("(filtering)") else ""
        val filterLine = "filter: '${styles.key(state.home.filterQuery)}' $filterPrefix"
        val shortcutsLine = styles.footer("↑/↓ move • Enter open • / or f filter • n new • s settings • q quit")
        val footerText = buildString {
            append(filterLine)
            state.message?.let {
                append("\n")
                append(it)
            }
            append("\n")
            append(shortcutsLine)
        }
        captionBottom(footerText, align = TextAlign.LEFT)
    }

    override fun onKey(event: KeyboardEvent, state: TuiAppState): TuiAction {
        if (event.isCtrlC) return TuiAction.Quit

        if (state.home.isFilterInputActive) {
            return when {
                event.key == "Escape" || event.key == "Enter" -> TuiAction.DeactivateFilterInput
                event.key == "Backspace" -> TuiAction.RemoveFilterCharacter
                event.key.length == 1 && !event.ctrl && !event.alt -> TuiAction.AppendFilterCharacter(event.key.first())
                else -> TuiAction.NoOp
            }
        }

        return when (event.key) {
            "q", "Q" -> TuiAction.Quit
            "n", "N", "+" -> TuiAction.Navigate(TuiScreenId.ADD_ACCOUNT)
            "ArrowUp", "k", "K" -> TuiAction.SelectPreviousAccount
            "ArrowDown", "j", "J" -> TuiAction.SelectNextAccount
            "Enter", "a", "A" -> TuiAction.OpenSelectedAccount
            "/", "f", "F" -> TuiAction.ActivateFilterInput
            "s", "S" -> TuiAction.Navigate(TuiScreenId.SETTINGS)
            else -> TuiAction.NoOp
        }
    }
}
