# TwoFac

<img src="docs/twofac-logo.png" alt="TwoFac logo" width="64" height="64" />

##### Open Source, Native, Cross-Platform 2FA App for Watch, Mobile, Desktop, Web, CLI

[![Kotlin](https://img.shields.io/badge/kotlin-2.2.0-blue.svg?logo=kotlin)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/compose-multiplatform-blue.svg?logo=jetbrains)](https://github.com/JetBrains/compose-multiplatform)

[![Android](https://img.shields.io/badge/platform-android-3DDC84.svg?logo=android)](https://developer.android.com/)
[![iOS](https://img.shields.io/badge/platform-ios-000000.svg?logo=apple)](https://developer.apple.com/ios/)
[![Desktop](https://img.shields.io/badge/platform-desktop-2496ED.svg?logo=openjdk)](https://openjdk.org/)
[![Web](https://img.shields.io/badge/platform-web-F7DF1E.svg?logo=webassembly)](https://webassembly.org/)
[![CLI](https://img.shields.io/badge/platform-cli-4EAA25.svg?logo=gnubash)](https://en.wikipedia.org/wiki/Command-line_interface)
[![Wear OS](https://img.shields.io/badge/platform-wearos-4285F4.svg?logo=wearos)](https://wearos.google.com/)

![two-fac-demo](https://github.com/user-attachments/assets/b95f3bc8-b27b-42c7-8dce-041ea9465dcb)


## ROADMAP

- [ ] Common functionality
    - [x] Add new accounts
    - [x] Display accounts with 2FA codes
    - [x] Save accounts to a storage
    - [ ] Backup & Restore via a backup transport
    - [ ] Export & Import accounts (encrypted with passkey)
    - [x] Import from other 2FA apps
        - [x] Authy (JSON format)
        - [x] 2FAS (JSON format)
        - [x] Ente (plaintext otpauth:// URIs)
- [ ] Mobile App
- [ ] Desktop App
- [ ] Web Extension
- [ ] CLI App
    - [ ] Add new accounts
    - [x] Display 2FA codes with auto-refresh

## Documentation

- **[Importing from Other Apps](docs/IMPORTING.md)** - Guide to importing 2FA secrets from Authy, 2FAS, Ente Auth, and other authenticator apps
- **[Development Guide](DEVELOPMENT.md)** - Setup, building, and contributing to the project


