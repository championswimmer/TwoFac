package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import tech.arnav.twofac.cli.theme.CliTheme
import tech.arnav.twofac.lib.otp.OtpCodes
import tech.arnav.twofac.lib.theme.AccountColorTag
import kotlin.test.Test
import kotlin.test.assertContains

class AccountScreenRenderTest {

    @Test
    fun testAccountScreenShowsSelectedAccountColor() {
        val output = renderAccountScreen(
            TuiAppState(
                home = HomeScreenState(accounts = listOf(coloredAccount)),
                selectedAccountId = coloredAccount.accountId,
            )
        )

        assertContains(output, "color")
        assertContains(output, "Teal")
        assertContains(output, "██")
    }

    @Test
    fun testAccountScreenShowsColorPickerWhenActive() {
        val output = renderAccountScreen(
            TuiAppState(
                home = HomeScreenState(accounts = listOf(coloredAccount)),
                selectedAccountId = coloredAccount.accountId,
                account = AccountScreenState(
                    isColorPickerActive = true,
                    selectedColorIndex = accountColorChoices.indexOf(AccountColorTag.TEAL),
                ),
            )
        )

        assertContains(output, "pick color")
        assertContains(output, "None")
        assertContains(output, "Teal")
        assertContains(output, "Enter save")
    }

    private fun renderAccountScreen(state: TuiAppState): String {
        val recorder = TerminalRecorder()
        val terminal = Terminal(terminalInterface = recorder)
        terminal.println(AccountScreen().render(state, CliTheme.styles(terminal)))
        return recorder.output()
    }

    private companion object {
        val coloredAccount = TuiOtpEntry(
            accountId = "1",
            accountLabel = "alice@example.com",
            issuer = "GitHub",
            otp = OtpCodes("123456"),
            nextCodeAt = 30,
            color = AccountColorTag.TEAL,
        )
    }
}
