---
name: Common Secure-Unlock Genericity Plan
status: Proposed
progress:
  - "[x] Inspect common/session contracts and call-sites"
  - "[x] Compare wasmJs vs Android/iOS readiness semantics"
  - "[x] Assess scope to remove web-specific coupling from commonMain"
  - "[x] Propose generic contract directions"
  - "[ ] Phase 1 - Add generic secure-readiness contract in commonMain"
  - "[ ] Phase 2 - Implement readiness in wasmJs/androidMain/iosMain session managers"
  - "[ ] Phase 3 - Commonify ViewModel and screen APIs (remove WebAuthn naming)"
  - "[ ] Phase 4 - Unify Settings enrollment flow around SecureSessionManager"
  - "[ ] Phase 5 - Update tests and verify with composeApp checks"
---

# Common Secure-Unlock Genericity Research

## Question

Is there scope to make `commonMain` secure-unlock code generic (not WebAuthn-specific), while still supporting Web/Wasm plus Android/iOS biometric unlock readiness?

## Short answer

Yes — there is clear scope, and the current state has WebAuthn-specific coupling in `commonMain` that can be replaced with generic secure-unlock semantics.

## What is web-specific today (in commonMain)

1. `AccountsViewModel` imports and casts to `WebAuthnSessionManager`:
   - `isWebAuthnUnlockReady()` calls `(sessionManager as? WebAuthnSessionManager)?.isPasskeyEnrolled()`
   - post-unlock enrollment helper has WebAuthn-only branching
2. `HomeScreen` and `AccountsScreen` use `isWebAuthnReady` naming and behavior.
3. `HomeLockedState` exposes `onWebAuthnUnlock` callback in a common UI component.
4. `SettingsScreen` contains `webAuthnSessionManager` casting and a dedicated WebAuthn enrollment dialog path.

Result: common flows are partially shaped around one platform implementation type.

## What is already generic and good

1. `SessionManager` is platform-agnostic and stable.
2. `SecureSessionManager` already exists as shared abstraction and is bound across all targets in DI:
   - wasm: `WebAuthnSessionManager -> SecureSessionManager -> SessionManager`
   - android/ios: `BiometricSessionManager -> SecureSessionManager -> SessionManager`
3. Platform-specific APIs stay in platform source sets (`wasmJsMain`, `androidMain`, `iosMain`), which is aligned with KMP conventions.

## Platform readiness semantics (research + code)

### Web/Wasm

- Uses support checks + capability checks + enrollment state (`isPasskeyEnrolled`) before offering auto-unlock.
- Underlying API readiness signals are capability/enrollment oriented (`PublicKeyCredential` support, user-verifying authenticator availability).

### Android

- Uses `BiometricManager.canAuthenticate(...)` for capability availability.
- Current session manager has no explicit “enrolled passkey blob exists” readiness method in the shared contract.

### iOS

- Uses `LAContext.canEvaluatePolicy(...)` for capability availability.
- Current manager also lacks shared-contract readiness for “secure material enrolled and immediately usable”.

### Cross-platform conclusion

All platforms have a concept of:
1. capability available
2. feature enabled by user
3. secure material enrolled/ready for fast unlock

Only web currently exposes (3) in shared API (`isPasskeyEnrolled()`), hence common code drifted web-specific.

## Why `isWebAuthnReady` in commonMain is an anti-pattern

It leaks one backend implementation name into:
- common ViewModel API (`isWebAuthnUnlockReady`)
- common UI state/props (`isWebAuthnReady`, `onWebAuthnUnlock`)

This makes future parity work (Android/iOS “ready” checks) harder and encourages more `as? WebAuthnSessionManager` branching.

## Scope assessment

**Scope to genericize: high.**

This can be done without moving platform API calls into `commonMain`:
- keep platform details in platform source sets
- expose generic readiness/enrollment semantics through `SecureSessionManager`
- keep shared UI/ViewModel code capability-driven, not implementation-type-driven

Effort is moderate (multi-file refactor), but conceptually straightforward.

## Recommended generic direction

Introduce shared readiness semantics on `SecureSessionManager`, e.g.:

1. `fun isSecureUnlockReady(): Boolean`
   - meaning: secure auto-unlock can be attempted now without manual passkey entry
2. `fun needsSecureEnrollment(): Boolean` (or derive it)
   - meaning: secure feature enabled+available but enrollment/material setup still missing

Implementation expectations:
- Web: ready iff credential + encrypted blob are present and feature enabled.
- Android: ready iff biometric enabled + encrypted passkey(+IV) exists (and key is usable).
- iOS: ready iff biometric/secure mode enabled + keychain passkey exists.

Then replace common WebAuthn-specific names:
- `isWebAuthnUnlockReady()` -> `isSecureUnlockReady()`
- `onWebAuthnUnlock` -> `onSecureUnlock`
- UI labels can still be platform-tailored in platform-aware code paths, but shared control logic should use generic secure semantics.

