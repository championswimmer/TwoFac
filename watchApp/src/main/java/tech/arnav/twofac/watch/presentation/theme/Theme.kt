package tech.arnav.twofac.watch.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import tech.arnav.twofac.lib.theme.ThemeColor
import tech.arnav.twofac.lib.theme.TwoFacColorTokens
import tech.arnav.twofac.lib.theme.TwoFacThemeTokens

private val LocalTwoFacTokens = staticCompositionLocalOf { TwoFacThemeTokens.dark }

object TwofacTheme {
    val tokens: TwoFacColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalTwoFacTokens.current
}

@Composable
fun TwofacTheme(
    content: @Composable () -> Unit
) {
    val tokens = TwoFacThemeTokens.dark
    val colors = Colors(
        primary = tokens.brand.toComposeColor(),
        primaryVariant = tokens.accent.toComposeColor(),
        secondary = tokens.accent.toComposeColor(),
        secondaryVariant = tokens.surfaceVariant.toComposeColor(),
        background = Color.Black,
        surface = Color.Black,
        error = tokens.danger.toComposeColor(),
        onPrimary = tokens.onBrand.toComposeColor(),
        onSecondary = tokens.onSurface.toComposeColor(),
        onBackground = tokens.onBackground.toComposeColor(),
        onSurface = tokens.onSurface.toComposeColor(),
        onSurfaceVariant = tokens.onSurfaceVariant.toComposeColor(),
        onError = tokens.onDanger.toComposeColor(),
    )
    CompositionLocalProvider(LocalTwoFacTokens provides tokens) {
        MaterialTheme(
            colors = colors,
            content = content,
        )
    }
}

internal fun ThemeColor.toComposeColor(): Color = Color(argb.toInt())
