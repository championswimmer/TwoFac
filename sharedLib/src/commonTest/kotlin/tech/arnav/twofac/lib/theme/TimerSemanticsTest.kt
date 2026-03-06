package tech.arnav.twofac.lib.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TimerSemanticsTest {
    @Test
    fun timerStateHealthyAboveFiftyPercentRemaining() {
        assertEquals(TimerState.Healthy, timerStateByRemainingProgress(0.5001f))
    }

    @Test
    fun timerStateWarningBetweenTwentyFiveAndFiftyPercentRemaining() {
        assertEquals(TimerState.Warning, timerStateByRemainingProgress(0.5f))
        assertEquals(TimerState.Warning, timerStateByRemainingProgress(0.2501f))
    }

    @Test
    fun timerStateCriticalAtOrBelowTwentyFivePercentRemaining() {
        assertEquals(TimerState.Critical, timerStateByRemainingProgress(0.25f))
        assertEquals(TimerState.Critical, timerStateByRemainingProgress(0f))
    }

    @Test
    fun elapsedProgressMapsThroughRemainingThresholds() {
        assertEquals(TimerState.Healthy, timerStateByElapsedProgress(0.1f))
        assertEquals(TimerState.Warning, timerStateByElapsedProgress(0.6f))
        assertEquals(TimerState.Critical, timerStateByElapsedProgress(0.9f))
    }

    @Test
    fun outOfRangeProgressValuesAreRejected() {
        assertFailsWith<IllegalArgumentException> { timerStateByRemainingProgress(-0.01f) }
        assertFailsWith<IllegalArgumentException> { timerStateByElapsedProgress(1.01f) }
    }
}
