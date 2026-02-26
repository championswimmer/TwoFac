package tech.arnav.twofac.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OTPCardTest {
    @Test
    fun testHasTotpIntervalChangedReturnsFalseWithinSameWindow() {
        assertFalse(hasTotpIntervalChanged(previousEpochSeconds = 100L, currentEpochSeconds = 129L, intervalSeconds = 30L))
    }

    @Test
    fun testHasTotpIntervalChangedReturnsTrueAcrossWindowBoundary() {
        assertTrue(hasTotpIntervalChanged(previousEpochSeconds = 129L, currentEpochSeconds = 130L, intervalSeconds = 30L))
    }

    @Test
    fun testHasTotpIntervalChangedReturnsTrueWhenBoundarySecondIsSkipped() {
        assertTrue(hasTotpIntervalChanged(previousEpochSeconds = 149L, currentEpochSeconds = 151L, intervalSeconds = 30L))
    }
}
