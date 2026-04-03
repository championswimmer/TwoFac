package tech.arnav.twofac.cli.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TuiNavigatorTest {

    private val navigator = TuiNavigator()

    @Test
    fun testNavigatePushesNewScreen() {
        val state = TuiAppState()

        val next = navigator.reduce(state, TuiAction.Navigate(TuiScreenId.SETTINGS))

        assertEquals(listOf(TuiScreenId.HOME, TuiScreenId.SETTINGS), next.navigator.stack)
        assertFalse(next.shouldExit)
    }

    @Test
    fun testBackPopsStackWhenPossible() {
        val state = TuiAppState(
            navigator = TuiNavigatorState(stack = listOf(TuiScreenId.HOME, TuiScreenId.ACCOUNT))
        )

        val next = navigator.reduce(state, TuiAction.Back)

        assertEquals(listOf(TuiScreenId.HOME), next.navigator.stack)
    }

    @Test
    fun testQuitSetsExitFlag() {
        val state = TuiAppState()

        val next = navigator.reduce(state, TuiAction.Quit)

        assertTrue(next.shouldExit)
    }

    @Test
    fun testActivateRemoveConfirmationSetsFlag() {
        val state = TuiAppState()

        val next = navigator.reduce(state, TuiAction.ActivateRemoveConfirmation)

        assertTrue(next.account.isRemoveConfirmationActive)
    }

    @Test
    fun testDeactivateRemoveConfirmationClearsFlag() {
        val state = TuiAppState(account = AccountScreenState(isRemoveConfirmationActive = true))

        val next = navigator.reduce(state, TuiAction.DeactivateRemoveConfirmation)

        assertFalse(next.account.isRemoveConfirmationActive)
    }

    @Test
    fun testCycleStorageBackendTogglesSetting() {
        val state = TuiAppState(settings = SettingsScreenState(StorageBackendOption.STANDALONE))

        val next = navigator.reduce(state, TuiAction.CycleStorageBackend)

        assertEquals(StorageBackendOption.COMMON, next.settings.backend)
    }
}
