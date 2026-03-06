package tech.arnav.twofac.lib.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TwoFacThemeTokensTest {
    @Test
    fun logoPaletteAnchorsArePreserved() {
        assertEquals(0xFF2C7CBFL, TwoFacThemeTokens.light.brand.argb)
        assertEquals(0xFFFDFAF8L, TwoFacThemeTokens.light.background.argb)
        assertEquals(0xFF171E1EL, TwoFacThemeTokens.light.onBackground.argb)
    }

    @Test
    fun allThemeTokenColorsAreInRange() {
        assertTokensInRange(TwoFacThemeTokens.light)
        assertTokensInRange(TwoFacThemeTokens.dark)
    }

    private fun assertTokensInRange(tokens: TwoFacColorTokens) {
        val colors = listOf(
            tokens.brand,
            tokens.onBrand,
            tokens.background,
            tokens.onBackground,
            tokens.surface,
            tokens.onSurface,
            tokens.surfaceVariant,
            tokens.onSurfaceVariant,
            tokens.accent,
            tokens.danger,
            tokens.onDanger,
            tokens.timer.healthy,
            tokens.timer.warning,
            tokens.timer.critical,
            tokens.timerTrack,
        )
        colors.forEach { color ->
            assertNotNull(color)
            assertTrue(color.argb in 0x00000000L..0xFFFFFFFFL)
        }
    }
}
