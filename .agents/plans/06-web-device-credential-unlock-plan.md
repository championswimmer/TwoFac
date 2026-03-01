---
name: Web Device-Credential Unlock for Extension/PWA
status: In Progress
progress:
   - "[x] Phase 0 - Apply locked scope and interface hierarchy"
   - "[x] Phase 1 - Add secure session abstractions in commonMain"
   - "[x] Phase 2 - Implement wasmJs WebAuthnSessionManager secure unlock path with library-backed interop"
  - "[ ] Phase 3 - Persist encrypted passkey and keep decrypted key session-only"
  - "[ ] Phase 4 - Wire Settings/Home unlock flows (secure-only, no legacy plaintext)"
  - "[ ] Phase 5 - Add tests and hardening"
---

# Web Device-Credential Unlock for Extension/PWA

## Goal

Use WebAuthn as the secure gate for web unlock so passkeys are never stored in plaintext, while keeping the existing `SessionManager` contract as the main app integration seam.

## Scope decisions (updated)

1. Keep the core `SessionManager` interface unchanged.
2. Introduce a new intermediate `SecureSessionManager` interface for encrypted passkey handling.
3. Update `BiometricSessionManager` to inherit from `SecureSessionManager` (mobile path remains conceptually the same).
4. Add `WebAuthnSessionManager` inheriting from `SecureSessionManager` for web (PWA + extension).
5. No backward compatibility with current insecure plaintext `localStorage` passkey storage.
6. No migration workflow from legacy plaintext keys is required because this app is pre-release.

## WebAuthn/API direction (research-backed)

1. WebAuthn (`PublicKeyCredential`) is origin-bound and secure-context only, making it the right browser primitive for user-verifying unlock prompts.
2. WebAuthn does not directly store arbitrary app secrets; use authenticated WebAuthn operations to gate key derivation and unwrapping.
3. Preferred key path: request PRF extension support, obtain PRF output during authenticated `navigator.credentials.get()`, then run HKDF/WebCrypto to derive an AES-GCM wrapping key.
4. Persist only encrypted passkey payload + metadata (never plaintext passkey).
5. For extension runtime, keep decrypted values in memory/session only (`chrome.storage.session` when needed), and keep persistent storage encrypted.
6. If required secure capabilities are unavailable, disable secure remember mode and fall back to manual passkey entry only (not plaintext persistence).
7. Browser-native JSON helpers (`PublicKeyCredential.parseCreationOptionsFromJSON`, `parseRequestOptionsFromJSON`, `.toJSON()`) are now broadly available and should be preferred over deprecated legacy wrappers.

## Candidate libraries (trusted/popular) and decisions

1. **`@simplewebauthn/browser` (SimpleWebAuthn by MasterKale, MIT)**
   - High-level WebAuthn ceremony helper (`startRegistration`, `startAuthentication`).
   - Popular usage signal (approx. 3.8M npm downloads/month at research time).
   - Decision: keep as an optional dependency if it materially reduces wasm interop boilerplate.
2. **`@github/webauthn-json` (GitHub)**
   - Trusted provenance but explicitly deprecated by maintainers in favor of native browser JSON helpers.
   - Decision: do **not** add as a new dependency.
3. **`webextension-polyfill` (Mozilla, MPL-2.0)**
   - Promise-based `browser.*` API wrapper with broad extension ecosystem adoption.
   - Popular usage signal (approx. 4.7M npm downloads/month; ~3k GitHub stars at research time).
   - Decision: adopt for extension storage/runtime access to reduce custom JS glue.
4. **`idb` (Jake Archibald, ISC)**
   - Lightweight and popular IndexedDB wrapper for modern browsers.
   - Popular usage signal (approx. 55M npm downloads/month; ~7k GitHub stars at research time).
   - Decision: optional fallback if KStore web backend is insufficient for required encrypted-blob operations.
5. **Important limitation**
   - No mainstream library fully provides "passkey-protected local secret vault" end-to-end.
   - We still must compose WebAuthn + WebCrypto key derivation + encrypted persistence ourselves.

## Kotlin/Wasm JS library integration approach (recommended)

