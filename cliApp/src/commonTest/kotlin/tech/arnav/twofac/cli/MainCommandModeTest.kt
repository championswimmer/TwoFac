package tech.arnav.twofac.cli

import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.testing.test
import tech.arnav.twofac.cli.commands.InfoCommand
import tech.arnav.twofac.cli.runtime.CliMode
import tech.arnav.twofac.cli.runtime.CliModeResolver
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainCommandModeTest {

    @Test
    fun testNoSubcommandInteractiveRunsInteractiveMode() {
        var interactiveModeLaunched = false

        val result = MainCommand(
            modeResolver = CliModeResolver { _, _ -> CliMode.INTERACTIVE },
            runInteractiveMode = { interactiveModeLaunched = true },
        ).test()

        assertEquals(0, result.statusCode)
        assertTrue(interactiveModeLaunched)
    }

    @Test
    fun testNoSubcommandNonInteractivePrintsHelp() {
        var interactiveModeLaunched = false

        val result = MainCommand(
            modeResolver = CliModeResolver { _, _ -> CliMode.NON_INTERACTIVE },
            runInteractiveMode = { interactiveModeLaunched = true },
        ).test()

        assertEquals(0, result.statusCode)
        assertContains(result.output, "Usage:")
        assertFalse(interactiveModeLaunched)
    }

    @Test
    fun testWithSubcommandSkipsRootModeHandling() {
        var interactiveModeLaunched = false

        val root = MainCommand(
            modeResolver = CliModeResolver { _, _ -> CliMode.INTERACTIVE },
            runInteractiveMode = { interactiveModeLaunched = true },
        ).subcommands(InfoCommand())

        val result = root.test("info")

        assertEquals(0, result.statusCode)
        assertFalse(interactiveModeLaunched)
    }
}
