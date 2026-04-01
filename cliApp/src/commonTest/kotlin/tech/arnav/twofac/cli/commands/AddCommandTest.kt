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

class AddCommandTest {

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
    fun testAddCommandWithUri() = runBlocking {
        val uri = "otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
        val result = AddCommand().test("--passkey=test $uri")

        assertEquals(0, result.statusCode)
        assertContains(result.output, "Account added successfully")
        assertEquals(1, twoFacLib.getAllAccounts().size)
    }
}
