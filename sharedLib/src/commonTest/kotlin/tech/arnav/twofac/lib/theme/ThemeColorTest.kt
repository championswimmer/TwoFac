package tech.arnav.twofac.lib.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ThemeColorTest {
    @Test
    fun parseRgbHexAppliesOpaqueAlpha() {
        val color = ThemeColor.fromArgbHex("#2C7CBF")
        assertEquals(0xFF2C7CBFL, color.argb)
    }

    @Test
    fun parseArgbHexUsesProvidedAlpha() {
        val color = ThemeColor.fromArgbHex("662C7CBF")
        assertEquals(0x662C7CBFL, color.argb)
    }

    @Test
    fun invalidHexIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            ThemeColor.fromArgbHex("xyz")
        }
    }
}
