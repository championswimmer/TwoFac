package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.widgets.Text

class HomeScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.HOME

    override fun render(state: TuiAppState) = Text(
        """
        TwoFac TUI (scaffold)

        Home Screen
        tick: ${state.tick}

        Shortcuts:
          Enter/a : Open account screen
          s       : Open settings
          q       : Quit
        """.trimIndent()
    )

    override fun onKey(event: KeyboardEvent, state: TuiAppState): TuiAction {
        if (event.isCtrlC) return TuiAction.Quit

        return when (event.key) {
            "q", "Q" -> TuiAction.Quit
            "Enter", "a", "A" -> TuiAction.Navigate(TuiScreenId.ACCOUNT)
            "s", "S" -> TuiAction.Navigate(TuiScreenId.SETTINGS)
            else -> TuiAction.NoOp
        }
    }
}
