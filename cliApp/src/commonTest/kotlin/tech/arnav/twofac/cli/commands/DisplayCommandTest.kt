package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.testing.test
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tech.arnav.twofac.cli.theme.CliIssuerIcons
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.storage.Storage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DisplayCommandTest {
    private companion object {
        const val PASSKEY = "testpasskey"
        const val GITHUB_URI =
            "otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
        const val CUSTOM_URI =
            "otpauth://totp/Custom:carol@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Custom"
    }

    private lateinit var koinApp: KoinApplication
    private lateinit var twoFacLib: TwoFacLib

    @BeforeTest
    fun setup() {
        runBlocking {
            stopKoin()
            koinApp = startKoin {
                modules(
                    module {
                        single<Storage> { MemoryStorage() }
                        single<TwoFacLib> { TwoFacLib.initialise(storage = get(), passKey = PASSKEY) }
                    }
                )
            }
            twoFacLib = koinApp.koin.get()
            twoFacLib.addAccount(GITHUB_URI)
            twoFacLib.addAccount(CUSTOM_URI)
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testDisplayCommandShowsIssuerIconsForInteractiveTerminalOutput() {
        val githubIcon = CliIssuerIcons.glyphForIssuer("GitHub")
        val fallbackIcon = CliIssuerIcons.glyphForIssuer("Custom")

        val result = DisplayCommand().test(
            "--passkey=$PASSKEY",
            outputInteractive = true,
            inputInteractive = false,
        )

        assertEquals(0, result.statusCode)
        assertContains(result.output, "$githubIcon  alice@example.com")
        assertContains(result.output, "$fallbackIcon  carol@example.com")
    }

    @Test
    fun testDisplayCommandDisablesIconsWithNoIconsFlag() {
        val githubIcon = CliIssuerIcons.glyphForIssuer("GitHub")
        val fallbackIcon = CliIssuerIcons.glyphForIssuer("Custom")

        val result = DisplayCommand().test(
            "--passkey=$PASSKEY --no-icons",
            outputInteractive = true,
            inputInteractive = false,
            envvars = mapOf(CliIssuerIcons.ENV_VAR_NAME to "1"),
        )

        assertEquals(0, result.statusCode)
        assertContains(result.output, "alice@example.com")
        assertContains(result.output, "carol@example.com")
        assertFalse(result.output.contains(githubIcon), "--no-icons should suppress GitHub icon")
        assertFalse(result.output.contains(fallbackIcon), "--no-icons should suppress fallback icon")
    }

    @Test
    fun testDisplayCommandCanForceIconsViaEnvironmentForNonInteractiveOutput() {
        val githubIcon = CliIssuerIcons.glyphForIssuer("GitHub")

        val result = DisplayCommand().test(
            "--passkey=$PASSKEY",
            outputInteractive = false,
            inputInteractive = false,
            envvars = mapOf(CliIssuerIcons.ENV_VAR_NAME to "1"),
        )

        assertEquals(0, result.statusCode)
        assertContains(result.output, "$githubIcon  alice@example.com")
    }

    @Test
    fun testDisplayCommandRespectsEnvironmentOptOut() {
        val githubIcon = CliIssuerIcons.glyphForIssuer("GitHub")

        val result = DisplayCommand().test(
            "--passkey=$PASSKEY",
            outputInteractive = true,
            inputInteractive = false,
            envvars = mapOf(CliIssuerIcons.ENV_VAR_NAME to "0"),
        )

        assertEquals(0, result.statusCode)
        assertContains(result.output, "alice@example.com")
        assertFalse(result.output.contains(githubIcon), "env opt-out should suppress icons")
    }
}
