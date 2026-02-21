# composeApp AGENTS.md

`composeApp` is the Compose Multiplatform UI layer for Android, iOS, Desktop, and Web.

## Source-set layout

- `src/commonMain`: shared UI, navigation, DI wiring, view models, storage adapter layer
- `src/androidMain`: Android entry points (`MainActivity`, `TwoFacApplication`)
- `src/iosMain`: iOS bridge (`MainViewController`)
- `src/desktopMain`: Desktop entry point (`main.kt`)
- `src/wasmJsMain`: Web entry point (`main.kt`)

## Architecture notes

- Navigation and screens are in `commonMain`.
- UI depends on `sharedLib` for core OTP/account logic.
- Platform-specific `Platform.*.kt` and `AppDirUtils.*.kt` files implement host differences.
