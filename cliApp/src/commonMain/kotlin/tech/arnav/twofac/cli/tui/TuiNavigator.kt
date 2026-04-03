package tech.arnav.twofac.cli.tui

import tech.arnav.twofac.cli.storage.CliStorageBackend

class TuiNavigator {
    fun reduce(state: TuiAppState, action: TuiAction): TuiAppState {
        return when (action) {
            TuiAction.NoOp -> state
            TuiAction.Quit -> state.copy(shouldExit = true)
            TuiAction.Back -> pop(state)
            is TuiAction.Navigate -> push(state, action.destination)

            TuiAction.SelectNextAccount -> moveSelection(state, delta = 1)
            TuiAction.SelectPreviousAccount -> moveSelection(state, delta = -1)
            TuiAction.ActivateFilterInput -> state.copy(home = state.home.copy(isFilterInputActive = true))
            TuiAction.DeactivateFilterInput -> state.copy(home = state.home.copy(isFilterInputActive = false))
            is TuiAction.AppendFilterCharacter -> updateFilterQuery(state, state.home.filterQuery + action.character)
            TuiAction.RemoveFilterCharacter -> updateFilterQuery(state, state.home.filterQuery.dropLast(1))
            TuiAction.OpenSelectedAccount -> openSelectedAccount(state)
            is TuiAction.RefreshHomeAccounts -> refreshAccounts(state, action.accounts)

            TuiAction.ActivateRemoveConfirmation -> state.copy(
                account = state.account.copy(
                    isRemoveConfirmationActive = true,
                    message = "Press Enter to confirm removal, Escape to cancel",
                ),
            )
            TuiAction.DeactivateRemoveConfirmation -> state.copy(
                account = state.account.copy(
                    isRemoveConfirmationActive = false,
                    message = null,
                ),
            )
            TuiAction.ConfirmRemoveSelectedAccount -> state
            TuiAction.SubmitNewAccount -> state

            is TuiAction.AppendAddAccountCharacter -> {
                state.copy(
                    addAccount = state.addAccount.copy(
                        uriInput = state.addAccount.uriInput + action.character,
                        message = null
                    )
                )
            }
            TuiAction.RemoveAddAccountCharacter -> {
                state.copy(
                    addAccount = state.addAccount.copy(
                        uriInput = state.addAccount.uriInput.dropLast(1),
                        message = null
                    )
                )
            }

            TuiAction.CycleStorageBackend -> {
                val nextBackend = when (state.settings.backend) {
                    CliStorageBackend.STANDALONE -> CliStorageBackend.COMMON
                    CliStorageBackend.COMMON -> CliStorageBackend.STANDALONE
                }
                state.copy(
                    settings = state.settings.copy(backend = nextBackend),
                    message = "Storage backend set to ${nextBackend.name.lowercase()}",
                )
            }
        }
    }

    private fun pop(state: TuiAppState): TuiAppState {
        val currentStack = state.navigator.stack
        val nextStack = if (currentStack.size > 1) currentStack.dropLast(1) else currentStack
        return state.copy(navigator = state.navigator.copy(stack = nextStack))
    }

    private fun push(state: TuiAppState, destination: TuiScreenId): TuiAppState {
        val currentStack = state.navigator.stack
        if (currentStack.lastOrNull() == destination) return state

        return state.copy(
            navigator = state.navigator.copy(stack = currentStack + destination),
        )
    }

    private fun moveSelection(state: TuiAppState, delta: Int): TuiAppState {
        val filtered = state.home.filteredAccounts()
        if (filtered.isEmpty()) return state

        val nextIndex = (state.home.selectedIndex + delta).coerceIn(0, filtered.lastIndex)
        return state.copy(home = state.home.copy(selectedIndex = nextIndex))
    }

    private fun updateFilterQuery(state: TuiAppState, nextQuery: String): TuiAppState {
        val updatedHome = state.home.copy(filterQuery = nextQuery)
        return state.copy(home = clampSelection(updatedHome))
    }

    private fun openSelectedAccount(state: TuiAppState): TuiAppState {
        val selected = state.home.filteredAccounts().getOrNull(state.home.selectedIndex) ?: return state
        return push(
            state.copy(
                selectedAccountId = selected.accountId,
                account = state.account.copy(
                    isRemoveConfirmationActive = false,
                    message = null,
                ),
            ),
            TuiScreenId.ACCOUNT,
        )
    }

    private fun refreshAccounts(state: TuiAppState, accounts: List<TuiOtpEntry>): TuiAppState {
        val updatedHome = state.home.copy(accounts = accounts)
        return state.copy(home = clampSelection(updatedHome))
    }

    private fun clampSelection(home: HomeScreenState): HomeScreenState {
        val filteredCount = home.filteredAccounts().size
        if (filteredCount <= 0) {
            return home.copy(selectedIndex = 0)
        }

        return home.copy(selectedIndex = home.selectedIndex.coerceIn(0, filteredCount - 1))
    }
}
