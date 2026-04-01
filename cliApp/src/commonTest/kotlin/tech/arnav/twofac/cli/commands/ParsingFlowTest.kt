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
        val root = RootCommand().subcommands(DisplayCommand(), AddCommand(), InfoCommand(), StorageCommand(), BackupCommand())
        val result = root.test("--help")
        assertEquals(0, result.statusCode)
        assertContains(result.output, "Usage:")
    }

    @Test
    fun testAddCommandWithMissingArguments() {
        val result = AddCommand().test()
        assertEquals(1, result.statusCode)
        assertContains(result.output, "Error:")
    }
}
