---
name: Web Device-Credential Unlock for Extension/PWA
status: Planned
progress:
  - "[ ] Phase 0 - Finalize threat model, scope, and fallback UX"
  - "[ ] Phase 1 - Add common abstractions and capability model in commonMain"
  - "[ ] Phase 2 - Implement wasmJs WebAuthn device-credential provider"
  - "[ ] Phase 3 - Implement wasmJs secure key wrapping and encrypted passkey storage"
  - "[ ] Phase 4 - Wire auto-unlock and enrollment flows through SessionManager"
  - "[ ] Phase 5 - Add Settings opt-in UX and migration from localStorage plaintext"
  - "[ ] Phase 6 - Extension/PWA runtime integration and policy hardening"
  - "[ ] Phase 7 - Add tests, compatibility matrix, and observability"
  - "[ ] Phase 8 - Validate manually and roll out with safe defaults"
---

# Web Device-Credential Unlock for Extension/PWA

## Goal

Add opt-in, device-credential-based unlock for the web target (browser extension + PWA) so users can unlock saved passkey material via platform biometrics/passkey prompts instead of retyping passkey each time. Keep current manual passkey flow as the required fallback and preserve shared architecture patterns already used for mobile biometric auth.

## Context from existing mobile plan

From `04-biometric-auth-mobile.md`, these principles should stay unchanged:

1. Opt-in only; manual passkey always available.
2. Passkey remains source-of-truth for `twoFacLib.unlock(passkey)`.
3. sharedLib stays platform-agnostic; credential logic stays in platform source sets.
4. SessionManager boundary remains the main integration seam.
5. Failure/cancel on device credential prompt falls back to passkey dialog.

## Web security model and API direction (research summary)

1. `PasswordCredential` from Credential Management API is primarily password autofill UX and not a secure local secret-protection primitive; do not use it for vault key protection.
2. WebAuthn (`PublicKeyCredential`) is secure-context-only and origin-bound; use it to gate key access with user verification (`userVerification = "required"`).
3. For encryption-key derivation from passkeys, use WebAuthn PRF extension when available; gate with `PublicKeyCredential.getClientCapabilities()` and runtime extension outputs.
4. If PRF is unavailable, use a fallback path that still requires user verification before unwrapping a locally protected key (see Phase 3 fallback design).
5. For extension mode, avoid persistent plaintext secrets; prefer in-memory/session storage and encrypted-at-rest blobs for anything persistent.
6. For MV3 extension-specific runtime, prefer `chrome.storage.session` for short-lived sensitive material and restrict content script access.

## Target architecture

Keep shared code mostly identical between extension and PWA by introducing one wasmJs provider that relies on standards-based browser APIs and only a thin optional extension adapter:

1. **commonMain**
   - Keep `SessionManager`/`BiometricSessionManager` contracts as main API.
   - Add platform-neutral `DeviceCredentialSessionManager` naming only if needed for clarity (or keep `BiometricSessionManager` to avoid churn).
   - Keep HomeScreen/ViewModel unlock choreography unchanged except new web implementation behavior.

2. **wasmJsMain**
   - `BrowserDeviceCredentialSessionManager` (new) implementing session + enrollment behavior.
   - `WebAuthnClient` interop layer (`@JsFun`) for:
     - capability checks
     - credential registration (`create`)
     - credential assertion (`get`)
     - extension result extraction (`getClientExtensionResults`)
   - `EncryptedPasskeyStore` using IndexedDB/KStore for encrypted payload + metadata.
   - Optional `ExtensionSessionStore` adapter for `chrome.storage.session` (runtime-detected, no browser-brand lock-in in core code).

3. **Storage shape**
   - Never persist plaintext passkey.
   - Persist only encrypted passkey blob + IV/nonce + KDF/version metadata + credential identifier reference.
   - Keep ephemeral unwrapped key/passkey only in memory and zeroize references quickly after use.

## Detailed phase-wise implementation plan

### Phase 0 - Finalize threat model, scope, and fallback UX

