package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.widgets.Text

class AccountScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.ACCOUNT

    override fun render(state: TuiAppState): Text {
        val selectedAccount = state.home.accounts.firstOrNull { it.accountId == state.selectedAccountId }
        return Text(
            """
            TwoFac TUI

            Account Screen
            account: ${selectedAccount?.accountLabel ?: "-"}
            issuer : ${selectedAccount?.issuer ?: "-"}
            otp    : ${selectedAccount?.otp ?: "-"}

            Shortcuts:
              b/Escape : Back
              s        : Open settings
              q        : Quit
            """.trimIndent()
        )
    }

    override fun onKey(event: KeyboardEvent, state: TuiAppState): TuiAction {
        if (event.isCtrlC) return TuiAction.Quit

        return when (event.key) {
            "q", "Q" -> TuiAction.Quit
            "s", "S" -> TuiAction.Navigate(TuiScreenId.SETTINGS)
            "Escape", "Backspace", "b", "B" -> TuiAction.Back
            else -> TuiAction.NoOp
        }
    }
}
