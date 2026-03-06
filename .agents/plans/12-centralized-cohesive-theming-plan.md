---
name: Centralized Cohesive Theming Across App Surfaces
status: Planned
progress:
  - "[ ] Phase 0 - Baseline audit, palette extraction, and semantic token contract"
  - "[ ] Phase 1 - Add sharedLib color-token source of truth (colors only)"
  - "[ ] Phase 2 - Build composeApp Material3 theme foundation from shared tokens"
  - "[ ] Phase 3 - Centralize timer bar red/yellow/green semantics across Compose + watchApp + iOS watch"
  - "[ ] Phase 4 - Adopt shared tokens in watchApp (Wear Compose) and iosApp watch extension (SwiftUI)"
  - "[ ] Phase 5 - Adopt shared tokens in cliApp via Mordant color mapping"
  - "[ ] Phase 6 - Validation matrix, accessibility checks, and rollout sequence"
---

# Centralized Cohesive Theming Across App Surfaces

## Goal
Create a centrally controlled, cohesive theme system across the full product surface:
- `composeApp` uses Material3 with explicit, reusable color roles.
- `sharedLib` owns common color tokens (colors only), consumable by all frontends.
- `watchApp`, `iosApp/watchAppExtension`, and `cliApp` consume shared color semantics from `sharedLib`.
- Timer/progress urgency colors are standardized via shared red/yellow/green semantic roles.

Default branding should align with the logo palette in `docs/twofac-logo.svg`.

---

## Current State (Audited)

### Branding source
From `docs/twofac-logo.svg`:
- Off-white: `#fdfaf8`
- Near-black: `#171e1e`
- Brand blue: `#2c7cbf`

### Current theming fragmentation
1. **composeApp**
   - Root uses plain `MaterialTheme { ... }` with defaults in `composeApp/src/commonMain/kotlin/tech/arnav/twofac/App.kt`.
   - No dedicated theme file and no explicit light/dark `ColorScheme`.
   - Timer bar in `composeApp/.../components/OTPCard.kt` uses `primary/secondary/error` threshold logic.

2. **watchApp (Android Wear)**
   - `watchApp/.../presentation/theme/Theme.kt` is a minimal wrapper around default `MaterialTheme`.
   - `watchApp/.../ui/OtpAccountScreen.kt` hardcodes timer arc colors:
     - green `0xFF4CAF50`
     - amber `0xFFFF9800`
     - red `0xFFF44336`

3. **iosApp watch extension (SwiftUI)**
   - `iosApp/watchAppExtension/WatchExtensionContentView.swift` hardcodes `.green/.yellow/.red` countdown semantics and `.red` error text.

4. **cliApp**
   - `DisplayCommand.kt` and `InfoCommand.kt` hardcode `TextColors.*` values (`brightBlue`, `brightCyan`, `brightGreen`, `yellow`, `green`).
   - No shared theme mapping layer.

5. **sharedLib**
   - No existing color/theme token model.

---

## Research Summary Driving the Plan

1. **Material3 foundation**
   - Material3 theming should be built from explicit `ColorScheme` and provided via `MaterialTheme(colorScheme=..., typography=..., shapes=...)`.
   - Compose Multiplatform Material3 API supports complete `ColorScheme` customization (`lightColorScheme` / `darkColorScheme` baseline pattern).

2. **Custom semantic roles**
   - Recommended approach for roles not directly in `ColorScheme` (e.g., timer states): provide custom theme tokens via `CompositionLocal` (extended theme pattern), while still keeping Material3 as base.

3. **CLI color adaptability**
   - Mordant supports both standard named colors and `rgb("#RRGGBB")`, enabling shared token mapping with terminal capability downsampling handled by `Terminal.println`.

---

## Proposed Architecture

### A) Single source of truth in `sharedLib` (colors only)
Create a lightweight, UI-framework-agnostic token model in `sharedLib/commonMain`.

**Planned structure**
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/theme/ThemeColor.kt`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/theme/TwoFacColorTokens.kt`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/theme/TimerColorSemantics.kt`

**Token design principles**
- Keep data serializable and framework-neutral (no Compose `Color` type in sharedLib).
- Use semantic naming (what color means) rather than usage-specific naming.
- Include dedicated timer semantics for success/warn/critical.

**Example token model (plan snippet)**
```kotlin
package tech.arnav.twofac.lib.theme