1. Add npm dependencies in `composeApp/build.gradle.kts` under `wasmJsMain.dependencies` using `implementation(npm("pkg", "version"))`.
2. Create dedicated interop files in `composeApp/src/wasmJsMain/kotlin/.../interop/` using `@file:JsModule`, optional `@JsNonModule`, and `external` declarations.
3. Keep raw `js()` / `@JsFun` usage minimal and isolated to interop adapters only; avoid scattered inline snippets in domain/session classes.
4. Wrap external APIs behind Kotlin adapters (for example `WebAuthnClient`, `WebStorageClient`) and keep `WebAuthnSessionManager` itself Kotlin-first.
5. Keep `commonMain` and `SecureSessionManager` contracts free of JS-specific types (`JsAny`, `Promise`) and map all platform details inside `wasmJsMain`.

## Target interface hierarchy

```kotlin
interface SessionManager {
    fun isAvailable(): Boolean
    fun isRememberPasskeyEnabled(): Boolean
    fun setRememberPasskey(enabled: Boolean)
    suspend fun getSavedPasskey(): String?
    fun savePasskey(passkey: String)
    fun clearPasskey()
}

interface SecureSessionManager : SessionManager {
    fun isSecureUnlockAvailable(): Boolean
    fun isSecureUnlockEnabled(): Boolean
    fun setSecureUnlockEnabled(enabled: Boolean)
    suspend fun enrollPasskey(passkey: String): Boolean
}

interface BiometricSessionManager : SecureSessionManager

interface WebAuthnSessionManager : SecureSessionManager
```

Notes:
- `SessionManager` stays stable for existing call sites.
- `SecureSessionManager` centralizes secure-enrollment/secure-unlock semantics across mobile and web.
- Platform-specific interfaces remain free to expose additional capability helpers later, but plan starts with this minimal hierarchy.

## Implementation roadmap

### Phase 0 - Apply locked scope and interface hierarchy

1. Confirm strict policy: no plaintext passkey persistence on web.
2. No backward compatibility/migration for legacy plaintext `localStorage` passkey storage (pre-release product).
3. Keep the core `SessionManager` interface unchanged for existing app flow integration.
4. Introduce `SecureSessionManager` as the encrypted-passkey abstraction.
5. Keep `BiometricSessionManager : SecureSessionManager` and add `WebAuthnSessionManager : SecureSessionManager`.
6. Unsupported secure capability => manual passkey entry only.

### Phase 1 - Add secure session abstractions in commonMain

1. Add `SecureSessionManager` in `composeApp/src/commonMain/.../session`.
2. Update `BiometricSessionManager` to extend `SecureSessionManager`.
3. Add `WebAuthnSessionManager` interface in commonMain.
4. Keep `SessionManager` method signatures and behavior contract unchanged.
5. Keep shared/domain modules platform-agnostic.

### Phase 2 - Implement wasmJs WebAuthn secure unlock path with library-backed interop

1. Replace/refactor `BrowserSessionManager` into a `WebAuthnSessionManager` implementation.
2. Build a wasm-only JS interop layer:
   - Add/validate npm dependencies in `wasmJsMain` (`webextension-polyfill`; optional `@simplewebauthn/browser` if chosen).
   - Add `@JsModule`-based external declarations in dedicated interop files.
   - Replace direct `localStorage` `@JsFun` calls with adapter usage.
3. Capability checks:
   - `window.PublicKeyCredential` presence
   - user-verifying authenticator availability checks
   - client extension capability probing (including PRF where available)
4. Enrollment:
   - perform `navigator.credentials.create()` with user verification requirements
   - enable PRF extension during credential creation when supported
5. Unlock:
   - perform `navigator.credentials.get()` with user verification required
   - read extension outputs (`getClientExtensionResults`) and assertion data as needed
6. Return explicit failure/cancel/unavailable outcomes to caller so UI can fall back to manual passkey prompt.

### Phase 3 - Persist encrypted passkey and keep decrypted key session-only

1. Derive encryption material from authenticated WebAuthn flow (PRF path preferred).
2. Use WebCrypto (HKDF + AES-GCM) to encrypt/decrypt passkey.
3. Persist only encrypted blob + metadata (`version`, `credentialId`, `nonce`, `ciphertext`, timestamps).
4. Use KStore web storage first; if needed, use `idb` for explicit IndexedDB transaction/control paths.
5. Keep decrypted passkey only in memory/session-lifetime storage and clear on logout/disable/app close/restart boundaries where feasible.

