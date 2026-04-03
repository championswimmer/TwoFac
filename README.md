| <img src="docs/twofac-logo.png" alt="TwoFac logo" width="128" height="128" /> | <h1 align="left">TwoFac</h1> <br> Open Source, Native, Cross-Platform 2FA App for Watch, Mobile, Desktop, Web and CLI! | 
|---|---|

[![Kotlin](https://img.shields.io/badge/kotlin-2.3.10-blue.svg?logo=kotlin)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/compose-multiplatform-blue.svg?logo=jetbrains)](https://github.com/JetBrains/compose-multiplatform)

[![Android](https://img.shields.io/badge/android-11%2B-3DDC84.svg?logo=android)](https://developer.android.com/)
[![iOS](https://img.shields.io/badge/iOS-18.2%2B-000000.svg?logo=apple)](https://developer.apple.com/ios/)
[![watchOS](https://img.shields.io/badge/watchOS-10%2B-000000.svg?logo=apple)](https://developer.apple.com/watchos/)
[![Wear OS](https://img.shields.io/badge/wearOS-11%2B-4285F4.svg?logo=wearos)](https://wearos.google.com/)
[![macOS](https://img.shields.io/badge/macOS-16%2B-000000.svg?logo=apple)](https://www.apple.com/macos/)
[![Windows](https://img.shields.io/badge/Windows-10%2B-0078D4.svg?logo=windows)](https://www.microsoft.com/windows/)
[![Ubuntu](https://img.shields.io/badge/Ubuntu-22.04%2B-E95420.svg?logo=ubuntu)](https://ubuntu.com/)
[![Web](https://img.shields.io/badge/platform-web-F7DF1E.svg?logo=webassembly)](https://webassembly.org/)
[![Chrome Extension](https://img.shields.io/badge/Chrome%20Extension-119%2B-4285F4.svg?logo=googlechrome)](https://developer.chrome.com/docs/extensions/)
[![Firefox Extension](https://img.shields.io/badge/Firefox%20Extension-120%2B-FF7139.svg?logo=firefoxbrowser)](https://extensionworkshop.com/)
[![CLI macOS](https://img.shields.io/badge/CLI-macOS-000000.svg?logo=apple)](https://www.apple.com/macos/)
[![CLI Windows](https://img.shields.io/badge/CLI-Windows-0078D4.svg?logo=windows)](https://www.microsoft.com/windows/)
[![CLI Linux](https://img.shields.io/badge/CLI-Linux-4EAA25.svg?logo=gnubash)](https://en.wikipedia.org/wiki/Command-line_interface)

![two-fac-demo](https://github.com/user-attachments/assets/b95f3bc8-b27b-42c7-8dce-041ea9465dcb)


## ROADMAP

- [ ] Common functionality
    - [x] Add new accounts
    - [x] Display accounts with 2FA codes
    - [x] Save accounts to a storage
    - [x] Delete account storage
    - [ ] Backup & Restore via a backup transport
    - [ ] Export & Import accounts (encrypted with passkey)
    - [x] QR Account Scanning (Camera + Clipboard)
    - [x] Import from other 2FA apps
        - [x] Authy (JSON format)
        - [x] 2FAS (JSON format)
        - [x] Ente (plaintext otpauth:// URIs)
- [ ] Mobile Apps (Android & iOS)
    - [x] Biometric Authentication
    - [ ] Home / Accounts / Settings Bottom Tabs
- [x] Wearable Apps
    - [x] Wear OS companion app with offline sync
    - [x] watchOS companion app with offline sync
- [ ] Desktop App
    - [ ] System Tray / Menu Bar Application
- [x] Web & Browser Extensions
    - [x] Progressive Web App (PWA)
    - [x] Chrome Extension
    - [x] Firefox Extension
    - [x] WebAuthn / Device-Credential Unlock
- [ ] CLI App
    - [x] Add new accounts
    - [x] Display 2FA codes with auto-refresh
    - [x] Delete account storage

## Documentation

- **[Importing from Other Apps](docs/IMPORTING.md)** - Guide to importing 2FA secrets from Authy, 2FAS, Ente Auth, and other authenticator apps
- **[Development Guide](#development)** - Setup, building, and running the project
- **[GitHub Copilot Setup](.github/workflows/copilot-setup-steps.yml)** - Pre-configured environment with Java 21, Node.js 22, and Gradle caching

## Development

### Prerequisites

- **JDK 17+** (recommended: JDK 21)
- **Android SDK** (if building Android targets)
- **Xcode** (if building iOS targets on macOS)
- **Native toolchains** for your platform (GCC/Clang for Linux, MSVC for Windows)

### Project structure

See [AGENTS.md](AGENTS.md) for the full module map, dependency direction, and
platform-to-module routing — it is the single source of truth for contributor
guidance.

```
TwoFac/
├── sharedLib/    # Shared 2FA library (TOTP/HOTP, crypto, storage)
├── cliApp/       # CLI application (Clikt-based, native binaries)
├── composeApp/   # Compose Multiplatform UI application
├── watchApp/     # Wear OS app
└── iosApp/       # iOS application wrapper
```

### Common commands

```bash
# Baseline checks
./gradlew check

# Module tests
./gradlew :sharedLib:test
./gradlew :cliApp:allTests
./gradlew :composeApp:test

# Run desktop app
./gradlew :composeApp:run

# Run web app
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

### CLI usage (dual mode)

`2fac` now supports two run modes:

- **Interactive TUI mode**: run `2fac` in an interactive terminal
- **One-shot CLI mode**: run explicit subcommands

When `2fac` is run without subcommands in a non-interactive terminal, it prints help and exits.

```text
2fac
  display
  info
  accounts
    add
    remove
  storage
    --use-backend <standalone|common>
    clean
    delete
    reinitialize
    backup
      export
      import
```

#### Migration notes

- Root-level commands `2fac add ...` and `2fac backup ...` were removed.
- Use `2fac accounts add ...` and `2fac storage backup ...` instead.