data class ThemeColor(val argb: Long) // e.g. 0xFFFDFaf8

data class TimerColorSemantics(
    val healthy: ThemeColor,   // green
    val warning: ThemeColor,   // yellow/amber
    val critical: ThemeColor,  // red
)

data class TwoFacColorTokens(
    val brandBlue: ThemeColor,
    val neutralWhite: ThemeColor,
    val neutralBlack: ThemeColor,
    val surface: ThemeColor,
    val onSurface: ThemeColor,
    val background: ThemeColor,
    val onBackground: ThemeColor,
    val accent: ThemeColor,
    val danger: ThemeColor,
    val timer: TimerColorSemantics,
)

object TwoFacThemeTokens {
    val light: TwoFacColorTokens = TODO()
    val dark: TwoFacColorTokens = TODO()
}
```

### B) composeApp uses Material3 theme built from shared tokens
Add a Compose theme layer in `composeApp/commonMain` that:
1. Converts shared ARGB tokens to Compose `Color`.
2. Builds `lightColorScheme`/`darkColorScheme`.
3. Exposes extra semantic colors (timer) through `CompositionLocal`.

**Planned files**
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/theme/TwoFacTheme.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/theme/TwoFacExtendedColors.kt`

**Example compose theme wrapper (plan snippet)**
```kotlin
@Immutable
data class TwoFacExtendedColors(
    val timerHealthy: Color,
    val timerWarning: Color,
    val timerCritical: Color,
)

val LocalTwoFacExtendedColors = staticCompositionLocalOf {
    TwoFacExtendedColors(Color.Unspecified, Color.Unspecified, Color.Unspecified)
}

@Composable
fun TwoFacTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val shared = if (darkTheme) TwoFacThemeTokens.dark else TwoFacThemeTokens.light
    val scheme = if (darkTheme) darkColorScheme(
        primary = shared.brandBlue.toComposeColor(),
        background = shared.background.toComposeColor(),
        onBackground = shared.onBackground.toComposeColor(),
        // ...
    ) else lightColorScheme(
        primary = shared.brandBlue.toComposeColor(),
        background = shared.background.toComposeColor(),
        onBackground = shared.onBackground.toComposeColor(),
        // ...
    )
    val extended = TwoFacExtendedColors(
        timerHealthy = shared.timer.healthy.toComposeColor(),
        timerWarning = shared.timer.warning.toComposeColor(),
        timerCritical = shared.timer.critical.toComposeColor(),
    )
    CompositionLocalProvider(LocalTwoFacExtendedColors provides extended) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
```

### C) Standard timer threshold contract in sharedLib
Keep threshold semantics centralized and explicit.

**Example threshold contract (plan snippet)**
```kotlin
object TimerSemantics {
    const val HEALTHY_THRESHOLD = 0.50f
    const val WARNING_THRESHOLD = 0.25f
}

enum class TimerState { Healthy, Warning, Critical }

fun timerStateByRemainingProgress(remaining: Float): TimerState = when {
    remaining > TimerSemantics.HEALTHY_THRESHOLD -> TimerState.Healthy
    remaining > TimerSemantics.WARNING_THRESHOLD -> TimerState.Warning
    else -> TimerState.Critical
}
```

---

## Phase-by-Phase Implementation Roadmap

## Phase 0 - Baseline audit, palette extraction, and semantic token contract
### Deliverables
1. Confirm baseline palette from logo (`#fdfaf8`, `#171e1e`, `#2c7cbf`) and define derived neutrals for light/dark.
2. Finalize semantic role vocabulary:
   - `brand`, `surface`, `background`, `text`, `danger`, `timerHealthy`, `timerWarning`, `timerCritical`.
3. Decide initial theming policy:
   - **Default:** static brand theme (white/black/blue centered).
   - **Optional future:** dynamic color adaptation for supported Android versions (explicitly deferred unless requested).

### Exit criteria
- Token contract approved and mapped to all platforms.