1. Define in-scope threat reduction: eliminate plaintext-at-rest passkey storage in web target.
2. Keep out-of-scope explicit: full anti-XSS guarantees are impossible in hostile extension/PWA JS context; focus on reducing exposure window and storage compromise risk.
3. Decide lock lifetime policy:
   - option A: unlock for popup/app session only
   - option B: unlock until browser restart
   - option C: configurable timeout
4. Define fallback UX requirements:
   - if device credential unavailable/denied/cancelled -> show passkey dialog
   - if user does not opt in -> manual passkey flow unchanged
5. Define migration policy for existing plaintext localStorage entries (one-time read -> re-enroll -> delete legacy key).

### Phase 1 - Add common abstractions and capability model in commonMain

1. Reuse existing `BiometricSessionManager` interface where possible to avoid broad refactor.
2. Add web-relevant capability surface (if needed) in common contract:
   - availability
   - enrollment support
   - current enrollment state
   - optional reason codes for unsupported environments.
3. Add sealed result models in commonMain for enrollment/unlock attempts:
   - `Success(passkey)`
   - `Cancelled`
   - `Unavailable(reason)`
   - `Failed(errorMessage)`
4. Keep current `suspend getSavedPasskey()` compatibility by mapping detailed results internally while preserving existing call sites.

### Phase 2 - Implement wasmJs WebAuthn device-credential provider

1. Create `WebAuthnCapabilityChecker`:
   - verify `window.PublicKeyCredential` presence
   - probe `isUserVerifyingPlatformAuthenticatorAvailable()`
   - probe `getClientCapabilities()` for extension support (including PRF when exposed).
2. Add registration flow helper:
   - create discoverable credential for app RP (same origin rules for PWA and extension origin)
   - request `userVerification = required`
   - request PRF enablement via `extensions.prf = {}` when supported.
3. Add assertion flow helper:
   - call `navigator.credentials.get()` only from explicit user action or safe flow point
   - set `userVerification = required`
   - request PRF evaluation salt(s) when available.
4. Normalize browser differences behind Kotlin wrappers so core app code never branches on Chrome/Firefox/Safari directly.

### Phase 3 - Implement wasmJs secure key wrapping and encrypted passkey storage

1. Preferred path (PRF-capable clients):
   - derive PRF output during authenticated assertion
   - feed PRF output into HKDF (`WebCrypto`) with domain-separated `info`
   - derive AES-GCM key for encrypt/decrypt of stored passkey blob.
2. Fallback path (no PRF support):
   - keep manual passkey only OR
   - gated key-wrap mode using WebCrypto non-extractable key + mandatory WebAuthn assertion before decrypt operation, if feasible on target browsers.
   - decide one path in Phase 0 and document security tradeoff.
3. Store encrypted blob in IndexedDB/KStore with versioned schema:
   - `version`, `ciphertext`, `nonce`, `saltId`, `credentialId`, `createdAt`, `updatedAt`.
4. Use `chrome.storage.session` only for ephemeral unlocked material in extension runtime when available; clear on restart/unload.
5. Implement explicit secure-clear paths on disable/logout/failure.

### Phase 4 - Wire auto-unlock and enrollment flows through SessionManager

1. Replace `BrowserSessionManager` binding in wasm Koin module with new device-credential manager.
2. Preserve HomeScreen flow semantics:
   - auto-attempt saved unlock
   - on null/failure -> passkey dialog
   - on successful manual unlock + opt-in -> enroll path.
3. Ensure `savePasskey(passkey)` no longer writes plaintext:
   - if device credential enabled: encrypt and persist blob
   - if only remember-passkey enabled without device credential: keep policy decided in Phase 0 (recommended: disable plaintext mode by default, optional compatibility toggle only behind warning).
4. Keep `clearPasskey()` removing encrypted blob + ephemeral session state.

### Phase 5 - Add Settings opt-in UX and migration from localStorage plaintext

1. Show device-credential toggle only when capability checker says available.
2. Enrollment UX:
   - user enables toggle
   - ask passkey once (reuse existing dialog)
   - run credential enrollment + encrypted storage
   - success -> set enabled; failure/cancel -> keep disabled + user-visible message.
