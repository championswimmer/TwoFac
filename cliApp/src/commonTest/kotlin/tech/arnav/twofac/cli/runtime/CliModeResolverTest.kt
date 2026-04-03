package tech.arnav.twofac.cli.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class CliModeResolverTest {

    @Test
    fun testBothInteractiveResolvesInteractiveMode() {
        val mode = DefaultCliModeResolver.resolve(inputInteractive = true, outputInteractive = true)
        assertEquals(CliMode.INTERACTIVE, mode)
    }

    @Test
    fun testInputNonInteractiveResolvesNonInteractiveMode() {
        val mode = DefaultCliModeResolver.resolve(inputInteractive = false, outputInteractive = true)
        assertEquals(CliMode.NON_INTERACTIVE, mode)
    }

    @Test
    fun testOutputNonInteractiveResolvesNonInteractiveMode() {
        val mode = DefaultCliModeResolver.resolve(inputInteractive = true, outputInteractive = false)
        assertEquals(CliMode.NON_INTERACTIVE, mode)
    }
}
