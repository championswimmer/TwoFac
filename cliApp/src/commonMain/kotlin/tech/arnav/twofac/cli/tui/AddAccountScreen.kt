package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.ColumnWidth.Companion.Fixed
import com.github.ajalt.mordant.table.table
import tech.arnav.twofac.cli.theme.CliThemeStyles

class AddAccountScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.ADD_ACCOUNT

    override fun render(state: TuiAppState, styles: CliThemeStyles) = table {
        borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
        column(0) { width = Fixed(10) }

        header {
            row(styles.title("TwoFac"), styles.title("Add Account"))
        }

        body {
            row(styles.label("uri input"), styles.key(state.addAccount.uriInput) + styles.label("_"))
        }

        val errorLine = state.addAccount.message?.let { styles.danger(it) + "\n" } ?: ""
        val footerText = errorLine + styles.footer("Enter submit • Esc cancel • q quit")
        captionBottom(footerText, align = TextAlign.LEFT)
    }

    override fun onKey(event: KeyboardEvent, state: TuiAppState): TuiAction {
        if (event.isCtrlC) return TuiAction.Quit

        return when {
            event.key == "Escape" || (event.key == "b" && state.addAccount.uriInput.isEmpty()) -> TuiAction.Back
            event.key == "Enter" -> TuiAction.SubmitNewAccount
            event.key == "Backspace" -> TuiAction.RemoveAddAccountCharacter
            event.key.length == 1 && !event.ctrl && !event.alt -> TuiAction.AppendAddAccountCharacter(event.key.first())
            else -> TuiAction.NoOp
        }
    }
}
