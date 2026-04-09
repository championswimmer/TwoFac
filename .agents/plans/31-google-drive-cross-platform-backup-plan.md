---
name: Google Drive Cross-Platform Backup Plan
status: Draft
---

> **Legend**
> - 🧑 **HUMAN** — requires a person (GCP Console, credential decisions, device testing)
> - 🤖 **AGENT** — pure code/config change; can be done autonomously

# Google Drive Cross-Platform Backup Plan

## Goal

Enable Google Drive as a unified backup/restore mechanism that works across **Android, iOS, Desktop (JVM), Web PWA, and CLI** — so a backup created on one platform can be restored on any other.

Watch OS is explicitly out of scope: it recovers via companion phone sync, not backup.

---

## Current State

### What's implemented

| Platform | Transport ID(s) registered | Google Drive? |
|----------|---------------------------|---------------|
| Android  | `local`, `gdrive-appdata` | ✅ Fully implemented |
| iOS      | `local`, `icloud`         | ❌ No, uses iCloud only |
| Desktop  | `local`                   | ❌ No, credentials present but unused |
| Web/Wasm | `local`                   | ❌ No |
| CLI      | `local`                   | ❌ No |

### Credential files per platform

| Platform | File | OAuth client_id |
|----------|------|-----------------|
| Android | `androidApp/src/main/assets/google-cloud-credentials.json` | `1015542852833-8l3u5il0o35qhb5noc3281uppqjpq00l` |
| Desktop | `composeApp/src/desktopMain/google-cloud-credentials.json` | `1015542852833-jglc153im0j68gptkdlai0dkf1ri458q` |
| iOS     | `iosApp/google-cloud-credentials.plist`                    | `1015542852833-fdda6v6e4v5mmif8s7npk9p66413c58r` |

All share GCP project `twofac-490000`.

---

## ✅ Good News: appDataFolder is scoped per GCP Project

Initial research suggested that Google Drive's `appDataFolder` might be isolated per OAuth Client ID, which would have forced us to use a single shared Client ID across all platforms.

However, deep research into the Google Drive API documentation confirms that **the `appDataFolder` is shared across all OAuth clients as long as they belong to the same Google Cloud Project ID.**

