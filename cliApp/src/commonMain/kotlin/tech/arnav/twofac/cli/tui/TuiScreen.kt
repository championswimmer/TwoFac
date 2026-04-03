package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.input.KeyboardEvent
import com.github.ajalt.mordant.rendering.Widget

enum class TuiScreenId {
    HOME,
    ACCOUNT,
    SETTINGS,
}

data class TuiOtpEntry(
    val accountId: String,
    val accountLabel: String,
    val issuer: String?,
    val otp: String,
    val nextCodeAt: Long,
)

data class HomeScreenState(
    val accounts: List<TuiOtpEntry> = emptyList(),
    val filterQuery: String = "",
    val isFilterInputActive: Boolean = false,
    val selectedIndex: Int = 0,
)

data class TuiNavigatorState(
    val stack: List<TuiScreenId> = listOf(TuiScreenId.HOME),
) {
    val current: TuiScreenId
        get() = stack.lastOrNull() ?: TuiScreenId.HOME
}

data class AccountScreenState(
    val isRemoveConfirmationActive: Boolean = false,
    val message: String? = null,
)

data class TuiAppState(
    val navigator: TuiNavigatorState = TuiNavigatorState(),
    val home: HomeScreenState = HomeScreenState(),
    val account: AccountScreenState = AccountScreenState(),
    val selectedAccountId: String? = null,
    val shouldExit: Boolean = false,
    val tick: Long = 0,
    val nowEpochSeconds: Long = 0,
    val message: String? = null,
)

sealed interface TuiAction {
    data object NoOp : TuiAction
    data object Quit : TuiAction
    data object Back : TuiAction
    data class Navigate(val destination: TuiScreenId) : TuiAction

    data object SelectNextAccount : TuiAction
    data object SelectPreviousAccount : TuiAction
    data object ActivateFilterInput : TuiAction
    data object DeactivateFilterInput : TuiAction
    data class AppendFilterCharacter(val character: Char) : TuiAction
    data object RemoveFilterCharacter : TuiAction
    data object OpenSelectedAccount : TuiAction
    data class RefreshHomeAccounts(val accounts: List<TuiOtpEntry>) : TuiAction

    data object ActivateRemoveConfirmation : TuiAction
    data object DeactivateRemoveConfirmation : TuiAction
    data object ConfirmRemoveSelectedAccount : TuiAction
}

interface TuiScreen {
    val id: TuiScreenId

    fun render(state: TuiAppState): Widget

    fun onKey(event: KeyboardEvent, state: TuiAppState): TuiAction = TuiAction.NoOp
}

fun HomeScreenState.filteredAccounts(): List<TuiOtpEntry> {
    val normalizedQuery = filterQuery.trim().lowercase()
    if (normalizedQuery.isBlank()) return accounts

    return accounts.filter { account ->
        account.accountLabel.lowercase().contains(normalizedQuery) ||
            (account.issuer?.lowercase()?.contains(normalizedQuery) == true)
    }
}
