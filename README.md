| <img src="docs/twofac-logo.png" alt="TwoFac logo" width="128" height="128" /> | <h1 align="left">TwoFac</h1> <br> Open Source, Native, Cross-Platform 2FA App for Watch, Mobile, Desktop, Web and CLI! | 
|---|---|

[![Kotlin](https://img.shields.io/badge/kotlin-2.2.0-blue.svg?logo=kotlin)](https://kotlinlang.org/)
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

