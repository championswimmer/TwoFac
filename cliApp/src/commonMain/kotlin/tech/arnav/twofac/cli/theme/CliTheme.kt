package tech.arnav.twofac.cli.theme

import com.github.ajalt.colormath.model.Ansi256
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import tech.arnav.twofac.lib.theme.ThemeColor
import tech.arnav.twofac.lib.theme.TimerState
import tech.arnav.twofac.lib.theme.TwoFacThemeTokens

data class CliThemeStyles(
    val title: TextStyle,
    val header: TextStyle,
    val label: TextStyle,
    val key: TextStyle,
    val otp: TextStyle,
    val footer: TextStyle,
    val timerHealthy: TextStyle,
    val timerWarning: TextStyle,
    val timerCritical: TextStyle,
    val timerTrack: TextStyle,
    val danger: TextStyle,
) {
    fun timer(state: TimerState): TextStyle = when (state) {
        TimerState.Healthy -> timerHealthy
        TimerState.Warning -> timerWarning
        TimerState.Critical -> timerCritical
    }
}

object CliTheme {
    private val tokens = TwoFacThemeTokens.dark

    fun styles(terminal: Terminal): CliThemeStyles {
        return when (terminal.terminalInfo.ansiLevel) {
            AnsiLevel.TRUECOLOR -> trueColorStyles()
            AnsiLevel.ANSI256 -> ansi256Styles()
            AnsiLevel.ANSI16 -> ansi16Styles()
            AnsiLevel.NONE -> noColorStyles()
        }
    }

    private fun trueColorStyles() = CliThemeStyles(
        title = TextStyles.bold + trueColor(tokens.brand),
        header = TextStyles.bold + trueColor(tokens.brand),
        label = TextStyles.dim + trueColor(tokens.onSurfaceVariant),
        key = TextStyles.bold + trueColor(tokens.accent),
        otp = TextStyles.bold + trueColor(tokens.brand),
        footer = TextStyles.dim + trueColor(tokens.onSurfaceVariant),
        timerHealthy = trueColor(tokens.timer.healthy),
        timerWarning = trueColor(tokens.timer.warning),
        timerCritical = trueColor(tokens.timer.critical),
        timerTrack = TextStyles.dim + trueColor(tokens.timerTrack),
        danger = TextStyles.bold + trueColor(tokens.danger),
    )

    private fun ansi256Styles() = CliThemeStyles(
        title = TextStyles.bold + ansi256(39),  // xterm-256 #00afff (dodger blue)
        header = TextStyles.bold + ansi256(33), // xterm-256 #0087ff (azure blue)
        label = TextStyles.dim + ansi256(250),  // xterm-256 #bcbcbc (light gray)
        key = TextStyles.bold + ansi256(45), // xterm-256 #00d7ff (bright cyan)
        otp = ansi256(33), // xterm-256 #87afff (brand blue equivalent)
        footer = TextStyles.dim + ansi256(250), // xterm-256 #bcbcbc (light gray)
        timerHealthy = ansi256(71), // xterm-256 #5faf5f (medium green)
        timerWarning = ansi256(214), // xterm-256 #ffaf00 (amber/orange)
        timerCritical = ansi256(203), // xterm-256 #ff5f5f (salmon red)
        timerTrack = TextStyles.dim + ansi256(242), // xterm-256 #6c6c6c (mid gray)
        danger = TextStyles.bold + ansi256(203), // xterm-256 #ff5f5f (salmon red)
    )

    private fun ansi16Styles() = CliThemeStyles(
        title = TextStyles.bold + TextColors.blue, // ANSI 34 blue, bold
        header = TextStyles.bold + TextColors.blue, // ANSI 34 blue
        label = TextStyles.dim + TextColors.white, // ANSI dim white
        key = TextStyles.bold + TextColors.cyan, // ANSI 36 cyan
        otp = TextStyles.bold + TextColors.blue,
        footer = TextStyles.dim + TextColors.white, // ANSI dim white
        timerHealthy = TextColors.green, // ANSI 32 green
        timerWarning = TextColors.yellow, // ANSI 33 yellow
        timerCritical = TextColors.red, // ANSI 31 red
        timerTrack = TextStyles.dim + TextColors.blue, // ANSI 34 blue (dimmed track)
        danger = TextStyles.bold + TextColors.red, // ANSI 31 red
    )

    private fun noColorStyles() = CliThemeStyles(
        title = TextStyles.bold.style,
        header = TextStyles.bold.style,
        label = TextStyles.dim.style,
        key = TextStyles.bold.style,
        otp = TextStyles.bold.style,
        footer = TextStyles.dim.style,
        timerHealthy = TextStyle(),
        timerWarning = TextStyle(),
        timerCritical = TextStyle(),
        timerTrack = TextStyles.dim.style,
        danger = TextStyles.bold.style,
    )

    private fun ansi256(index: Int): TextStyle = TextColors.color(Ansi256(index), AnsiLevel.ANSI256)

    private fun trueColor(color: ThemeColor): TextStyle =
        TextColors.rgb(color.toRgbHex(), AnsiLevel.TRUECOLOR)
}

private fun ThemeColor.toRgbHex(): String {
    return "#${red.toHexByte()}${green.toHexByte()}${blue.toHexByte()}"
}

private fun Int.toHexByte(): String = toString(16).padStart(2, '0').uppercase()
