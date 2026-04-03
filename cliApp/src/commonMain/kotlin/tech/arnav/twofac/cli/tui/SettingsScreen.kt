package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.widgets.Text

class SettingsScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.SETTINGS

    override fun render(state: TuiAppState): Text {
        val backend = state.settings.backend.name.lowercase()
        return Text(
            """
            TwoFac TUI

            Settings Screen

            Storage backend: $backend
            (press 'u' to switch between standalone/common)

            Backup provider surface:
              - local provider: available
              - provider auth/list UI: placeholder (next phases)

            ${state.message ?: ""}

            Shortcuts:
              u        : Toggle backend
              h        : Home
              b/Escape : Back
              q        : Quit
            """.trimIndent()
        )
    }

    override fun onKey(event: KeyboardEvent, state: TuiAppState): TuiAction {
        if (event.isCtrlC) return TuiAction.Quit

        return when (event.key) {
            "q", "Q" -> TuiAction.Quit
            "u", "U" -> TuiAction.CycleStorageBackend
            "h", "H" -> TuiAction.Navigate(TuiScreenId.HOME)
            "Escape", "Backspace", "b", "B" -> TuiAction.Back
            else -> TuiAction.NoOp
        }
    }
}