## Execution plan to make commonification happen

### Phase 1 - Add generic secure-readiness contract in `commonMain`

1. Update `SecureSessionManager` to expose a generic readiness signal:
   - `fun isSecureUnlockReady(): Boolean`
   - meaning: secure auto-unlock can be attempted immediately (without manual passkey entry).
2. Keep `WebAuthnSessionManager` for wasm-specific internals, but remove need for `commonMain` callers to depend on it for readiness checks.
3. Add KDoc that clearly separates:
   - availability (`isSecureUnlockAvailable`)
   - enabled preference (`isSecureUnlockEnabled`)
   - effective readiness (`isSecureUnlockReady`)

### Phase 2 - Implement readiness in platform managers

1. `BrowserSessionManager` (`wasmJsMain`)
   - Implement `isSecureUnlockReady()` as:
     `isSecureUnlockEnabled && isSecureUnlockAvailable && enrolledCredentialPresent && encryptedBlobPresent`.
2. `AndroidBiometricSessionManager` (`androidMain`)
   - Implement `isSecureUnlockReady()` as:
     `isBiometricEnabled && isBiometricAvailable && encryptedPasskey+IV present`.
   - Keep biometric prompt logic platform-specific; only expose boolean readiness to shared code.
3. `IosBiometricSessionManager` (`iosMain`)
   - Implement `isSecureUnlockReady()` as:
     `isBiometricEnabled && isBiometricAvailable && savedPasskeyPresent`.
   - Preserve current storage behavior in this refactor; do not mix with a separate keychain-hardening change.

### Phase 3 - Commonify ViewModel and screen APIs

1. `AccountsViewModel`
   - Replace `isWebAuthnUnlockReady()` with `isSecureUnlockReady()` using `SecureSessionManager` cast.
   - Update post-unlock enrollment helper to avoid WebAuthn-type checks where generic checks suffice.
2. `HomeScreen` / `AccountsScreen`
   - Rename local state from `isWebAuthnReady` to `isSecureUnlockReady`.
   - Preserve behavior: if secure path is not ready, show manual passkey dialog.
3. `HomeLockedState`
   - Rename callback from `onWebAuthnUnlock` to `onSecureUnlock`.
   - Keep UI copy neutral in common code (platform-specific labels can stay configurable from caller).

### Phase 4 - Unify Settings flow around `SecureSessionManager`

1. In `SettingsScreen`, use `SecureSessionManager` as primary abstraction for secure enrollment toggle flow.
2. Keep `BiometricSessionManager` usage only for biometric-specific extra toggle/details.
3. Keep WebAuthn-specific copy and edge-case messaging behind optional checks, but avoid shaping main flow around `WebAuthnSessionManager` type in common code.
4. Ensure disable path still clears secure data and resets toggles consistently across platforms.

### Phase 5 - Tests and verification

1. `commonTest`
   - Update `AccountsViewModelSessionManagerTest` to assert generic secure-readiness behavior.
   - Replace/limit `FakeWebAuthnSessionManager` usage where generic secure fakes are sufficient.
2. `wasmJsTest`
   - Keep WebAuthn-specific tests (`isPasskeyEnrolled`, capability mapping, encrypt/decrypt lifecycle).
   - Add/adjust test coverage for `isSecureUnlockReady()` mapping.
3. `iosTest` / Android tests
   - Add readiness-focused tests for biometric managers where feasible.
4. Validate with:
   - `./gradlew --no-daemon :composeApp:desktopTest :composeApp:compileKotlinMetadata`
   - plus any impacted platform-specific tests already present in the repo.

### Acceptance criteria

1. No `isWebAuthnReady`/`isWebAuthnUnlockReady` naming remains in `commonMain` screens/viewmodels.
2. Common unlock flow decisions use `SecureSessionManager` readiness semantics.
3. Web-specific APIs remain inside `wasmJsMain`.
4. Android/iOS participate in the same generic readiness contract (even with different internals).

## Risks / caveats

1. Android/iOS managers currently conflate “enabled” and “ready” in places; splitting these states may surface existing behavior assumptions.
2. iOS implementation currently stores passkey via `NSUserDefaults` placeholder methods (`saveToKeychain` naming mismatch), so readiness semantics should be defined carefully before tightening behavior.
3. `remember { viewModel.isWebAuthnUnlockReady() }` in screens is a snapshot; if readiness becomes more dynamic, state observation strategy should be revisited.

## Final finding

There is strong and legitimate scope to remove web-specific naming/branching from `commonMain`.

The existing `SecureSessionManager` abstraction is already the right backbone; adding a generic “secure unlock readiness” concept would let common code support Web, Android, and iOS uniformly without leaking WebAuthn-specific types into shared flows.
