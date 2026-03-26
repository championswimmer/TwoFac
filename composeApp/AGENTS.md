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

## Localization / String Resources

All user-facing strings live in Compose Multiplatform resource XML files under `src/commonMain/composeResources/values/`. Generated accessors are in `twofac.composeapp.generated.resources.*`.

### Resource files by domain

| File | Domain |
|------|--------|
| `strings_core.xml` | App identity, navigation, common actions/labels, OTP, errors |
| `strings_home.xml` | Home screen states (empty, locked, loading) |
| `strings_accounts.xml` | Accounts list, add account, account detail |
| `strings_settings.xml` | Settings screen, storage, companion sync, desktop tray/window |
| `strings_backup.xml` | Backup/export dialogs and provider rows |
| `strings_onboarding.xml` | Onboarding guide steps, headers, states |
| `strings_security.xml` | Passkey dialog, biometric prompts |

### How to add a new string

1. Choose the resource file matching the feature domain.
2. Add a `<string name="...">` entry using the naming convention below.
3. Use `stringResource(Res.string.xxx)` in `@Composable` functions or `getString(Res.string.xxx)` in suspend functions.
4. Import from `twofac.composeapp.generated.resources.*`.

### Key naming convention

Keys follow the pattern `{domain}_{feature}_{element}`:

- `home_empty_title`, `home_locked_description`
- `settings_biometric_title`, `settings_companion_sync_button`
- `accounts_add_title`, `accounts_detail_edit_button`
- `action_cancel`, `action_delete` (common actions use `action_` prefix)
- `error_prefix`, `error_unknown` (common errors use `error_` prefix)

### Formatted strings and plurals

- Use `%1$s`, `%2$s` for string arguments; `%1$d` for integers.
- Pass arguments via `stringResource(Res.string.key, arg1, arg2)`.
- Compose Multiplatform resources do not yet support `<plurals>`; use conditional logic in Kotlin for now.

### How to add a new locale

1. Create `src/commonMain/composeResources/values-{lang}/` (e.g. `values-fr/`, `values-ja/`).
2. Copy each `strings_*.xml` file and translate the values.
3. The framework automatically selects the correct locale at runtime based on the system language.
4. Keep translations complete per locale before advertising support.

### Language selection policy

The app currently uses **system locale only** — the OS language setting determines which strings are displayed. No in-app language override is implemented. If one is needed in the future, add an `AppLanguage` setting and inject the locale at the app root.

### watchApp strings

The `watchApp` module is a standalone Android Wear OS app and uses standard Android `strings.xml` resources at `watchApp/src/main/res/values/strings.xml`, not Compose Multiplatform resources.
