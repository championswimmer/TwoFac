package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.testing.test
import kotlinx.coroutines.runBlocking
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

class RemoveCommandTest {

    private lateinit var twoFacLib: TwoFacLib

    @BeforeTest
    fun setup() {
        stopKoin()
        val storage = MemoryStorage()
        twoFacLib = TwoFacLib.initialise(storage = storage, passKey = "test")
        startKoin {
            modules(
                module {
                    single<Storage> { storage }
                    single<TwoFacLib> { twoFacLib }
                }
            )
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testRemoveCommandWithValidAccountId() = runBlocking {
        twoFacLib.addAccount("otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub")
        val accountId = twoFacLib.getAllAccounts().single().accountID

        val result = RemoveCommand().test("--passkey=test $accountId")

        assertEquals(0, result.statusCode)
        assertContains(result.output, "Account removed successfully")
        assertEquals(0, twoFacLib.getAllAccounts().size)
    }

    @Test
    fun testRemoveCommandWithUnknownAccountIdFails() {
        val result = RemoveCommand().test("--passkey=test 75f8eeec-cf47-4d3d-98c6-4f880f7702cc")

        assertEquals(1, result.statusCode)
        assertContains(result.output, "Account not found")
    }
}
