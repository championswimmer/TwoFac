package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.testing.test
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.SystemFileSystem
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tech.arnav.twofac.cli.storage.AppDirUtils
import tech.arnav.twofac.cli.storage.CliConfig
import tech.arnav.twofac.cli.storage.CliConfigStore
import tech.arnav.twofac.cli.theme.CliIssuerIcons
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.storage.Storage
import tech.arnav.twofac.lib.theme.AccountColorTag
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
        deleteCliConfig()
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
            twoFacLib.addAccount(GITHUB_URI, AccountColorTag.TEAL)
            twoFacLib.addAccount(CUSTOM_URI)
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        deleteCliConfig()
    }

    @Test
    fun testDisplayCommandShowsIssuerIconsWhenEnabledInSettings() {
        writeCliConfig(issuerIconsEnabled = true)
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
    fun testDisplayCommandShowsColorColumnAndSwatch() {
        writeCliConfig(issuerIconsEnabled = true)

        val result = DisplayCommand().test(
            "--passkey=$PASSKEY",
            outputInteractive = false,
            inputInteractive = false,
        )

        assertEquals(0, result.statusCode)
        assertContains(result.output, "Color")
        assertContains(result.output, "[T]")
    }

    @Test
    fun testDisplayCommandHidesIssuerIconsWhenDisabledInSettings() {
        writeCliConfig(issuerIconsEnabled = false)
        val githubIcon = CliIssuerIcons.glyphForIssuer("GitHub")
        val fallbackIcon = CliIssuerIcons.glyphForIssuer("Custom")

        val result = DisplayCommand().test(
            "--passkey=$PASSKEY",
            outputInteractive = true,
            inputInteractive = false,
        )

        assertEquals(0, result.statusCode)
        assertContains(result.output, "alice@example.com")
        assertContains(result.output, "carol@example.com")
        assertFalse(result.output.contains(githubIcon), "settings opt-out should suppress GitHub icon")
        assertFalse(result.output.contains(fallbackIcon), "settings opt-out should suppress fallback icon")
    }

    @Test
    fun testDisplayCommandUsesSettingsForNonInteractiveOutputToo() {
        writeCliConfig(issuerIconsEnabled = true)
        val githubIcon = CliIssuerIcons.glyphForIssuer("GitHub")

        val result = DisplayCommand().test(
            "--passkey=$PASSKEY",
            outputInteractive = false,
            inputInteractive = false,
        )

        assertEquals(0, result.statusCode)
        assertContains(result.output, "$githubIcon  alice@example.com")
    }

    private fun writeCliConfig(issuerIconsEnabled: Boolean) {
        CliConfigStore.write(CliConfig(issuerIconsEnabled = issuerIconsEnabled))
    }

    private fun deleteCliConfig() {
        val configPath = AppDirUtils.getCliConfigFilePath()
        if (SystemFileSystem.exists(configPath)) {
            SystemFileSystem.delete(configPath)
        }
    }
}
