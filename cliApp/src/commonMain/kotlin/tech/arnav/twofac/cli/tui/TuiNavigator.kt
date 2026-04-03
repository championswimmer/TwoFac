package tech.arnav.twofac.cli.tui

class TuiNavigator {
    fun reduce(state: TuiAppState, action: TuiAction): TuiAppState {
        return when (action) {
            TuiAction.NoOp -> state
            TuiAction.Quit -> state.copy(shouldExit = true)
            TuiAction.Back -> {
                val currentStack = state.navigator.stack
                val nextStack = if (currentStack.size > 1) currentStack.dropLast(1) else currentStack
                state.copy(navigator = state.navigator.copy(stack = nextStack))
            }
            is TuiAction.Navigate -> {
                val currentStack = state.navigator.stack
                if (currentStack.lastOrNull() == action.destination) {
                    state
                } else {
                    state.copy(navigator = state.navigator.copy(stack = currentStack + action.destination))
                }
            }
        }
    }
}
