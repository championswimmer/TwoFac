package tech.arnav.twofac.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class TwoFacExtendedColors(
    val timerHealthy: Color,
    val timerWarning: Color,
    val timerCritical: Color,
    val timerTrack: Color? = null,
)

internal val LocalTwoFacExtendedColors = staticCompositionLocalOf {
    TwoFacExtendedColors(
        timerHealthy = Color.Unspecified,
        timerWarning = Color.Unspecified,
        timerCritical = Color.Unspecified,
        timerTrack = null,
    )
}
