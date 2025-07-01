# TwoFac

##### Open Source, Native, Cross-Platform 2FA App for Watch, Mobile, Desktop, Web, CLI

[![Kotlin](https://img.shields.io/badge/kotlin-2.2.0-blue.svg?logo=kotlin)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/compose-multiplatform-blue.svg?logo=jetbrains)](https://github.com/JetBrains/compose-multiplatform)

## ROADMAP

- [ ] Common functionality
    - [x] Add new accounts
    - [x] Display accounts with 2FA codes
    - [x] Save accounts to a storage
    - [ ] Backup & Restore via a backup transport
    - [ ] Export & Import accounts (encrypted with passkey)
    - [ ] Import from other 2FA apps
        - [ ] Authy
        - [ ] 2FAS
        - [ ] Ente
- [ ] Mobile App
- [ ] Desktop App
- [ ] Web Extension
- [ ] CLI App
    - [ ] Add new accounts
    - [x] Display 2FA codes with auto-refresh

## CODE STRUCTURE
This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - `commonMain` is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* `/cliApp` contains the command-line interface application code.
  It uses the shared library to provide 2FA functionality through terminal commands.
  The CLI is built using Clikt framework and provides features like:
    - Display 2FA codes with auto-refresh
    - Add new accounts
    - Show platform information

* `/sharedLib` is a shared library that contains the core 2FA functionality.
  It is used by all applications and provides the logic for managing 2FA accounts,
  generating codes, and handling encryption.
  The library is written in Kotlin and can be used across different platforms.

## DEPENDENCY STRUCTURE

| App               | Codebase                       | Depends on sharedLib variant |
|-------------------|--------------------------------|------------------------------|
| Android           | `composeApp/androidMain`       | `jvm`                        |
| iOS (+ Simulator) | `iosApp -> composeApp/iosMain` | `native` (as framework)      |
| Desktop           | `composeApp/desktopMain`       | `jvm`                        |
| Web               | `composeApp/wasmJsMain`        | `wasmJs`                     |
| CLI               | `cliApp`                       | `native` (as static lib)     |