### Phase 4 - Wire Settings/Home unlock flows without legacy plaintext compatibility

1. Keep Home/ViewModel unlock choreography anchored on `SessionManager` calls.
2. On manual unlock success with secure toggle enabled, call secure enrollment path.
3. On auto-unlock attempt:
   - secure success -> unlock app
   - cancel/unavailable/failure -> show manual passkey prompt
4. Remove legacy plaintext `localStorage` passkey read/write paths from web implementation.
5. Do not implement one-time migration from old plaintext keys.

### Phase 5 - Add tests and hardening

1. Common tests: `SessionManager` contract remains stable.
2. wasmJs tests:
   - capability mapping
   - enrollment success/failure/cancel handling
   - encrypted blob encode/decode and clear paths
3. Manual checks:
   - Chrome extension popup
   - Chrome PWA installed + tab mode
   - Unsupported browser behavior (manual-only fallback)
4. Security checks:
   - verify no plaintext passkey is persisted
   - verify secure toggle off clears encrypted and session-resident passkey material

## Files likely impacted during implementation

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/session/SessionManager.kt` (contract retained, docs may be updated)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/session/SecureSessionManager.kt` (new)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/session/BiometricSessionManager.kt` (extends `SecureSessionManager`)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/session/WebAuthnSessionManager.kt` (new)
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/session/BrowserSessionManager.kt` (replace/refactor)
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/session/interop/*.kt` (new JS-module external declarations/adapters)
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/di/WasmModules.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/HomeScreen.kt` (if flow adjustments are needed)
- `composeApp/build.gradle.kts` (wasmJs npm dependencies)

## Locked implementation defaults

1. Web persistent storage must contain encrypted data only:
   - PWA path: IndexedDB-backed storage abstraction.
   - Extension path: `chrome.storage.local` for encrypted blob persistence.
2. Decrypted passkey is session-only:
   - in-memory by default.
   - extension runtime may mirror session-only data in `chrome.storage.session` when needed.
   - clear on logout, disable, extension reload, and browser restart boundaries.
3. No plaintext fallback and no plaintext migration path.
4. Keep `SecureSessionManager` API surface minimal for the first iteration (`Boolean`/nullable results), and map richer platform outcomes internally.
5. JS dependency defaults:
   - prefer native WebAuthn + native JSON parse/toJSON helpers.
   - adopt `webextension-polyfill` for extension storage/runtime integration.
   - keep `@simplewebauthn/browser` and `idb` as opt-in if implementation complexity warrants them.

## References used

- MDN Web Authentication API: https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API
- MDN WebAuthn extensions and extension outputs: https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API/WebAuthn_extensions
- MDN `PublicKeyCredential.getClientCapabilities()`: https://developer.mozilla.org/en-US/docs/Web/API/PublicKeyCredential/getClientCapabilities_static
- Chrome Extensions Storage API (`storage.session`, `storage.local`): https://developer.chrome.com/docs/extensions/reference/api/storage
- W3C WebAuthn Level 3 PRF extension: https://w3c.github.io/webauthn/#sctn-prf-extension
- Yubico PRF implementation guide (PRF + HKDF usage patterns): https://developers.yubico.com/WebAuthn/Concepts/PRF_Extension/Developers_Guide_to_PRF.html
- Kotlin/Wasm JS interop docs: https://kotlinlang.org/docs/wasm-js-interop.html
- Kotlin JS module interop docs (`@JsModule`/`@JsNonModule`): https://kotlinlang.org/docs/js-modules.html
- Kotlin npm dependency setup docs: https://kotlinlang.org/docs/js-project-setup.html#npm-dependencies
- SimpleWebAuthn browser package docs: https://simplewebauthn.dev/docs/packages/browser/
- GitHub `@github/webauthn-json` deprecation note: https://github.com/github/webauthn-json
- Mozilla webextension polyfill docs: https://github.com/mozilla/webextension-polyfill
- `idb` IndexedDB wrapper docs: https://github.com/jakearchibald/idb
