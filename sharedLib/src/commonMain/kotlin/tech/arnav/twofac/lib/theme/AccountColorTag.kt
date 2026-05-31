package tech.arnav.twofac.lib.theme

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.PublicApi

/**
 * Canonical optional color tags users can assign to accounts.
 *
 * The enum value, not the raw ARGB shade, is persisted so the palette can be
 * refined in future releases without rewriting account stores.
 */
@PublicApi
@Serializable
enum class AccountColorTag(
    val displayName: String,
    val lightColor: ThemeColor,
    val darkColor: ThemeColor,
) {
    @SerialName("red")
    RED(
        displayName = "Red",
        lightColor = ThemeColor.fromArgbHex("#FFFFD6D6"),
        darkColor = ThemeColor.fromArgbHex("#FF6F2C2C"),
    ),

    @SerialName("orange")
    ORANGE(
        displayName = "Orange",
        lightColor = ThemeColor.fromArgbHex("#FFFFE0B8"),
        darkColor = ThemeColor.fromArgbHex("#FF754C1F"),
    ),

    @SerialName("yellow")
    YELLOW(
        displayName = "Yellow",
        lightColor = ThemeColor.fromArgbHex("#FFFFF2B8"),
        darkColor = ThemeColor.fromArgbHex("#FF6A5A1E"),
    ),

    @SerialName("green")
    GREEN(
        displayName = "Green",
        lightColor = ThemeColor.fromArgbHex("#FFD8EED6"),
        darkColor = ThemeColor.fromArgbHex("#FF2F6235"),
    ),

    @SerialName("teal")
    TEAL(
        displayName = "Teal",
        lightColor = ThemeColor.fromArgbHex("#FFD4EEEE"),
        darkColor = ThemeColor.fromArgbHex("#FF256463"),
    ),

    @SerialName("blue")
    BLUE(
        displayName = "Blue",
        lightColor = ThemeColor.fromArgbHex("#FFD6E6FA"),
        darkColor = ThemeColor.fromArgbHex("#FF2D5688"),
    ),

    @SerialName("purple")
    PURPLE(
        displayName = "Purple",
        lightColor = ThemeColor.fromArgbHex("#FFE6DAF4"),
        darkColor = ThemeColor.fromArgbHex("#FF5B3F83"),
    ),

    @SerialName("brown")
    BROWN(
        displayName = "Brown",
        lightColor = ThemeColor.fromArgbHex("#FFE8D8C5"),
        darkColor = ThemeColor.fromArgbHex("#FF634A33"),
    );

    fun color(isDarkTheme: Boolean): ThemeColor = if (isDarkTheme) darkColor else lightColor
}
