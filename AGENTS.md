# AGENTS.md

This repository is a Kotlin Multiplatform 2FA project with multiple app frontends over one shared core library.

## Module map

- `sharedLib/`: core domain library (OTP generation, storage models, import adapters, crypto helpers)
- `composeApp/`: Compose Multiplatform UI app (Android, iOS framework, Desktop JVM, Web/Wasm)
  - `commonMain`: code shared across all Compose targets
  - `androidMain`: Android-specific code
  - `iosMain`: iOS-specific code (compiled into framework consumed by `iosApp`)
  - `desktopMain`: JVM desktop-specific code
  - `wasmJsMain`: Web/Wasm-specific code
- `cliApp/`: native CLI app built with Clikt, uses `sharedLib`
  - Provides 2FA codes with auto-refresh, account management, and platform info commands
- `watchApp/`: Android Wear OS app, currently minimal UI scaffold with `sharedLib` dependency
- `iosApp/`: Xcode project that integrates the Compose iOS framework

## Dependency direction

- `sharedLib` is the central dependency used by `composeApp`, `cliApp`, and `watchApp`.
- `iosApp` consumes the framework generated from `composeApp`.

## Platform-to-module mapping

| Platform          | Codebase                       | sharedLib variant        |
|-------------------|--------------------------------|--------------------------|
| Android           | `composeApp/androidMain`       | `jvm`                    |
| iOS (+ Simulator) | `iosApp → composeApp/iosMain`  | `native` (as framework)  |
| Desktop           | `composeApp/desktopMain`       | `jvm`                    |
| Web               | `composeApp/wasmJsMain`        | `wasmJs`                 |
| CLI               | `cliApp`                       | `native` (as static lib) |
| Wear OS           | `watchApp`                     | `jvm`                    |

## Agent skills

Use the dedicated skills for detailed command and routing guidance:

- `.agents/skills/gradle-build/SKILL.md`
- `.agents/skills/module-routing/SKILL.md`
- `.agents/skills/kmp-modules/SKILL.md`

## Agent navigation

To keep this file concise, module-specific guidance is in:

- `sharedLib/AGENTS.md`
- `composeApp/AGENTS.md`
- `cliApp/AGENTS.md`
- `watchApp/AGENTS.md`
