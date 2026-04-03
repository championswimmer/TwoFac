package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.terminal.Terminal
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TuiApp(
    private val terminal: Terminal = Terminal(),
    private val navigator: TuiNavigator = TuiNavigator(),
    private val screens: Map<TuiScreenId, TuiScreen> = defaultScreens(),
    private val renderLoop: TuiRenderLoop = TuiRenderLoop(terminal),
) {
    @OptIn(ExperimentalTime::class)
    fun run() {
        val initialState = TuiAppState(nowEpochSeconds = Clock.System.now().epochSeconds)

        runCatching {
            renderLoop.run(
                initialState = initialState,
                render = { state -> screenFor(state).render(state) },
                onKey = { event, state ->
                    val action = screenFor(state).onKey(event, state)
                    navigator.reduce(state, action)
                },
            )
        }.onFailure { error ->
            terminal.println("Failed to launch interactive mode: ${error.message}")
        }
    }

    private fun screenFor(state: TuiAppState): TuiScreen {
        return screens[state.navigator.current] ?: screens.getValue(TuiScreenId.HOME)
    }

    companion object {
        fun defaultScreens(): Map<TuiScreenId, TuiScreen> {
            return listOf(
                HomeScreen(),
                AccountScreen(),
                SettingsScreen(),
            ).associateBy { it.id }
        }
    }
}
