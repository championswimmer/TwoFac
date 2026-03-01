package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.testing.test
import org.koin.core.context.startKoin
import tech.arnav.twofac.cli.di.appModule
import tech.arnav.twofac.cli.di.storageModule
import tech.arnav.twofac.cli.di.testStorageModule
import kotlin.test.BeforeClass
import kotlin.test.Test
import kotlin.test.assertContains

class DisplayCommandTest {

    companion object {
        @BeforeClass
        fun setupKoin() {
            startKoin {
                modules(appModule, testStorageModule)
            }
        }
    }

    @Test
    fun testDisplayCommandWithoutPasskey() {
        val result = DisplayCommand().test()

        assertContains(result.output, "missing option --passkey")

    }

    @Test
    fun testDisplayCommandWithPasskeyNonInteractive() {
        val result = DisplayCommand().test(
            "--passkey=testpasskey",
            outputInteractive = false,
            inputInteractive = false,
        )

        assertContains(result.output, "Account")
        assertContains(result.output, "OTP")
        assertContains(result.output, "Validity")
    }
}
