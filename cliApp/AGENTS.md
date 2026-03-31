# cliApp AGENTS.md

`cliApp` is the native command-line frontend for TwoFac.

## What this module does
It provides a terminal-based interface to interact with TwoFac. It allows users to add and delete accounts, reset local storage, back up data, examine system info, and securely view live 2FA codes with an auto-refreshing progress indicator right in the shell.

## Dependencies
Depends directly on `:sharedLib` for all 2FA logic, cryptography, and storage interfaces.

## Platforms
Compiles natively (Kotlin/Native) to:
- **macOS** (`macosArm64`, `macosX64`)
- **Linux** (`linuxX64`)
- **Windows** (`mingwX64`)

## Libraries Used
- [Clikt](https://github.com/ajalt/clikt) - Command-line argument and option parsing.
- [Mordant](https://github.com/ajalt/mordant) - Terminal styling, tables, colors, and dynamic screen animations (used for the live OTP countdown).
- [Koin](https://github.com/InsertKoinIO/koin) - Dependency injection wiring for the CLI context.
- [KStore](https://github.com/xxfast/KStore) - Reading/writing to the local `accounts.json` file.
- [AppDirs](https://github.com/Syer123/kotlin-multiplatform-appdirs) - Finding the local application data directory.

## Code Structure
- `src/commonMain/kotlin/tech/arnav/twofac/cli/`:
  - `Main.kt`: Entry point routing via Clikt subcommands.
  - `commands/`: CLI command implementations (`DisplayCommand`, `AddCommand`, `BackupCommand`, `InfoCommand`, `StorageCommand`).
  - `di/`: Koin modules used to wire the CLI runtime.
  - `theme/`: `CliTheme` terminal styling that maps shared theme/timer semantics into Mordant text styles.
  - `storage/`: Configuration for local path resolution and FileStorage via KStore.
  - `backup/`: CLI-specific implementation of the local filesystem `BackupTransport`.
- `src/{linuxMain,macosMain,mingwMain}/kotlin/tech/arnav/twofac/cli/`: Native platform shims for filesystem and terminal integration.
- `src/commonTest/kotlin/tech/arnav/twofac/cli/`: Command and DI tests (`DisplayCommandTest`, `InfoCommandTest`, `StorageCommandTest`, `KoinVerificationTest`).
