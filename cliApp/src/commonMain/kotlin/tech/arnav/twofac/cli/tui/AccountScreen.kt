package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.ColumnWidth.Companion.Fixed
import com.github.ajalt.mordant.table.table
import tech.arnav.twofac.cli.theme.CliThemeStyles

class AccountScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.ACCOUNT

    override fun render(state: TuiAppState, styles: CliThemeStyles) = table {
        borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
        column(0) { width = Fixed(10) }

        val selectedAccount = state.home.accounts.firstOrNull { it.accountId == state.selectedAccountId }

        header {
            row(styles.title("TwoFac"), styles.title("Account Details"))
        }

        body {
            row(styles.label("account"), styles.key(selectedAccount?.accountLabel ?: "-"))
            row(styles.label("issuer"), selectedAccount?.issuer ?: "-")
            row(
                styles.label("otp"),
                styles.otp(selectedAccount?.otp?.chunked(3)?.joinToString(" ") ?: "-"),
            )
        }

        val removalLine = if (state.account.isRemoveConfirmationActive) {
            styles.danger("Confirm remove: Enter = yes, Escape = cancel")
        } else {
            styles.footer("d remove • b/Esc back • s settings • q quit")
        }
        val footerText = buildString {
            state.account.message?.let { append(it).append("\n") }
            append(removalLine)
        }
        captionBottom(footerText, align = TextAlign.LEFT)
    }

    override fun onKey(event: KeyboardEvent, state: TuiAppState): TuiAction {
        if (event.isCtrlC) return TuiAction.Quit

        if (state.account.isRemoveConfirmationActive) {
            return when (event.key) {
                "Enter", "y", "Y" -> TuiAction.ConfirmRemoveSelectedAccount
                "Escape", "Backspace", "n", "N" -> TuiAction.DeactivateRemoveConfirmation
                else -> TuiAction.NoOp
            }
        }

        return when (event.key) {
            "q", "Q" -> TuiAction.Quit
            "s", "S" -> TuiAction.Navigate(TuiScreenId.SETTINGS)
            "d", "D", "Delete" -> TuiAction.ActivateRemoveConfirmation
            "Escape", "Backspace", "b", "B" -> TuiAction.Back
            else -> TuiAction.NoOp
        }
    }
}