---

## Phase 1 - Add sharedLib color-token source of truth (colors only)
### Work items
1. Add `ThemeColor`, `TwoFacColorTokens`, `TimerColorSemantics`, and default token objects to `sharedLib/commonMain`.
2. Add utility helpers:
   - ARGB/hex normalization.
   - Timer state threshold helper.
3. Add unit tests in `sharedLib/commonTest`:
   - Token validity (all required tokens non-null, in-range).
   - Timer threshold behavior at boundaries (0.50, 0.25).

### Example test cases (plan snippet)
```kotlin
@Test fun `timer state healthy above fifty percent`() { ... }
@Test fun `timer state warning between twenty five and fifty`() { ... }
@Test fun `timer state critical at or below twenty five`() { ... }
```

### Exit criteria
- sharedLib exports stable color/token API for all clients.

---

## Phase 2 - Build composeApp Material3 theme foundation from shared tokens
### Work items
1. Create `TwoFacTheme` wrapper in composeApp and replace root `MaterialTheme` usage in `App.kt`.
2. Build full `ColorScheme` using shared tokens.
3. Add `TwoFacExtendedColors` `CompositionLocal` for non-Material semantics (timer urgency colors).
4. Replace direct/implicit usage patterns where needed with semantic accessors.

### Migration targets (high priority)
- `composeApp/.../App.kt` root theme wiring.
- `composeApp/.../components/OTPCard.kt` timer colors via extended timer tokens.
- Any screen currently relying on generic defaults where brand mapping should be explicit.

### Exit criteria
- composeApp no longer depends on default Material3 colors; it uses centrally defined brand scheme.

---

## Phase 3 - Centralize timer bar red/yellow/green semantics across Compose + watchApp + iOS watch
### Work items
1. Use shared timer threshold helper from sharedLib in compose and watch logic.
2. Replace:
   - Compose timer color selection in `OTPCard.kt`.
   - Android watch arc color logic in `OtpAccountScreen.kt`.
   - iOS watch `CountdownBar.barColor` decision logic in Swift (mirroring shared thresholds).
3. Keep state mapping identical across all three surfaces.

### Swift integration snippet (plan snippet)
```swift
let state = TimerStateMapper.stateFor(remaining: remaining)
switch state {
case .healthy: return theme.timerHealthy
case .warning: return theme.timerWarning
case .critical: return theme.timerCritical
}
```

### Exit criteria
- Timer urgency colors and thresholds behave consistently across app, Android watch, and iOS watch.

---

## Phase 4 - Adopt shared tokens in watchApp (Wear Compose) and iosApp watch extension (SwiftUI)
### Android watchApp
1. Expand `watchApp/.../presentation/theme/Theme.kt` from minimal wrapper to token-backed Wear theme.
2. Convert hardcoded colors and direct defaults to token-mapped values.

### iOS watch extension
1. Expose shared token values through the generated Kotlin framework API (`TwoFacKit`) in a Swift-consumable shape (ARGB/hex).
2. Add Swift color conversion helper:
   - `ThemeColor(argb) -> SwiftUI.Color`.
3. Replace hardcoded `.green/.yellow/.red` and other direct hardcoded alert colors with shared semantic mapping.

### Swift conversion snippet (plan snippet)
```swift
extension Color {
    init(argb: UInt32) {
        let a = Double((argb >> 24) & 0xff) / 255.0
        let r = Double((argb >> 16) & 0xff) / 255.0
        let g = Double((argb >> 8) & 0xff) / 255.0
        let b = Double(argb & 0xff) / 255.0
        self = Color(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
```

### Exit criteria
- Both watch surfaces consume shared token semantics with no remaining hardcoded urgency colors.

---

## Phase 5 - Adopt shared tokens in cliApp via Mordant color mapping
### Work items
1. Add CLI adapter mapping from shared semantic tokens to Mordant styles:
   - Prefer `TextColors.rgb("#RRGGBB")` for brand fidelity.
   - Optional fallback map to named `TextColors.*` if terminal capabilities are limited.
2. Replace hardcoded `TextColors` references in:
   - `DisplayCommand.kt`
   - `InfoCommand.kt`
