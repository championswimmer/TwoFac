package tech.arnav.twofac.cli.tui

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import tech.arnav.twofac.cli.theme.CliIssuerIcons
import tech.arnav.twofac.cli.theme.CliTheme
import tech.arnav.twofac.lib.otp.OtpCodes
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class HomeScreenRenderTest {
    private val accounts = listOf(
        TuiOtpEntry("1", "alice@example.com", "GitHub", OtpCodes("123456"), 30),
        TuiOtpEntry("2", "bob@example.com", "Google", OtpCodes("654321"), 30),
    )

    @Test
    fun testHomeScreenShowsIssuerIconsWhenEnabledInSettings() {
        val output = renderHomeScreen(
            TuiAppState(
                home = HomeScreenState(accounts = accounts, selectedIndex = 1),
                settings = SettingsScreenState(issuerIconsEnabled = true),
                nowEpochSeconds = 0,
            )
        )

        assertContains(output, "${CliIssuerIcons.glyphForIssuer("GitHub")}  alice@example.com")
    }

    @Test
    fun testHomeScreenHidesIssuerIconsWhenDisabledInSettings() {
        val githubIcon = CliIssuerIcons.glyphForIssuer("GitHub")
        val output = renderHomeScreen(
            TuiAppState(
                home = HomeScreenState(accounts = accounts, selectedIndex = 1),
                settings = SettingsScreenState(issuerIconsEnabled = false),
                nowEpochSeconds = 0,
            )
        )

        assertContains(output, "alice@example.com")
        assertFalse(output.contains(githubIcon), "home screen should not render icons when disabled")
    }

    private fun renderHomeScreen(state: TuiAppState): String {
        val recorder = TerminalRecorder()
        val terminal = Terminal(terminalInterface = recorder)
        terminal.println(HomeScreen().render(state, CliTheme.styles(terminal)))
        return recorder.output()
    }
}
