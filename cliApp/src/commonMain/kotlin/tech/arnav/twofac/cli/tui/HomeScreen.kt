package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.isCtrlC
import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.ColumnWidth.Companion.Fixed
import com.github.ajalt.mordant.table.table

class HomeScreen : TuiScreen {
    override val id: TuiScreenId = TuiScreenId.HOME

    override fun render(state: TuiAppState) = table {
        borderType = BorderType.SQUARE_DOUBLE_SECTION_SEPARATOR
        column(0) {
            width = Fixed(1)
            cellBorders = Borders.NONE
        }

        header {
            row(" ", "Account", "Issuer", "OTP", "TTL")
        }

        body {
            val filteredAccounts = state.home.filteredAccounts()
            if (filteredAccounts.isEmpty()) {
                row("", "No matching accounts", "", "", "")
            } else {
                filteredAccounts.forEachIndexed { index, account ->
                    val selectedMarker = if (index == state.home.selectedIndex) ">" else " "
                    val otp = account.otp.chunked(1).joinToString(" ")
                    val ttl = if (account.nextCodeAt <= 0L) {
                        "--"
                    } else {
                        "${(account.nextCodeAt - state.nowEpochSeconds).coerceAtLeast(0)}s"
                    }
                    row(selectedMarker, account.accountLabel, account.issuer ?: "-", otp, ttl)
                }
            }
        }

        val filterPrefix = if (state.home.isFilterInputActive) "(filtering)" else ""
        val footerText = buildString {
            append("filter: '${state.home.filterQuery}' $filterPrefix")
            state.message?.let {
                append("\n")
                append(it)
            }
            append("\n↑/↓ move • Enter open • / or f filter • s settings • q quit")
        }
        captionBottom(footerText, align = TextAlign.LEFT)
    }

    override fun onKey(event: KeyboardEvent, state: TuiAppState): TuiAction {
        if (event.isCtrlC) return TuiAction.Quit

        if (state.home.isFilterInputActive) {
            return when {
                event.key == "Escape" || event.key == "Enter" -> TuiAction.DeactivateFilterInput
                event.key == "Backspace" -> TuiAction.RemoveFilterCharacter
                event.key.length == 1 && !event.ctrl && !event.alt -> TuiAction.AppendFilterCharacter(event.key.first())
                else -> TuiAction.NoOp
            }
        }

        return when (event.key) {
            "q", "Q" -> TuiAction.Quit
            "ArrowUp", "k", "K" -> TuiAction.SelectPreviousAccount
            "ArrowDown", "j", "J" -> TuiAction.SelectNextAccount
            "Enter", "a", "A" -> TuiAction.OpenSelectedAccount
            "/", "f", "F" -> TuiAction.ActivateFilterInput
            "s", "S" -> TuiAction.Navigate(TuiScreenId.SETTINGS)
            else -> TuiAction.NoOp
        }
    }
}
