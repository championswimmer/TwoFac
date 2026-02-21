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
