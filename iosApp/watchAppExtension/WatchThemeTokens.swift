import SwiftUI
import TwoFacKit

enum WatchThemeTokens {
    static func tokens() -> TwoFacColorTokens {
        TwoFacThemeTokens.shared.dark
    }

    static func timerColor(for remainingProgress: Double) -> Color {
        let state = timerState(for: remainingProgress)
        let timer = tokens().timer
        let themeColor: ThemeColor
        switch state {
        case .healthy:
            themeColor = timer.healthy
        case .warning:
            themeColor = timer.warning
        case .critical:
            themeColor = timer.critical
        default:
            themeColor = timer.critical
        }
        return Color(themeColor: themeColor)
    }

    static func timerTrackColor() -> Color {
        Color(themeColor: tokens().timerTrack)
    }

    static func errorColor() -> Color {
        Color(themeColor: tokens().danger)
    }

    static func backgroundColor() -> Color {
        Color(themeColor: tokens().background)
    }

    private static func timerState(for remainingProgress: Double) -> TimerState {
        let clamped = min(max(remainingProgress, 0.0), 1.0)
        return TimerColorSemanticsKt.timerStateByRemainingProgress(remainingProgress: Float(clamped))
    }
}

extension Color {
    init(themeColor: ThemeColor) {
        self.init(argb: themeColor.argb)
    }

    init(argb: Int64) {
        let unsignedArgb = UInt64(bitPattern: argb)
        let alpha = Double((unsignedArgb >> 24) & 0xFF) / 255.0
        let red = Double((unsignedArgb >> 16) & 0xFF) / 255.0
        let green = Double((unsignedArgb >> 8) & 0xFF) / 255.0
        let blue = Double(unsignedArgb & 0xFF) / 255.0
        self = Color(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}
