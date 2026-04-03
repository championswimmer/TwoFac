package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.widgets.Text

class AddAccountScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.ADD_ACCOUNT

    override fun render(state: TuiAppState): Text {
        val errorMessage = state.addAccount.message?.let { "\n$it\n" } ?: ""
        return Text(
            """
            TwoFac TUI

            Add New Account
            
            Paste or type the otpauth:// URI below:
            > ${state.addAccount.uriInput}_
            $errorMessage
            Shortcuts:
              Enter    : Submit
              Escape   : Cancel and go back
              q        : Quit
            """.trimIndent()
        )
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
