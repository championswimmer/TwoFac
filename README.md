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

## What is TwoFac?

TwoFac is a free, open-source, cross-platform 2FA app for generating one-time codes on the devices you already use. It is built with Kotlin Multiplatform and ships native experiences for mobile, desktop, wearables, web, browser extensions, and the command line.

It is designed to be practical and privacy-friendly: local code generation, biometric protection on supported platforms, encrypted backups, and no ads or tracking.

## Features

- Native apps for Android, iOS, macOS, Windows, Linux, Wear OS, watchOS, web, browser extensions, and CLI
- End-to-end encryption and zero-knowledge handling for secrets and backups
- Biometric unlock on Android and iOS
- Import support for Authy, 2FAS, Ente Auth, and Google Authenticator exports
- Companion watch apps with offline access and browser/CLI workflows for fast access

## Download

**Start here:** [twofac.app/download](https://twofac.app/download)

[![Get it on Google Play](docs/google-play-badge.png)](https://play.google.com/store/apps/details?id=tech.arnav.twofac.app)

- **Website:** [twofac.app](https://twofac.app)
- **Web app:** [web.twofac.app](https://web.twofac.app)
- **Desktop and CLI releases:** [GitHub Releases](https://github.com/championswimmer/TwoFac/releases)

For macOS and Linux, the CLI can also be installed with:

```bash
curl -fsSL https://twofac.app/install.sh | bash -s --
```

## Learn more

- **Features:** [twofac.app/features](https://twofac.app/features)
- **Getting started:** [twofac.app/getting-started](https://twofac.app/getting-started)
- **FAQ:** [twofac.app/faq](https://twofac.app/faq)
- **Import guide:** [docs/IMPORTING.md](docs/IMPORTING.md)

## Development

Clone the repo, install the toolchains for the targets you want to work on, and run Gradle tasks from the repository root.

For the current module map, build guidance, and platform routing, read [AGENTS.md](AGENTS.md). It is written to make AI-driven development work smoothly, and it is also the most up-to-date reference for how this codebase is organized.
