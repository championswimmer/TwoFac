package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.rendering.Widget

enum class TuiScreenId {
    HOME,
    ACCOUNT,
    SETTINGS,
}

data class TuiNavigatorState(
    val stack: List<TuiScreenId> = listOf(TuiScreenId.HOME),
) {
    val current: TuiScreenId
        get() = stack.lastOrNull() ?: TuiScreenId.HOME
}

data class TuiAppState(
    val navigator: TuiNavigatorState = TuiNavigatorState(),
    val shouldExit: Boolean = false,
    val tick: Long = 0,
    val nowEpochSeconds: Long = 0,
)

sealed interface TuiAction {
    data object NoOp : TuiAction
    data object Quit : TuiAction
    data object Back : TuiAction
    data class Navigate(val destination: TuiScreenId) : TuiAction
}

interface TuiScreen {
    val id: TuiScreenId

    fun render(state: TuiAppState): Widget

    fun onKey(event: KeyboardEvent, state: TuiAppState): TuiAction = TuiAction.NoOp
}
