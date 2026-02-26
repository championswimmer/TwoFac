# cliApp AGENTS.md

`cliApp` is the native command-line frontend for TwoFac.

## Key files

- Entry point: `src/commonMain/kotlin/tech/arnav/twofac/cli/Main.kt`
- Commands: `commands/` (`DisplayCommand`, `AddCommand`, `InfoCommand`)
- DI modules: `di/`
- View models: `viewmodels/`
- Storage helpers: `storage/`

## Build targets

- `macosArm64`, `macosX64`, `linuxX64`, `mingwX64`
- Produces executable binary named `2fac`

## Testing focus

- `commonTest` contains command behavior and DI verification tests.

## UI and Terminal Rendering (Mordant)

The CLI relies heavily on the [Mordant](https://github.com/ajalt/mordant) library for styling and stateful terminal capabilities:

- **Styling**: Uses `table`, borders, text colors, and dimension styling to create a clean, modern command-line interface.
- **Dynamic Screens & Timers**: To display live OTP countdowns without constantly flooding or flickering the terminal, `DisplayCommand` leverages Mordant's built-in animations.
  - An animation `Terminal.animation<DisplayAccountsStatic>` is declared which renders a styled table layout based on the current state framework.
  - A coroutine loop securely delays for 1 second intervals via `runBlocking` and invokes `animation.update(data)` repeatedly. 
  - On each loop iteration, Mordant efficiently recalculates the layout state, handles terminal ANSI escape codes to clear lines or reposition the cursor relative to screen size, and injects updated representations—such as updated remaining seconds text and dynamic `ProgressBar` widgets—seamlessly into the same footprint, mimicking a dynamic digital screen.