3. Add explanatory copy for web:
   - "Use device credential (passkey/biometrics) to unlock saved passkey."
   - "If unavailable or cancelled, you'll enter passkey manually."
4. One-time migration:
   - detect old plaintext storage key
   - after successful enrollment, delete plaintext key
   - if enrollment skipped, keep behavior but surface warning until migrated.

### Phase 6 - Extension/PWA runtime integration and policy hardening

1. Keep implementation standards-based first so PWA and extension share logic.
2. Extension hardening:
   - enforce strict extension CSP
   - limit permissions
   - ensure sensitive storage not exposed to content scripts.
3. PWA hardening:
   - maintain HTTPS-only deployment
   - ensure secure-context checks and graceful unsupported UX.
4. Ensure any required Permissions-Policy settings for WebAuthn operations are documented for embedded contexts (if future iframe usage appears).

### Phase 7 - Add tests, compatibility matrix, and observability

1. Unit tests in commonMain for SessionManager behavior contract and fallback transitions.
2. wasmJs tests for:
   - capability detection mapping
   - encrypted blob serialization/version migration
   - enable/disable and clear behavior.
3. Manual compatibility matrix:
   - Chrome extension popup
   - Chrome PWA installed + tab mode
   - Firefox extension
   - Safari PWA (where supported).
4. Add non-sensitive diagnostics for unlock outcome categories (cancelled/unavailable/failed) to improve support without logging secret data.

### Phase 8 - Validate manually and roll out with safe defaults

1. Manual test checklist:
   - first-time opt-in success
   - cancelled prompt fallback
   - disabled toggle + manual passkey path
   - restart/browser relaunch behavior
   - migration from existing plaintext value
   - extension and PWA parity.
2. Default behavior for first rollout:
   - keep feature opt-in
   - keep clear warning if only legacy remember mode exists
   - prefer secure mode where supported.
3. Update README/Settings copy with explicit browser support and fallback behavior.

## Files likely impacted in implementation

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/session/SessionManager.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/session/BiometricSessionManager.kt` (or web-capable counterpart)
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/session/BrowserSessionManager.kt` (replace/refactor)
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/di/WasmModules.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/HomeScreen.kt` (minimal flow adjustments only if needed)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModel.kt` (only if richer result handling is introduced)

## Decision log (to finalize before implementation)

1. PRF fallback behavior when unsupported:
   - strict mode: no persisted encrypted passkey, manual-only fallback
   - compatibility mode: weaker encrypted local mode gated by WebAuthn assertion.
2. Whether to rename `BiometricSessionManager` to a broader `DeviceCredentialSessionManager` now or keep existing type for minimal churn.
3. Unlock session lifetime and re-auth prompt frequency.

## References used

- Existing mobile architecture plan: `.agents/plans/04-biometric-auth-mobile.md`
- MDN WebAuthn overview: https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API
- MDN WebAuthn extensions: https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API/WebAuthn_extensions
- MDN Credential Management API: https://developer.mozilla.org/en-US/docs/Web/API/Credential_Management_API
- MDN CredentialsContainer.get/create:  
  https://developer.mozilla.org/en-US/docs/Web/API/CredentialsContainer/get  
  https://developer.mozilla.org/en-US/docs/Web/API/CredentialsContainer/create
- MDN PublicKeyCredential capability APIs:  
  https://developer.mozilla.org/en-US/docs/Web/API/PublicKeyCredential/getClientCapabilities_static  
  https://developer.mozilla.org/en-US/docs/Web/API/PublicKeyCredential/isUserVerifyingPlatformAuthenticatorAvailable_static
- Chrome Extensions storage API (`storage.session` / `storage.local`):  
  https://developer.chrome.com/docs/extensions/reference/api/storage
- Yubico PRF developer guide:  
  https://developers.yubico.com/WebAuthn/Concepts/PRF_Extension/Developers_Guide_to_PRF.html

