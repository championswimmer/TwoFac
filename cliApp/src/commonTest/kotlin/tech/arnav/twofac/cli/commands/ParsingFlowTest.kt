package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.storage.Storage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ParsingFlowTest {

    @BeforeTest
    fun setup() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single<Storage> { MemoryStorage() }
                    single<TwoFacLib> { TwoFacLib.initialise(storage = get(), passKey = "test") }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testRootCommandHelp() {
        val root = tech.arnav.twofac.cli.MainCommand().subcommands(
            DisplayCommand(),
            InfoCommand(),
            AccountsCommand(),
            StorageCommand(),
        )
        val result = root.test("--help")
        assertEquals(0, result.statusCode)
        assertContains(result.output, "Usage:")
        assertContains(result.output, "accounts")
        assertContains(result.output, "storage")
    }

    @Test
    fun testAccountsAddCommandWithMissingArguments() {
        val root = tech.arnav.twofac.cli.MainCommand().subcommands(
            DisplayCommand(),
            InfoCommand(),
            AccountsCommand(),
            StorageCommand(),
        )
        val result = root.test("accounts add")
        assertEquals(1, result.statusCode)
        assertContains(result.output, "Error:")
    }

    @Test
    fun testStorageBackupExportHelpParses() {
        val root = tech.arnav.twofac.cli.MainCommand().subcommands(
            DisplayCommand(),
            InfoCommand(),
            AccountsCommand(),
            StorageCommand(),
        )
        val result = root.test("storage backup export --help")
        assertEquals(0, result.statusCode)
        assertContains(result.output, "Export all accounts")
    }
}
