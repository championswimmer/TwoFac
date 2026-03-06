package tech.arnav.twofac.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
        val colorScheme = if (darkTheme) {
            darkColorSchemeFromTokens(tokens)
        } else {
            lightColorSchemeFromTokens(tokens)
        }
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

private fun lightColorSchemeFromTokens(tokens: TwoFacColorTokens): ColorScheme = lightColorScheme(
    primary = tokens.brand.toComposeColor(),
    onPrimary = tokens.onBrand.toComposeColor(),
    primaryContainer = tokens.accent.toComposeColor(),
    onPrimaryContainer = tokens.onBackground.toComposeColor(),
    secondary = tokens.accent.toComposeColor(),
    onSecondary = tokens.onBrand.toComposeColor(),
    secondaryContainer = tokens.surfaceVariant.toComposeColor(),
    onSecondaryContainer = tokens.onSurfaceVariant.toComposeColor(),
    tertiary = tokens.brand.toComposeColor(),
    onTertiary = tokens.onBrand.toComposeColor(),
    tertiaryContainer = tokens.surfaceVariant.toComposeColor(),
    onTertiaryContainer = tokens.onSurfaceVariant.toComposeColor(),
    error = tokens.danger.toComposeColor(),
    onError = tokens.onDanger.toComposeColor(),
    errorContainer = tokens.danger.toComposeColor(),
    onErrorContainer = tokens.onDanger.toComposeColor(),
    background = tokens.background.toComposeColor(),
    onBackground = tokens.onBackground.toComposeColor(),
    surface = tokens.surface.toComposeColor(),
    onSurface = tokens.onSurface.toComposeColor(),
    surfaceVariant = tokens.surfaceVariant.toComposeColor(),
    onSurfaceVariant = tokens.onSurfaceVariant.toComposeColor(),
    outline = tokens.onSurfaceVariant.toComposeColor(),
    outlineVariant = tokens.surfaceVariant.toComposeColor(),
    inverseOnSurface = tokens.surface.toComposeColor(),
    inverseSurface = tokens.onSurface.toComposeColor(),
    inversePrimary = tokens.accent.toComposeColor(),
    surfaceTint = tokens.brand.toComposeColor(),
    scrim = tokens.onBackground.toComposeColor(),
    surfaceBright = tokens.surface.toComposeColor(),
    surfaceDim = tokens.background.toComposeColor(),
    surfaceContainer = tokens.surfaceVariant.toComposeColor(),
    surfaceContainerHigh = tokens.surfaceVariant.toComposeColor(),
    surfaceContainerHighest = tokens.surfaceVariant.toComposeColor(),
    surfaceContainerLow = tokens.surface.toComposeColor(),
    surfaceContainerLowest = tokens.background.toComposeColor(),
    primaryFixed = tokens.brand.toComposeColor(),
    primaryFixedDim = tokens.accent.toComposeColor(),
    onPrimaryFixed = tokens.onBrand.toComposeColor(),
    onPrimaryFixedVariant = tokens.onBackground.toComposeColor(),
    secondaryFixed = tokens.accent.toComposeColor(),
    secondaryFixedDim = tokens.brand.toComposeColor(),
    onSecondaryFixed = tokens.onBrand.toComposeColor(),
    onSecondaryFixedVariant = tokens.onBackground.toComposeColor(),
    tertiaryFixed = tokens.brand.toComposeColor(),
    tertiaryFixedDim = tokens.accent.toComposeColor(),
    onTertiaryFixed = tokens.onBrand.toComposeColor(),
    onTertiaryFixedVariant = tokens.onBackground.toComposeColor(),
)

private fun darkColorSchemeFromTokens(tokens: TwoFacColorTokens): ColorScheme = darkColorScheme(
    primary = tokens.brand.toComposeColor(),
    onPrimary = tokens.onBrand.toComposeColor(),
    primaryContainer = tokens.accent.toComposeColor(),
    onPrimaryContainer = tokens.onBackground.toComposeColor(),
    secondary = tokens.accent.toComposeColor(),
    onSecondary = tokens.onBrand.toComposeColor(),
    secondaryContainer = tokens.surfaceVariant.toComposeColor(),
    onSecondaryContainer = tokens.onSurfaceVariant.toComposeColor(),
    tertiary = tokens.brand.toComposeColor(),
    onTertiary = tokens.onBrand.toComposeColor(),
    tertiaryContainer = tokens.surfaceVariant.toComposeColor(),
    onTertiaryContainer = tokens.onSurfaceVariant.toComposeColor(),
    error = tokens.danger.toComposeColor(),
    onError = tokens.onDanger.toComposeColor(),
    errorContainer = tokens.danger.toComposeColor(),
    onErrorContainer = tokens.onDanger.toComposeColor(),
    background = tokens.background.toComposeColor(),
    onBackground = tokens.onBackground.toComposeColor(),
    surface = tokens.surface.toComposeColor(),
    onSurface = tokens.onSurface.toComposeColor(),
    surfaceVariant = tokens.surfaceVariant.toComposeColor(),
    onSurfaceVariant = tokens.onSurfaceVariant.toComposeColor(),
    outline = tokens.onSurfaceVariant.toComposeColor(),
    outlineVariant = tokens.surfaceVariant.toComposeColor(),
    inverseOnSurface = tokens.surface.toComposeColor(),
    inverseSurface = tokens.onSurface.toComposeColor(),
    inversePrimary = tokens.accent.toComposeColor(),
    surfaceTint = tokens.brand.toComposeColor(),
    scrim = tokens.onBackground.toComposeColor(),
    surfaceBright = tokens.surface.toComposeColor(),
    surfaceDim = tokens.background.toComposeColor(),
    surfaceContainer = tokens.surfaceVariant.toComposeColor(),
    surfaceContainerHigh = tokens.surfaceVariant.toComposeColor(),
    surfaceContainerHighest = tokens.surfaceVariant.toComposeColor(),
    surfaceContainerLow = tokens.surface.toComposeColor(),
    surfaceContainerLowest = tokens.background.toComposeColor(),
    primaryFixed = tokens.brand.toComposeColor(),
    primaryFixedDim = tokens.accent.toComposeColor(),
    onPrimaryFixed = tokens.onBrand.toComposeColor(),
    onPrimaryFixedVariant = tokens.onBackground.toComposeColor(),
    secondaryFixed = tokens.accent.toComposeColor(),
    secondaryFixedDim = tokens.brand.toComposeColor(),
    onSecondaryFixed = tokens.onBrand.toComposeColor(),
    onSecondaryFixedVariant = tokens.onBackground.toComposeColor(),
    tertiaryFixed = tokens.brand.toComposeColor(),
    tertiaryFixedDim = tokens.accent.toComposeColor(),
    onTertiaryFixed = tokens.onBrand.toComposeColor(),
    onTertiaryFixedVariant = tokens.onBackground.toComposeColor(),
)

private fun ThemeColor.toComposeColor(): Color = Color(argb.toInt())
