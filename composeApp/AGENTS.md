# composeApp AGENTS.md

`composeApp` is the Compose Multiplatform UI library that powers the graphical interface for Android, iOS, Desktop, and Web.

## What this module does
It provides the visual interface and user flows (Home, Settings, Add Account). It bridges the platform-agnostic UI with platform-specific capabilities such as Biometric authentication, Camera-based QR scanning, Clipboard access, and companion device synchronization. It handles the injection of `sharedLib` into ViewModels to display data.

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
- `src/commonMain/`: Shared screens, ViewModels, navigation, and DI definitions.
- `src/androidMain/`: Android entry points, Biometric prompt implementations, and Wear OS DataLayer publishing logic.
- `src/iosMain/`: iOS UI controller bindings and iOS Keychain / FaceID integration.
- `src/desktopMain/`: JVM `main()` entry, tray/window configuration, and local file system backup logic.
- `src/wasmJsMain/`: Web WasmJS setup, `BrowserSessionManager` for WebAuthn secure unlock, and browser storage JS interop.
- `extension/`: Overlay files (`manifest.json`, `background.js`, `popup.html`) used to package the Wasm output as browser extensions.
