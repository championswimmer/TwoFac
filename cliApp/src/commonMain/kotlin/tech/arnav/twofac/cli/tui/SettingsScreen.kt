package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.widgets.Text

class SettingsScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.SETTINGS

    override fun render(state: TuiAppState) = Text(
        """
        TwoFac TUI (scaffold)

        Settings Screen
        tick: ${state.tick}

        Shortcuts:
          h       : Home
          b/Escape: Back
          q       : Quit
        """.trimIndent()
    )

    override fun onKey(event: KeyboardEvent, state: TuiAppState): TuiAction {
        if (event.isCtrlC) return TuiAction.Quit

        return when (event.key) {
            "q", "Q" -> TuiAction.Quit
            "h", "H" -> TuiAction.Navigate(TuiScreenId.HOME)
            "Escape", "Backspace", "b", "B" -> TuiAction.Back
            else -> TuiAction.NoOp
        }
    }
}
