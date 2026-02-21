# sharedLib AGENTS.md

`sharedLib` contains cross-platform 2FA business logic and public APIs used by all apps.

## What lives here

- OTP engines: `otp/` (`TOTP`, `HOTP`)
- Parsing: `uri/OtpAuthURI.kt`
- Core facade: `TwoFacLib.kt`
- Import adapters: `importer/adapters/` (Authy, 2FAS, Ente)
- Storage contracts and models: `storage/`
- Crypto abstraction + defaults: `crypto/`

## Targets

- `jvm` (Android/Desktop consumers)
- `native` (CLI + exported libs)
- `wasmJs` (web consumer)

## Testing focus

- Prefer `commonTest` for business logic behavior.
- Keep target-specific tests (`jvmTest`, `nativeTest`, `wasmJsTest`) for platform/provider differences.