**Consequence**: We **can and must** use separate OAuth client IDs for iOS, Android, Desktop, and Web (as required by Google's OAuth policies for native apps/origins), while still being able to seamlessly read and write to the exact same `appDataFolder` for a given user.

### Action items for credentials

1. 🧑 **HUMAN — GCP Console**: Ensure all platform-specific client IDs exist under the same `twofac-490000` GCP Project.
   - Keep the existing Android client (`...8l3...`).
   - Keep the existing Desktop client (`...jgl...`).
   - Keep the existing iOS client (`...fdda...`).
   - Enable the Device Authorization Grant on the Desktop/CLI client if you want CLI support.

2. 🧑 **HUMAN — GCP Console**: Create a separate **Web application** type OAuth client under the same project `twofac-490000`, with the PWA's origin(s) set as authorized JavaScript origins (e.g. `https://twofac.app`). Note down the resulting web `client_id`.

3. 🤖 **AGENT**: Add the web `client_id` to the appropriate Wasm/web config location (e.g. a new `composeApp/src/wasmJsMain/google-cloud-credentials.json` or hardcoded constant in the TypeScript bridge).

4. 🤖 **AGENT**: Document the client IDs and the cross-platform sharing capabilities in `AGENTS.md`.

---

## Platform-by-platform implementation plan

### Android — Already implemented ✅

- `GoogleDriveAppDataBackupTransport` in `composeApp/src/androidMain/` is production-ready.
- Uses `https://www.googleapis.com/auth/drive.appdata` scope via Google Identity `AuthorizationClient`.
- Auth is GMS-backed (activity-based OAuth consent).

**Action needed:** None. The existing client ID works.

### iOS — Needs new Google Drive transport

Currently iOS only has `ICloudBackupTransport`. It has Google credentials in `iosApp/google-cloud-credentials.plist` but no Drive transport implementation.

**Required work:**

1. 🤖 **AGENT**: Implement `GoogleDriveAppDataBackupTransport` in `composeApp/src/iosMain/` using Ktor with Darwin HTTP engine and an injected `tokenProvider` (same pattern as the shared `GoogleDriveRestClient` refactor).

2. 🤖 **AGENT**: Add an `expect/actual` `GoogleDriveAuthProvider` interface — `iosMain` actual wraps `ASWebAuthenticationSession`-based OAuth flow to acquire access tokens and passes them to the Kotlin layer.

3. 🧑 **HUMAN — Xcode**: Add the `GoogleSignIn` Swift package (or `ASWebAuthenticationSession` entitlement) to `iosApp`. Add the `REVERSED_CLIENT_ID` URL scheme to `iosApp/Info.plist` so the OAuth redirect is captured by the app.

4. 🤖 **AGENT**: Confirm `iosApp/google-cloud-credentials.plist` has the correct iOS client ID.

5. 🤖 **AGENT**: Register `GOOGLE_DRIVE_APPDATA` transport in `composeApp/src/iosMain/kotlin/tech/arnav/twofac/di/IosModules.kt`.

### Desktop (JVM) — Has credentials, needs implementation

Desktop has `google-cloud-credentials.json` but `desktopBackupModule` only registers `local`.

**Required work:**

1. 🤖 **AGENT**: Implement `GoogleDriveAppDataBackupTransport` in `composeApp/src/desktopMain/` using the shared `GoogleDriveRestClient` with Ktor/OkHttp engine.

2. 🤖 **AGENT**: Implement the "installed app" loopback redirect OAuth flow in `desktopMain`:
   - Spin up a temporary `HttpServer` on an ephemeral localhost port to capture the authorization code.
   - Open the authorization URL in the system browser via `Desktop.getDesktop().browse(uri)`.
   - Exchange the auth code for access + refresh tokens.
   - Refresh tokens automatically before expiry.

3. 🤖 **AGENT**: Persist the refresh token via `MacOSKeychainBackend` on macOS. For Linux/Windows, store in the platform credential store (this may require a stub `UnsupportedDesktopSecureUnlockBackend` path for platforms without a keyring implementation yet).

4. 🤖 **AGENT**: Embed `google-cloud-credentials.json` as a classpath resource in the Desktop build (it is currently gitignored — the file must be present locally at build time, similar to Android assets).

5. 🤖 **AGENT**: Register `GOOGLE_DRIVE_APPDATA` in `desktopBackupModule` in `DesktopModules.kt`.

6. 🧑 **HUMAN**: Smoke-test the loopback OAuth flow on macOS, Linux, and Windows (browser opens, consent is given, token is stored, Drive backup round-trips).

> **Note on `client_secret`**: The Desktop credentials file already contains a `client_secret`. For "installed app" OAuth clients this is per-spec acceptable — RFC 6749 §2.1 classifies native apps as public clients where the secret is not confidential. No security action needed; just don't accidentally commit the file.

### Web/Wasm PWA — Needs new transport + web OAuth client

No Google Drive credentials or implementation exist for the web target.

**Required work:**

1. 🧑 **HUMAN — GCP Console**: Create a **Web application** type OAuth client under project `twofac-490000`. Set the PWA's origin(s) as authorized JavaScript origins (e.g. `https://twofac.app`). Note down the resulting `client_id`.

2. 🤖 **AGENT**: Add the web `client_id` to a new TypeScript config or constant file in `composeApp/src/wasmJsMain/typescript/src/`.

3. 🤖 **AGENT**: Implement a `google-auth.mts` TypeScript bridge that loads the Google Identity Services (GIS) library and exposes `requestDriveToken(): Promise<string>` via a `@JsModule` interop — same pattern as `backup.mts`, `crypto.mts`, etc.

4. 🤖 **AGENT**: Implement `GoogleDriveAppDataBackupTransport` in `composeApp/src/wasmJsMain/` using the shared `GoogleDriveRestClient` with the Ktor JS HTTP engine, fed by the GIS token provider.

5. 🤖 **AGENT**: Store access tokens in-memory only (tab lifetime). For the refresh token: encrypt it with the vault passkey and persist in the browser's IndexedDB (follow the same pattern as `storage.mts`).

6. 🤖 **AGENT**: Verify/update the PWA's Content Security Policy to allow `connect-src https://accounts.google.com https://www.googleapis.com`.

7. 🤖 **AGENT**: Register `GOOGLE_DRIVE_APPDATA` in the wasmJs DI module init.

8. 🧑 **HUMAN**: Smoke-test in a browser: OAuth popup opens, consent is given, backup round-trips to Drive.

### CLI — Needs device authorization grant flow

The CLI is a native binary (Kotlin/Native, no GMS, no browser API).

**Required work:**

1. 🧑 **HUMAN — GCP Console**: Confirm the Device Authorization Grant is enabled on the Desktop installed-app OAuth client (or whichever client is designated for CLI). In the GCP Console → OAuth client → edit → check "Enable Device Authorization Grant" (this is separate from the regular authorization code flow).

2. 🤖 **AGENT**: Implement the Device Authorization Grant polling flow in `cliApp/src/commonMain/`:
   - POST to `https://oauth2.googleapis.com/device/code` → receive `device_code` + `user_code` + `verification_url`.
   - Print to terminal: `Open https://google.com/device and enter code XXXX-XXXX`
   - Poll `https://oauth2.googleapis.com/token` until the user approves or timeout.
   - Exchange for access + refresh tokens.

3. 🤖 **AGENT**: Implement `GoogleDriveAppDataBackupTransport` in `cliApp/src/commonMain/` using the shared `GoogleDriveRestClient` with Ktor native HTTP engine.

4. 🤖 **AGENT**: Store the refresh token in the OS keyring:
   - macOS: `security add-generic-password` / Keychain API (via platform call already used in `MacOSKeychainBackend`).
   - Linux: `libsecret` / `secret-tool` (new platform call needed).
   - Windows: Windows Credential Manager (new platform call needed).

5. 🤖 **AGENT**: Add Drive subcommands to `BackupCommand.kt`:
   - `twofac backup export --provider=gdrive`
   - `twofac backup import --provider=gdrive`
   - `twofac backup list --provider=gdrive`
   - `twofac backup auth gdrive` (explicit login/token setup command)

6. 🧑 **HUMAN**: End-to-end test: run CLI auth flow, confirm Drive backup exports and can be restored on Android.

---

## Shared Transport Core — Refactoring opportunity

The existing Android `GoogleDriveAppDataBackupTransport` contains Drive REST logic (list, upload, download, delete) that is pure HTTP and could be extracted into a `commonMain` or shared non-platform-specific location.

**Recommended refactor** (do before implementing per-platform transports):

1. 🤖 **AGENT**: Extract a `GoogleDriveRestClient` into `composeApp/src/commonMain/` that accepts:
   - An injected `HttpClient` (Ktor; engine provided per platform)
   - An injected `tokenProvider: suspend () -> String`

2. 🤖 **AGENT**: Refactor Android's `GoogleDriveAppDataBackupTransport` to delegate to `GoogleDriveRestClient`.

3. 🤖 **AGENT**: All other platform transports (iOS, Desktop, CLI, Wasm) become thin wrappers: provide platform engine + platform OAuth token acquisition → `GoogleDriveRestClient` does the rest.

---

## Backup file naming — Cross-platform compatibility check

Current backup file names: `twofac-backup-{epochSeconds}-{suffix}.json`

- Android: sets `name = descriptor.id` on upload; filters by `BACKUP_FILE_PREFIX = "twofac-backup-"` on list.
- iOS (iCloud): uses `fileName` directly as backup ID.
- CLI: filters by `twofac-backup-` prefix and `.json` suffix.

The naming is already consistent. Since all platforms share the same `appDataFolder` (under the same GCP Project), they will all see and correctly parse each other's backup files. No format change needed.

---

## Token persistence per platform

| Platform | Where to store refresh token |
|----------|------------------------------|
| Android  | GMS manages token lifecycle automatically (no action needed) |
| iOS      | iOS Keychain via `KeychainHelper` (already used for session passkey) |
| Desktop macOS | `MacOSKeychainBackend` (already implemented) |
| Desktop Linux/Windows | Platform credential store (needs implementation in `DesktopSecureUnlockBackend`) |
| Web/Wasm | Encrypted in IndexedDB, key derived from vault passkey |
| CLI      | OS keyring via platform-specific native call |

---

## Prerequisites and pre-emption checklist

Before implementing any new Google Drive transports, the following must be resolved:

### Must-do before any cross-platform Drive work

- 🧑 **HUMAN — GCP Console**: Ensure all platform-specific client IDs exist under the same `twofac-490000` GCP Project.
- 🧑 **HUMAN — GCP Console**: Enable Device Authorization Grant on the Desktop/CLI client (needed for CLI).
- 🧑 **HUMAN — GCP Console**: Create the Web application OAuth client for Web/Wasm.
- 🤖 **AGENT**: Extract shared `GoogleDriveRestClient` into `composeApp/src/commonMain/` (do this first, before any per-platform work).

### Must-do per platform (in recommended order)

| Order | Platform | Owner |
|-------|----------|-------|
| 1 | Shared `GoogleDriveRestClient` refactor | 🤖 AGENT |
| 2 | iOS: transport + `ASWebAuthenticationSession` OAuth | 🤖 AGENT code + 🧑 HUMAN Xcode setup |
| 3 | Desktop: transport + loopback OAuth | 🤖 AGENT |
| 4 | CLI: transport + device auth | 🤖 AGENT |
| 5 | Web/Wasm: transport + GIS JS bridge | 🤖 AGENT |

### Nice-to-have (hardening)

- 🤖 **AGENT**: Add integration test that verifies two platform configurations with different client IDs (but same project) can both list each other's backup files.
- 🤖 **AGENT**: Ensure `BackupProviderCapabilityRules` in sharedLib correctly marks `gdrive-appdata` as available on each platform when registered.
- 🤖 **AGENT**: Document the credential file locations and the "cross-platform appDataFolder sharing" invariant in `AGENTS.md`.
- 🤖 **AGENT**: Verify/update CSP headers for Web/Wasm build to allow `accounts.google.com` and `googleapis.com`.
- 🤖 **AGENT**: Handle Drive quota errors (429) and partial failures gracefully in all transport implementations.
- 🧑 **HUMAN**: Cross-platform smoke test: create a backup on Android, restore it on Desktop and vice versa.

---

## Relationship to existing plan 13

This plan narrows down and expands Phase 3 of `13-backup-transports-rollout-plan.md`:

- Phase 3, item 20 ("Build a shared Drive transport core") → addressed by the shared `GoogleDriveRestClient` refactor above.
- Phase 3, item 23 ("Roll out by platform") → each platform is broken out in detail here.
- The initial concern about `appDataFolder`-per-client-ID isolation has been resolved via research, confirming that clients within the same GCP project share the `appDataFolder`. We can proceed safely.
