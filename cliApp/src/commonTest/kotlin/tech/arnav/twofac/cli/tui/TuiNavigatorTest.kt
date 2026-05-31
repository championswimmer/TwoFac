package tech.arnav.twofac.cli.tui

import tech.arnav.twofac.cli.storage.CliStorageBackend
import tech.arnav.twofac.lib.otp.OtpCodes
import tech.arnav.twofac.lib.theme.AccountColorTag
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
    fun testActivateColorPickerSelectsCurrentAccountColor() {
        val state = TuiAppState(
            home = HomeScreenState(
                accounts = listOf(
                    TuiOtpEntry("1", "alice@example.com", "GitHub", OtpCodes("123456"), 30, AccountColorTag.BLUE),
                ),
            ),
            selectedAccountId = "1",
        )

        val next = navigator.reduce(state, TuiAction.ActivateColorPicker)

        assertTrue(next.account.isColorPickerActive)
        assertEquals(AccountColorTag.BLUE, next.account.selectedPendingColor)
    }

    @Test
    fun testColorPickerCyclesThroughNoneAndPalette() {
        val state = TuiAppState(account = AccountScreenState(isColorPickerActive = true, selectedColorIndex = 0))

        val next = navigator.reduce(state, TuiAction.SelectNextAccountColor)
        val previous = navigator.reduce(next, TuiAction.SelectPreviousAccountColor)

        assertEquals(AccountColorTag.RED, next.account.selectedPendingColor)
        assertEquals(null, previous.account.selectedPendingColor)
    }

    @Test
    fun testCycleStorageBackendTogglesSetting() {
        val state = TuiAppState(settings = SettingsScreenState(CliStorageBackend.STANDALONE))

        val next = navigator.reduce(state, TuiAction.CycleStorageBackend)

        assertEquals(CliStorageBackend.COMMON, next.settings.backend)
    }

    @Test
    fun testToggleIssuerIconsFlipsSetting() {
        val state = TuiAppState(settings = SettingsScreenState(issuerIconsEnabled = true))

        val next = navigator.reduce(state, TuiAction.ToggleIssuerIcons)

        assertFalse(next.settings.issuerIconsEnabled)
    }
}
