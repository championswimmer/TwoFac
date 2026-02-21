# AGENTS.md

This repository is a Kotlin Multiplatform 2FA project with multiple app frontends over one shared core library.

## Module map

- `sharedLib/`: core domain library (OTP generation, storage models, import adapters, crypto helpers)
- `composeApp/`: Compose Multiplatform UI app (Android, iOS framework, Desktop JVM, Web/Wasm)
- `cliApp/`: native CLI app built with Clikt, uses `sharedLib`
- `watchApp/`: Android Wear OS app, currently minimal UI scaffold with `sharedLib` dependency
- `iosApp/`: Xcode project that integrates the Compose iOS framework

## Dependency direction

- `sharedLib` is the central dependency used by `composeApp`, `cliApp`, and `watchApp`.
- `iosApp` consumes the framework generated from `composeApp`.

## Build and test entry points

- Baseline checks: `./gradlew check`
- Shared library tests: `./gradlew :sharedLib:test`
- CLI tests: `./gradlew :cliApp:test`
- Compose tests: `./gradlew :composeApp:test`

## Agent navigation

To keep this file concise, module-specific guidance is in:

- `sharedLib/AGENTS.md`
- `composeApp/AGENTS.md`
- `cliApp/AGENTS.md`
- `watchApp/AGENTS.md`
