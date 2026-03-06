package tech.arnav.twofac.lib.theme

import tech.arnav.twofac.lib.PublicApi

@PublicApi
data class TimerColorSemantics(
    val healthy: ThemeColor,
    val warning: ThemeColor,
    val critical: ThemeColor,
)

@PublicApi
enum class TimerState {
    Healthy,
    Warning,
    Critical,
}

@PublicApi
object TimerSemantics {
    const val HEALTHY_THRESHOLD = 0.50f
    const val WARNING_THRESHOLD = 0.25f
}

@PublicApi
fun timerStateByRemainingProgress(remainingProgress: Float): TimerState {
    require(remainingProgress in 0f..1f) {
        "remainingProgress must be between 0 and 1 inclusive, got $remainingProgress"
    }
    return when {
        remainingProgress > TimerSemantics.HEALTHY_THRESHOLD -> TimerState.Healthy
        remainingProgress > TimerSemantics.WARNING_THRESHOLD -> TimerState.Warning
        else -> TimerState.Critical
    }
}

@PublicApi
fun timerStateByElapsedProgress(elapsedProgress: Float): TimerState {
    require(elapsedProgress in 0f..1f) {
        "elapsedProgress must be between 0 and 1 inclusive, got $elapsedProgress"
    }
    return timerStateByRemainingProgress(1f - elapsedProgress)
}

@PublicApi
fun TimerColorSemantics.colorForState(state: TimerState): ThemeColor {
    return when (state) {
        TimerState.Healthy -> healthy
        TimerState.Warning -> warning
        TimerState.Critical -> critical
    }
}
