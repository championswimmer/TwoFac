package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.ColumnWidth.Companion.Fixed
import com.github.ajalt.mordant.table.table
import tech.arnav.twofac.cli.theme.CliThemeStyles

class SettingsScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.SETTINGS

    override fun render(state: TuiAppState, styles: CliThemeStyles) = table {
        borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
        column(0) { width = Fixed(18) }

        header {
            row(styles.title("TwoFac"), styles.title("Settings"))
        }

        body {
            row(styles.label("storage backend"), styles.key(state.settings.backend.cliValue))
            row(styles.label("backup provider"), styles.label("local (available)"))
        }

        val messageLine = state.message?.let { styles.key(it) + "\n" } ?: ""
        val footerText = messageLine + styles.footer("u toggle backend • h home • b/Esc back • q quit")
        captionBottom(footerText, align = TextAlign.LEFT)
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
