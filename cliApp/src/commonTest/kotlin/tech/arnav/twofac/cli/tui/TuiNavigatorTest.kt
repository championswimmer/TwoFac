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
}
