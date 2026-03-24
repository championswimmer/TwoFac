package tech.arnav.twofac.components.otp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OTPCardTest {
    @Test
    fun testHasTotpIntervalChangedReturnsFalseWithinSameWindow() {
        assertFalse(hasTotpIntervalChanged(previousEpochSeconds = 100L, currentEpochSeconds = 119L, intervalSeconds = 30L))
    }

    @Test
    fun testHasTotpIntervalChangedReturnsTrueAcrossWindowBoundary() {
        assertTrue(hasTotpIntervalChanged(previousEpochSeconds = 119L, currentEpochSeconds = 120L, intervalSeconds = 30L))
    }

    @Test
    fun testHasTotpIntervalChangedReturnsTrueWhenBoundarySecondIsSkipped() {
        assertTrue(hasTotpIntervalChanged(previousEpochSeconds = 149L, currentEpochSeconds = 151L, intervalSeconds = 30L))
    }

    @Test
    fun testOtpCopiedSnackbarMessageIncludesClipboardIcon() {
        assertEquals("📋 OTP copied", OTP_COPIED_SNACKBAR_MESSAGE)
    }
}
