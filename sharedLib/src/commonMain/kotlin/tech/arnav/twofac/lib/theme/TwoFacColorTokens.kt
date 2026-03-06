package tech.arnav.twofac.lib.theme

import tech.arnav.twofac.lib.PublicApi

@PublicApi
data class TwoFacColorTokens(
    val brand: ThemeColor,
    val onBrand: ThemeColor,
    val background: ThemeColor,
    val onBackground: ThemeColor,
    val surface: ThemeColor,
    val onSurface: ThemeColor,
    val surfaceVariant: ThemeColor,
    val onSurfaceVariant: ThemeColor,
    val accent: ThemeColor,
    val danger: ThemeColor,
    val onDanger: ThemeColor,
    val timer: TimerColorSemantics,
    val timerTrack: ThemeColor,
)

@PublicApi
object TwoFacThemeTokens {
    val light = TwoFacColorTokens(
        brand = ThemeColor.fromArgbHex("#FF2C7CBF"),
        onBrand = ThemeColor.fromArgbHex("#FFFDFaf8"),
        background = ThemeColor.fromArgbHex("#FFFDFaf8"),
        onBackground = ThemeColor.fromArgbHex("#FF171E1E"),
        surface = ThemeColor.fromArgbHex("#FFFFFFFF"),
        onSurface = ThemeColor.fromArgbHex("#FF171E1E"),
        surfaceVariant = ThemeColor.fromArgbHex("#FFE5EDF4"),
        onSurfaceVariant = ThemeColor.fromArgbHex("#FF2F3B40"),
        accent = ThemeColor.fromArgbHex("#FF3E8ECC"),
        danger = ThemeColor.fromArgbHex("#FFF44336"),
        onDanger = ThemeColor.fromArgbHex("#FFFDFaf8"),
        timer = TimerColorSemantics(
            healthy = ThemeColor.fromArgbHex("#FF4CAF50"),
            warning = ThemeColor.fromArgbHex("#FFFF9800"),
            critical = ThemeColor.fromArgbHex("#FFF44336"),
        ),
        timerTrack = ThemeColor.fromArgbHex("#332C7CBF"),
    )

    val dark = TwoFacColorTokens(
        brand = ThemeColor.fromArgbHex("#FF5EA7E5"),
        onBrand = ThemeColor.fromArgbHex("#FF171E1E"),
        background = ThemeColor.fromArgbHex("#FF171E1E"),
        onBackground = ThemeColor.fromArgbHex("#FFFDFaf8"),
        surface = ThemeColor.fromArgbHex("#FF1F2728"),
        onSurface = ThemeColor.fromArgbHex("#FFFDFaf8"),
        surfaceVariant = ThemeColor.fromArgbHex("#FF2B3638"),
        onSurfaceVariant = ThemeColor.fromArgbHex("#FFC5D0D0"),
        accent = ThemeColor.fromArgbHex("#FF8EC6F3"),
        danger = ThemeColor.fromArgbHex("#FFFF6B5E"),
        onDanger = ThemeColor.fromArgbHex("#FF171E1E"),
        timer = TimerColorSemantics(
            healthy = ThemeColor.fromArgbHex("#FF66BB6A"),
            warning = ThemeColor.fromArgbHex("#FFFFB74D"),
            critical = ThemeColor.fromArgbHex("#FFEF5350"),
        ),
        timerTrack = ThemeColor.fromArgbHex("#66FDFaf8"),
    )
}
