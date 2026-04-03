package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.prompt
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.lib.TwoFacLib
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class TuiApp(
    private val terminal: Terminal = Terminal(),
    private val navigator: TuiNavigator = TuiNavigator(),
    private val screens: Map<TuiScreenId, TuiScreen> = defaultScreens(),
    private val renderLoop: TuiRenderLoop = TuiRenderLoop(terminal),
) : KoinComponent {
    private val twoFacLib: TwoFacLib by inject()

    @OptIn(ExperimentalTime::class)
    fun run() {
        val passkey = terminal.prompt("Enter passkey", hideInput = true).orEmpty()
        if (passkey.isBlank()) {
            terminal.println("Passkey cannot be blank")
            return
        }

        val initialAccounts = runCatching {
            runBlocking {
                twoFacLib.unlock(passkey)
                fetchOtpEntries()
            }
        }.getOrElse { error ->
            terminal.println("Failed to unlock account store: ${error.message}")
            return
        }

        val initialState = TuiAppState(
            nowEpochSeconds = Clock.System.now().epochSeconds,
            home = HomeScreenState(accounts = initialAccounts),
        )

        runCatching {
            renderLoop.run(
                initialState = initialState,
                render = { state -> screenFor(state).render(state) },
                onKey = { event, state ->
                    val action = screenFor(state).onKey(event, state)
                    navigator.reduce(state, action)
                },
                onTick = { state -> refreshHomeAccounts(state) },
            )
        }.onFailure { error ->
            terminal.println("Failed to launch interactive mode: ${error.message}")
        }
    }

    private fun screenFor(state: TuiAppState): TuiScreen {
        return screens[state.navigator.current] ?: screens.getValue(TuiScreenId.HOME)
    }

    private fun refreshHomeAccounts(state: TuiAppState): TuiAppState {
        val accounts = runCatching {
            runBlocking { fetchOtpEntries() }
        }.getOrElse { error ->
            return state.copy(message = "Refresh failed: ${error.message}")
        }

        return navigator.reduce(
            state.copy(message = null),
            TuiAction.RefreshHomeAccounts(accounts),
        )
    }

    private suspend fun fetchOtpEntries(): List<TuiOtpEntry> {
        return twoFacLib.getAllAccountOTPs().map { (account, otp) ->
            TuiOtpEntry(
                accountId = account.accountID,
                accountLabel = account.accountLabel,
                issuer = account.issuer,
                otp = otp,
                nextCodeAt = account.nextCodeAt,
            )
        }
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
