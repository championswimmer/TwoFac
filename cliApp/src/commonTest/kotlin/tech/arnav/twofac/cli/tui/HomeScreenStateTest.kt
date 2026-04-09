package tech.arnav.twofac.cli.tui

import tech.arnav.twofac.lib.otp.OtpCodes
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeScreenStateTest {

    private val accounts = listOf(
        TuiOtpEntry("1", "alice@example.com", "GitHub", OtpCodes("123456"), 30),
        TuiOtpEntry("2", "bob@example.com", "Google", OtpCodes("654321"), 30),
        TuiOtpEntry("3", "team@example.com", "GitLab", OtpCodes("111111"), 30),
    )

    private val navigator = TuiNavigator()

    @Test
    fun testFilterMatchesIssuerAndAccountLabel() {
        val state = HomeScreenState(accounts = accounts, filterQuery = "git")

        val filtered = state.filteredAccounts()

        assertEquals(2, filtered.size)
        assertEquals(listOf("1", "3"), filtered.map { it.accountId })
    }

    @Test
    fun testSelectionMovesWithinFilteredRows() {
        val initial = TuiAppState(home = HomeScreenState(accounts = accounts, filterQuery = "git"))

        val next = navigator.reduce(initial, TuiAction.SelectNextAccount)

        assertEquals(1, next.home.selectedIndex)
    }

    @Test
    fun testOpenSelectedAccountNavigatesToAccountScreen() {
        val initial = TuiAppState(home = HomeScreenState(accounts = accounts, selectedIndex = 1))

        val next = navigator.reduce(initial, TuiAction.OpenSelectedAccount)

        assertEquals("2", next.selectedAccountId)
        assertEquals(TuiScreenId.ACCOUNT, next.navigator.current)
    }
}
