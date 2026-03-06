package tech.arnav.twofac.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import tech.arnav.twofac.lib.theme.ThemeColor
import tech.arnav.twofac.lib.theme.TwoFacColorTokens
import tech.arnav.twofac.lib.theme.TwoFacThemeTokens

@Immutable
object TwoFacTheme {
    val extendedColors: TwoFacExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTwoFacExtendedColors.current

    @Composable
    operator fun invoke(
        darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit,
    ) {
        val tokens = if (darkTheme) TwoFacThemeTokens.dark else TwoFacThemeTokens.light
        val colorScheme = colorSchemeFromTokens(tokens)
        val extendedColors = TwoFacExtendedColors(
            timerHealthy = tokens.timer.healthy.toComposeColor(),
            timerWarning = tokens.timer.warning.toComposeColor(),
            timerCritical = tokens.timer.critical.toComposeColor(),
            timerTrack = tokens.timerTrack.toComposeColor(),
        )

        CompositionLocalProvider(LocalTwoFacExtendedColors provides extendedColors) {
            MaterialTheme(
                colorScheme = colorScheme,
                content = content,
            )
        }
    }
}

private fun colorSchemeFromTokens(tokens: TwoFacColorTokens): ColorScheme {
    val brand = tokens.brand.toComposeColor()
    val onBrand = tokens.onBrand.toComposeColor()
    val accent = tokens.accent.toComposeColor()
    val background = tokens.background.toComposeColor()
    val onBackground = tokens.onBackground.toComposeColor()
    val surface = tokens.surface.toComposeColor()
    val onSurface = tokens.onSurface.toComposeColor()
    val surfaceVariant = tokens.surfaceVariant.toComposeColor()
    val onSurfaceVariant = tokens.onSurfaceVariant.toComposeColor()
    val danger = tokens.danger.toComposeColor()
    val onDanger = tokens.onDanger.toComposeColor()

    return ColorScheme(
        primary = brand,
        onPrimary = onBrand,
        primaryContainer = accent,
        onPrimaryContainer = onBackground,
        inversePrimary = accent,
        secondary = accent,
        onSecondary = onBrand,
        secondaryContainer = surfaceVariant,
        onSecondaryContainer = onSurfaceVariant,
        tertiary = brand,
        onTertiary = onBrand,
        tertiaryContainer = surfaceVariant,
        onTertiaryContainer = onSurfaceVariant,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = brand,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        error = danger,
        onError = onDanger,
        errorContainer = danger,
        onErrorContainer = onDanger,
        outline = onSurfaceVariant,
        outlineVariant = surfaceVariant,
        scrim = onBackground,
        surfaceBright = surface,
        surfaceDim = background,
        surfaceContainer = surfaceVariant,
        surfaceContainerHigh = surfaceVariant,
        surfaceContainerHighest = surfaceVariant,
        surfaceContainerLow = surface,
        surfaceContainerLowest = background,
        primaryFixed = brand,
        primaryFixedDim = accent,
        onPrimaryFixed = onBrand,
        onPrimaryFixedVariant = onBackground,
        secondaryFixed = accent,
        secondaryFixedDim = brand,
        onSecondaryFixed = onBrand,
        onSecondaryFixedVariant = onBackground,
        tertiaryFixed = brand,
        tertiaryFixedDim = accent,
        onTertiaryFixed = onBrand,
        onTertiaryFixedVariant = onBackground,
    )
}

private fun ThemeColor.toComposeColor(): Color = Color(argb.toInt())
