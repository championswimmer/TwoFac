package tech.arnav.twofac.cli.commands

import com.github.ajalt.clikt.testing.test
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class InfoCommandTest {

    val infoCommand = InfoCommand()

    @Test
    fun testInfoCommand() {
        val result = infoCommand.test()

        assertTrue(result.statusCode == 0)
        assertContains(result.output, "TwoFac CLI")
        assertContains(result.output, "Platform")
        assertContains(result.output, "Library")
        assertContains(result.output, "Data Directory")

    }
}
