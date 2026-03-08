# composeApp AGENTS.md

`composeApp` is the Compose Multiplatform UI library that powers the graphical interface for Android, iOS, Desktop, and Web.

## What this module does
It provides the visual interface and user flows (Home, Settings, Add Account). It bridges the platform-agnostic UI with platform-specific capabilities such as biometric/passkey unlock, camera and clipboard QR scanning, desktop tray integration, browser storage/WebAuthn interop, and companion device synchronization. It handles the injection of `sharedLib` into ViewModels to display data.

## Dependencies
Depends heavily on `:sharedLib` for data structures, cryptographic unlock flows, and OTP generation.

## Platforms
Runs on:
- **Android** (Jetpack Compose)
- **iOS** (Compose Multiplatform exported as `TwoFacUIKit`)
- **Desktop** (JVM, via Skiko rendering to native windows)
- **Web** (WasmJs, producing the PWA and Chrome/Firefox extensions)

## Libraries Used
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform) - Declarative UI framework.
- [Koin](https://github.com/InsertKoinIO/koin) - Dependency injection.
- [KStore](https://github.com/xxfast/KStore) - For persistent storage serialization into the `accounts.json` file.
- [AppDirs](https://github.com/Syer123/kotlin-multiplatform-appdirs) - Discovering the correct OS-level data directories for storage.
- [KScan](https://github.com/ismai117/KScan) - Camera QR scanning for Android and iOS.
- [ZXing](https://github.com/zxing/zxing) - Desktop image QR decoding.
- [jsQR](https://github.com/cozmo/jsQR) - Web WasmJs QR decoding.
- `androidx.biometric` - Secure unlock via fingerprint/face (Android).

## Code Structure
- `src/commonMain/`: Shared screens, navigation, DI, storage/session abstractions, plus reusable `components/`, `theme/`, `viewmodels/`, `wear/`, and `companion/` packages.
- `src/androidMain/`: Android platform bindings, biometric prompt implementations, QR scanner integrations, storage wiring, and Wear OS DataLayer publishing logic.
- `src/iosMain/`: `MainViewController` entrypoint, iOS-specific DI, QR/session/storage integrations, and companion-sync helpers.
- `src/desktopMain/`: JVM `main()` entry, tray/window configuration, desktop settings management, QR readers, and local file backup transport.
- `src/wasmJsMain/`: Web/Wasm entrypoint, browser session/storage/QR code interop, and `@JsModule` bridges to TypeScript helpers.
- `src/webMain/`: Web-only packaged resources layered on top of the Wasm app.
- `src/{commonTest,androidInstrumentedTest,iosTest,wasmJsTest}/`: Module tests for shared flows and platform-specific integrations.
- `src/wasmJsMain/typescript/src/`: TypeScript source files (`crypto.mts`, `storage.mts`, `qr-reader.mts`, `webauthn.mts`, `time.mts`) compiled into the Wasm/browser bundle.
- `extension/`: Browser-extension overlay files (`manifest.base.json`, `background.js`, `popup.html`, `sidepanel.html`) used when packaging the Wasm output for Chrome/Firefox.
