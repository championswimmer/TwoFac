package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.testing.test
import org.koin.core.context.startKoin
import tech.arnav.twofac.cli.di.appModule
import tech.arnav.twofac.cli.di.storageModule
import kotlin.test.BeforeClass
import kotlin.test.Test
import kotlin.test.assertContains

class DisplayCommandTest {

    companion object {
        @BeforeClass
        fun setupKoin() {
            startKoin {
                modules(appModule, storageModule)
            }
        }
    }

    @Test
    fun testDisplayCommandWithoutPasskey() {
        val result = DisplayCommand().test()

        assertContains(result.output, "Passkey cannot be blank")

    }

    @Test
    fun testDisplayCommandWithPasskey() {
        val result = DisplayCommand().test("--passkey=testpasskey")

        assertContains(result.output, "display command executed with passkey: tes...")
    }

    @Test
    fun testDisplayCommandWithPasskeyInPrompt() {
        val result = DisplayCommand().test(stdin = "testpasskey\n")

        assertContains(result.output, "Enter passkey")
        assertContains(result.output, "display command executed with passkey: tes...")
    }
}
