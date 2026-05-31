package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.ColumnWidth.Companion.Fixed
import com.github.ajalt.mordant.table.table
import tech.arnav.twofac.cli.theme.CliIssuerIcons
import tech.arnav.twofac.cli.theme.CliThemeStyles

class AccountScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.ACCOUNT

    private fun renderColorPicker(accountState: AccountScreenState, styles: CliThemeStyles): String {
        return accountColorChoices.mapIndexed { index, color ->
            val label = if (color == null) "None" else styles.accountColorLabel(color)
            if (index == accountState.selectedColorIndex) {
                "${styles.key("›")} $label ${styles.key("‹")}"
            } else {
                "  $label  "
            }
        }.joinToString(" ")
    }

    override fun render(state: TuiAppState, styles: CliThemeStyles) = table {
        borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
        column(0) { width = Fixed(10) }

        val selectedAccount = state.home.accounts.firstOrNull { it.accountId == state.selectedAccountId }

        header {
            row(styles.title("TwoFac"), styles.title("Account Details"))
        }

        body {
            row(styles.label("account"), styles.key(selectedAccount?.accountLabel ?: "-"))
            row(
                styles.label("issuer"),
                CliIssuerIcons.formatIssuerLabel(
                    issuer = selectedAccount?.issuer,
                    iconsEnabled = state.settings.issuerIconsEnabled,
                )
            )
            row(styles.label("color"), styles.accountColorLabel(selectedAccount?.color))
            if (state.account.isColorPickerActive) {
                row(styles.label("pick color"), renderColorPicker(state.account, styles))
            }
            row(
                styles.label("otp"),
                styles.otp(selectedAccount?.otp?.currentOTP?.chunked(3)?.joinToString(" ") ?: "-"),
            )
        }

        val removalLine = when {
            state.account.isRemoveConfirmationActive -> styles.danger("Confirm remove: Enter = yes, Escape = cancel")
            state.account.isColorPickerActive -> styles.footer("←/→ cycle color • Enter save • Esc cancel")
            else -> styles.footer("c color • d remove • b/Esc back • s settings • q quit")
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

        if (state.account.isColorPickerActive) {
            return when (event.key) {
                "Enter", "y", "Y" -> TuiAction.ConfirmSelectedAccountColor
                "Escape", "Backspace", "n", "N" -> TuiAction.DeactivateColorPicker
                "ArrowRight", "ArrowDown", "l", "L", "j", "J" -> TuiAction.SelectNextAccountColor
                "ArrowLeft", "ArrowUp", "h", "H", "k", "K" -> TuiAction.SelectPreviousAccountColor
                else -> TuiAction.NoOp
            }
        }

        return when (event.key) {
            "q", "Q" -> TuiAction.Quit
            "s", "S" -> TuiAction.Navigate(TuiScreenId.SETTINGS)
            "c", "C" -> TuiAction.ActivateColorPicker
            "d", "D", "Delete" -> TuiAction.ActivateRemoveConfirmation
            "Escape", "Backspace", "b", "B" -> TuiAction.Back
            else -> TuiAction.NoOp
        }
    }
}
