package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.testing.test
import kotlin.test.Test
import kotlin.test.assertContains

class DisplayCommandTest {

    @Test
    fun testDisplayCommandWithoutPasskey() {
        val result = DisplayCommand().test()

        assertContains(result.output, "Enter passkey")

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
