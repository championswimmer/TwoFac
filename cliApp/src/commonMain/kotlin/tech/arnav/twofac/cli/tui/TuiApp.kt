package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.prompt
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.arnav.twofac.cli.storage.CliConfig
import tech.arnav.twofac.cli.storage.CliConfigStore
import tech.arnav.twofac.cli.theme.CliTheme
import tech.arnav.twofac.cli.theme.CliThemeStyles
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
    private val styles: CliThemeStyles = CliTheme.styles(terminal)

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
            settings = SettingsScreenState(backend = CliConfigStore.read().storageBackend),
        )

        runCatching {
            renderLoop.run(
                initialState = initialState,
                render = { state -> screenFor(state).render(state, styles) },
                onKey = { event, state ->
                    val action = screenFor(state).onKey(event, state)
                    applyAction(state, action)
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

    private fun applyAction(state: TuiAppState, action: TuiAction): TuiAppState {
        return when (action) {
            TuiAction.ConfirmRemoveSelectedAccount -> removeSelectedAccount(state)
            TuiAction.SubmitNewAccount -> submitNewAccount(state)
            TuiAction.CycleStorageBackend -> {
                val nextState = navigator.reduce(state, action)
                if (!CliConfigStore.write(CliConfig(storageBackend = nextState.settings.backend))) {
                    nextState.copy(message = "Failed to persist storage backend setting")
                } else {
                    nextState
                }
            }
            else -> navigator.reduce(state, action)
        }
    }

    private fun removeSelectedAccount(state: TuiAppState): TuiAppState {
        val accountId = state.selectedAccountId
            ?: return state.copy(
                account = state.account.copy(
                    isRemoveConfirmationActive = false,
                    message = "No account selected",
                ),
            )

        val removed = runCatching {
            runBlocking { twoFacLib.deleteAccount(accountId) }
        }.getOrElse { error ->
            return state.copy(
                account = state.account.copy(
                    isRemoveConfirmationActive = false,
                    message = "Remove failed: ${error.message}",
                ),
            )
        }

        if (!removed) {
            return state.copy(
                account = state.account.copy(
                    isRemoveConfirmationActive = false,
                    message = "Account not found",
                ),
            )
        }

        val refreshedState = refreshHomeAccounts(
            state.copy(
                selectedAccountId = null,
                account = AccountScreenState(),
                message = "Account removed successfully",
            ),
        )
        return navigator.reduce(refreshedState, TuiAction.Back)
    }

    private fun submitNewAccount(state: TuiAppState): TuiAppState {
        val uri = state.addAccount.uriInput.trim()
        if (uri.isBlank()) {
            return state.copy(addAccount = state.addAccount.copy(message = "URI cannot be empty"))
        }

        val success = runCatching {
            runBlocking { twoFacLib.addAccount(uri) }
        }.getOrElse { error ->
            return state.copy(addAccount = state.addAccount.copy(message = "Failed: ${error.message}"))
        }

        if (!success) {
            return state.copy(addAccount = state.addAccount.copy(message = "Failed to add account. Storage returned false."))
        }

        val refreshedState = refreshHomeAccounts(
            state.copy(
                addAccount = AddAccountScreenState(),
                message = "Account added successfully",
            )
        )
        return navigator.reduce(refreshedState, TuiAction.Back)
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
                AddAccountScreen(),
            ).associateBy { it.id }
        }
    }
}