3. Align CLI progress/timer visual semantics to same urgency states.

### CLI mapping snippet (plan snippet)
```kotlin
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb

object CliTheme {
    val header = rgb("#2C7CBF")
    val ok = rgb("#4CAF50")
    val warn = rgb("#FFC107")
    val critical = rgb("#F44336")
}
```

### Exit criteria
- CLI color usage is centralized and derived from shared tokens.

---

## Phase 6 - Validation matrix, accessibility checks, and rollout sequence
### Validation checklist
1. **Visual consistency**
   - Compare composeApp + watchApp + iOS watch + CLI screenshots/outputs for token consistency.
2. **Contrast checks**
   - Verify key text contrast on brand blue/off-white/near-black combinations (light and dark variants).
3. **Timer behavior parity**
   - Validate threshold transitions at identical elapsed points across all platforms.
4. **Regression sweep**
   - ensure no screens regress to unreadable defaults.

### Suggested rollout slices (small PR strategy)
1. PR1: sharedLib token model + tests.
2. PR2: composeApp theme foundation and root adoption.
3. PR3: timer semantics centralization in composeApp + watchApp.
4. PR4: iOS watch extension token adoption.
5. PR5: cliApp mapping + final consistency QA.

### Exit criteria
- Centralized token ownership achieved and consumed by all required app fronts.

---

## Planned File-Level Change Map

### sharedLib (new)
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/theme/ThemeColor.kt`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/theme/TwoFacColorTokens.kt`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/theme/TimerColorSemantics.kt`
- `sharedLib/src/commonTest/kotlin/tech/arnav/twofac/lib/theme/*Test.kt`

### composeApp
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/theme/TwoFacTheme.kt` (new)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/theme/TwoFacExtendedColors.kt` (new)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/App.kt` (switch to `TwoFacTheme`)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/OTPCard.kt` (timer colors via semantic tokens)

### watchApp
- `watchApp/src/main/java/tech/arnav/twofac/watch/presentation/theme/Theme.kt`
- `watchApp/src/main/java/tech/arnav/twofac/watch/ui/OtpAccountScreen.kt`

### iosApp
- `iosApp/watchAppExtension/WatchExtensionContentView.swift`
- `iosApp/watchAppExtension/*` helper for ARGB/semantic token mapping (new helper file expected)

### cliApp
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/theme/*` (new mapper helpers)
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/commands/DisplayCommand.kt`
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/commands/InfoCommand.kt`

---

## Risks and Mitigations
1. **Risk:** Token format mismatch across Kotlin/Swift/CLI.
   - **Mitigation:** Standardize on ARGB in sharedLib + tested converters per platform.
2. **Risk:** Wear Material (non-M3) differs from composeApp Material3 role model.
   - **Mitigation:** Map shared semantic tokens directly to Wear theme colors where role parity is imperfect.
3. **Risk:** Terminal capability variability for truecolor in CLI.
   - **Mitigation:** Use Mordant style rendering via `Terminal.println`; add fallback named colors if needed.
4. **Risk:** Dark-mode ambiguity for brand fidelity.
   - **Mitigation:** Ship explicit dark tokens in sharedLib from day one, even if conservative.

---

## Sources Consulted (Research)
- Repository files:
  - `docs/twofac-logo.svg`
  - `composeApp/.../App.kt`
  - `composeApp/.../components/OTPCard.kt`
  - `watchApp/.../presentation/theme/Theme.kt`
  - `watchApp/.../ui/OtpAccountScreen.kt`
  - `iosApp/watchAppExtension/WatchExtensionContentView.swift`
  - `cliApp/.../commands/DisplayCommand.kt`
  - `cliApp/.../commands/InfoCommand.kt`
- External docs and references:
  - https://developer.android.com/develop/ui/compose/designsystems/material3
  - https://developer.android.com/develop/ui/compose/designsystems/custom
  - https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-material-theme.html
  - https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-color-scheme/
  - https://ajalt.github.io/mordant/guide/
  - https://ajalt.github.io/mordant/api/mordant/com.github.ajalt.mordant.rendering/-text-colors/
