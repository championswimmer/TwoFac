package tech.arnav.twofac.lib.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AccountColorTagTest {

    @Test
    fun canonicalPaletteContainsExpectedColorsInStableOrder() {
        assertEquals(
            listOf(
                AccountColorTag.RED,
                AccountColorTag.ORANGE,
                AccountColorTag.YELLOW,
                AccountColorTag.GREEN,
                AccountColorTag.TEAL,
                AccountColorTag.BLUE,
                AccountColorTag.PURPLE,
                AccountColorTag.BROWN,
            ),
            AccountColorTag.entries.toList(),
        )
    }

    @Test
    fun paletteDoesNotUseBlackOrWhiteAsSelectableShades() {
        val forbidden = setOf(
            ThemeColor.fromArgbHex("#FF000000"),
            ThemeColor.fromArgbHex("#FFFFFFFF"),
        )

        AccountColorTag.entries.forEach { tag ->
            assertNotEquals(tag.lightColor, tag.darkColor)
            assertEquals(0xFF, tag.lightColor.alpha)
            assertEquals(0xFF, tag.darkColor.alpha)
            assertEquals(false, tag.lightColor in forbidden, "${tag.name} light shade must not be black/white")
            assertEquals(false, tag.darkColor in forbidden, "${tag.name} dark shade must not be black/white")
        }
    }
}
