# AGENTS.md

This repository is a Kotlin Multiplatform 2FA project with multiple app frontends over one shared core library.

## Module map

- `sharedLib/`: core domain library (OTP generation, storage models, import adapters, crypto helpers)
- `composeApp/`: Compose Multiplatform UI library (Android library, iOS framework, Desktop JVM, Web/Wasm, browser-extension packaging)
  - `commonMain`: code shared across all Compose targets
  - `androidMain`: Android-specific shared code (DI, biometrics, wear sync, storage)
  - `iosMain`: iOS-specific code (compiled into framework consumed by `iosApp`)
  - `desktopMain`: JVM desktop-specific code
  - `wasmJsMain`: Web/Wasm-specific code
- `androidApp/`: thin Android app wrapper that hosts `composeApp` (Application, Activity, resources)
- `cliApp/`: native CLI app built with Clikt, uses `sharedLib`
  - Provides 2FA codes with auto-refresh, account management, backup workflows, and platform info commands
- `watchApp/`: Android Wear OS companion with synced OTP data, offline caching, and pager-based code display
- `iosApp/`: Xcode project containing the native iOS wrapper plus the watchOS companion targets

## Dependency direction

- `sharedLib` is the central dependency used directly by `composeApp`, `cliApp`, and `watchApp`, and is exported to Apple platforms as `TwoFacKit`.
- `androidApp` depends on `composeApp` (thin wrapper for Android entry point).
- `iosApp` consumes `TwoFacUIKit` from `composeApp` for the iOS app and `TwoFacKit` from `sharedLib` for the watchOS companion.

## Platform-to-module mapping

| Platform          | Codebase                                | sharedLib variant        |
|-------------------|-----------------------------------------|--------------------------|
| Android           | `androidApp → composeApp/androidMain`   | `jvm`                    |
| iOS (+ Simulator) | `iosApp → composeApp/iosMain`           | `native` (as framework)  |
| watchOS           | `iosApp/watchAppExtension → sharedLib`  | `native` (as framework)  |
| Desktop           | `composeApp/desktopMain`                | `jvm`                    |
| Web               | `composeApp/wasmJsMain`                 | `wasmJs`                 |
| CLI               | `cliApp`                                | `native` (as static lib) |
| Wear OS           | `watchApp`                              | `jvm`                    |

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
- `iosApp/AGENTS.md`

`androidApp` intentionally has no dedicated `AGENTS.md`; it remains a thin Android wrapper around `composeApp`.
