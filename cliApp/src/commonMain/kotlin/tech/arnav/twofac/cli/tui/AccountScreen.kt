package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.widgets.Text

class AccountScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.ACCOUNT

    override fun render(state: TuiAppState) = Text(
        """
        TwoFac TUI (scaffold)

        Account Screen
        tick: ${state.tick}

        Shortcuts:
          b/Escape : Back
          s        : Open settings
          q        : Quit
        """.trimIndent()
    )

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
