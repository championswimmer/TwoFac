package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.input.enterRawMode
import com.github.ajalt.mordant.terminal.Terminal
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

class TuiRenderLoop(
    private val terminal: Terminal,
) {
    @OptIn(ExperimentalTime::class)
    fun run(
        initialState: TuiAppState,
        render: (TuiAppState) -> com.github.ajalt.mordant.rendering.Widget,
        onKey: (KeyboardEvent, TuiAppState) -> TuiAppState,
    ): TuiAppState {
        var state = if (initialState.nowEpochSeconds == 0L) {
            initialState.copy(nowEpochSeconds = Clock.System.now().epochSeconds)
        } else {
            initialState
        }

        val animation = terminal.animation<TuiAppState> { appState ->
            render(appState)
        }

        terminal.cursor.hide(showOnExit = true)
        animation.update(state)

        terminal.enterRawMode().use { rawMode ->
            while (!state.shouldExit) {
                val event = rawMode.readKeyOrNull(250.milliseconds)
                if (event != null) {
                    state = onKey(event, state)
                    animation.update(state)
                    continue
                }

                val nowEpochSeconds = Clock.System.now().epochSeconds
                if (nowEpochSeconds != state.nowEpochSeconds) {
                    state = state.copy(
                        nowEpochSeconds = nowEpochSeconds,
                        tick = state.tick + 1,
                    )
                    animation.update(state)
                }
            }
        }

        animation.stop()
        return state
    }
}
