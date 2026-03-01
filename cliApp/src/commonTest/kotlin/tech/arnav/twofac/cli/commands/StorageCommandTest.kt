package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.testing.test
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinApplication
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
import kotlin.test.assertTrue

class StorageCommandTest {
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
                        single<TwoFacLib> { TwoFacLib.initialise(storage = get(), passKey = "testpasskey") }
                    }
                )
            }
            twoFacLib = koinApp.koin.get()
            twoFacLib.addAccount(
                "otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
            )
            twoFacLib.addAccount(
                "otpauth://totp/Google:bob@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Google"
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testStorageDeleteWithYesDeletesAccounts() = runBlocking {
        val result = StorageDeleteCommand().test("--yes")

        assertEquals(0, result.statusCode)
        assertContains(result.output, "Deleted all accounts from storage.")
        assertTrue(twoFacLib.getAllAccounts().isEmpty())
    }

    @Test
    fun testStorageDeleteWithoutConfirmationCancelsByDefault() = runBlocking {
        val result = StorageDeleteCommand().test()

        assertEquals(0, result.statusCode)
        assertContains(result.output, "Type DELETE to confirm:")
        assertContains(result.output, "Deletion cancelled.")
        assertEquals(2, twoFacLib.getAllAccounts().size)
    }

    @Test
    fun testStorageDeleteCancellationKeepsAccounts() = runBlocking {
        val result = StorageDeleteCommand().test(stdin = "no\n")

        assertEquals(0, result.statusCode)
        assertContains(result.output, "Deletion cancelled.")
        assertEquals(2, twoFacLib.getAllAccounts().size)
    }
}
