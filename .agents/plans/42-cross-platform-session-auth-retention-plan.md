---
name: Cross-Platform Session-Scoped Auth Retention Plan
status: In Progress
progress:
  - "[x] Re-evaluate browser-extension plan against shared KMP architecture"
  - "[x] Inspect current common/session contracts and platform managers"
  - "[x] Phase 1 - Add common session-auth-retention contract in commonMain"
  - "[x] Phase 2 - Add shared session-passkey-cache abstraction with default in-memory implementation"
  - "[ ] Phase 3 - Refactor platform session managers to use the shared retention policy"
  - "[ ] Phase 4 - Implement browser-extension backend using extension storage.session"
  - "[ ] Phase 5 - Expose a common settings/UI flow with platform capability gating"
  - "[ ] Phase 6 - Add tests and cross-platform verification"
---

# Cross-Platform Session-Scoped Auth Retention Plan

## Re-evaluation result

The existing browser-specific plan (`41-browser-extension-session-auth-plan.md`) is **directionally correct**, but it is **not yet shared enough** for the product direction you just described.

Right now, plan 41 mainly does this:
- adds extension-specific `storage.session` interop,
- teaches `BrowserSessionManager` to reuse that cache,
- adds an extension-only settings toggle.

That is good for Chrome/Firefox popup behavior, but it places too much of the **policy** inside the wasm/browser implementation.

Your desired architecture is broader:
- the **policy** of “prompt every time vs remember for the current session” should live in shared KMP/common code,
- while each platform provides its own **session-retention backend**.

That means plan 41 should become a **platform backend slice** of a broader shared plan, not the whole design.

## What is already in good shape

The codebase is already closer to this architecture than it first looked.

### Shared contracts already exist

In `composeApp/src/commonMain/kotlin/tech/arnav/twofac/session/`:
- `SessionManager`
- `SecureSessionManager`
- `BiometricSessionManager`

These are already the right common seams for unlock behavior.

### Shared UI/control flow already exists

In common code:
- `AccountsViewModel` calls `sessionManager?.getSavedPasskey()` and `savePasskey(...)`
- `SettingsViewModel` already drives unlock settings from shared code
- `SettingsScreen` already renders unlock configuration from common state

So the app already centralizes most unlock orchestration in commonMain.

### Platform managers already fit the model

Current platform implementations:
- `BrowserSessionManager` (wasm/web)
- `AndroidBiometricSessionManager`
- `IosBiometricSessionManager`
- `DesktopBiometricSessionManager`

All of them already implement shared session contracts.

So this is **not** a case where we need a brand new architecture. We mainly need to add one missing shared concept.

## What is missing today

There is currently **no shared concept** for:

> “After secure authentication succeeds, may the decrypted passkey be retained for the current runtime session, and if so where?”

### Current gap in the contracts

`SessionManager` currently models:
- availability
- whether remembering is enabled
- save/get/clear passkey

But it does **not** explicitly distinguish between:
1. secure enrollment/persistence being enabled, and
2. temporary retention of an already-authenticated passkey for the current session.

### Current gap in implementations

- `BrowserSessionManager` has a `sessionPasskey` field, but it is only in-memory and not reused across popup reopens.
- Android/iOS/Desktop managers can securely store/retrieve passkeys, but there is no shared opt-in mode for **current-session-only reuse** of an already-authenticated unlock.
- The browser-specific plan currently makes this a wasm-only concern instead of a shared secure-unlock concern.

## Recommended shared architecture

## 1) Add a common retention policy concept

Add a common enum in `commonMain`, something like:

```kotlin
enum class SecureUnlockRetentionPolicy {
    PROMPT_EVERY_TIME,
    RETAIN_FOR_CURRENT_SESSION,
}
```

This is the missing shared product concept.

Meaning:
- `PROMPT_EVERY_TIME` = successful secure unlock is not reused; the next lock/open asks again
- `RETAIN_FOR_CURRENT_SESSION` = successful secure unlock may be cached in a session-scoped backend until that runtime session ends

This is a **policy**, not a storage mechanism.

## 2) Add a shared capability API

Expose this from a shared contract, preferably on `SecureSessionManager` or a small sub-interface in `commonMain`.

Example direction:

```kotlin
interface SessionRetentionCapableSecureSessionManager : SecureSessionManager {
    fun supportsSessionRetention(): Boolean
    fun getSecureUnlockRetentionPolicy(): SecureUnlockRetentionPolicy
    fun setSecureUnlockRetentionPolicy(policy: SecureUnlockRetentionPolicy)
}
```

This keeps:
- policy in commonMain,
- storage details in platform source sets.

## 3) Add a shared session-passkey-cache abstraction

Introduce a small cache abstraction in `commonMain`, for example:

```kotlin
interface SessionPasskeyCache {
    suspend fun read(): String?
    suspend fun write(passkey: String)
    suspend fun clear()
}
```

Then provide:
- a **default in-memory implementation** in commonMain,
- platform-specific implementations where needed.

This is the critical inversion point:
- common unlock logic decides **whether** to cache,
- platform backend decides **where** the cache lives.

## 4) Use platform-specific backends only for storage semantics

### Browser extension
Use extension session storage:
- `chrome.storage.session`
- `browser.storage.session`

This survives popup close/reopen and clears on browser restart.

### Android / iOS / Desktop
First pass should use a shared **in-memory cache** tied to app process lifetime / manager lifetime.

That gives a sensible initial interpretation of “current session” for installed apps:
- survive unlock/lock cycles while the app process remains alive,
- clear on fresh app process start.

If later we want more platform-native semantics, each platform can swap the backend without changing common policy/UI.

### Website / PWA
Do **not** enable this mode in the first pass unless we intentionally define its lifecycle.

Reason:
- browser tab/PWA lifecycle is different from extension lifecycle,
- `sessionStorage` means tab/page-session, not browser-session,
- this should be a separate product decision.

## 5) Keep common flow in shared code

The common flow should become:

1. User enables secure unlock.
2. User chooses retention policy:
   - prompt every time
   - retain for current session
3. After a successful secure unlock:
   - shared code checks selected policy,
   - if session retention is enabled, shared code writes decrypted passkey to `SessionPasskeyCache`.
4. On next unlock attempt:
   - shared code checks `SessionPasskeyCache` first,
   - if cache hit, return passkey without another biometric/WebAuthn prompt,
   - if miss, run the platform secure-auth flow and repopulate cache if policy allows.

That is the core logic you wanted shared.

## Proposed implementation plan

### Phase 1 - Add common session-auth-retention contract in `commonMain`

Files likely affected:
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/session/SecureSessionManager.kt`
- possibly a new file under `.../session/` for the policy enum/sub-interface

Work:
1. Introduce a common retention-policy enum.
2. Add a shared capability API for secure session retention.
3. Keep the API optional so platforms can opt in cleanly.
4. Preserve existing `SessionManager` behavior for platforms that do not implement the new capability yet.

### Phase 2 - Add shared session-passkey-cache abstraction

Files likely affected:
- new `composeApp/src/commonMain/kotlin/tech/arnav/twofac/session/SessionPasskeyCache.kt`
- possibly a simple in-memory implementation in the same package

Work:
1. Create the cache abstraction.
2. Add a default in-memory implementation in commonMain.
3. Keep it intentionally tiny: read/write/clear only.
4. Document that the backend defines lifecycle boundaries.

### Phase 3 - Refactor platform session managers to use the shared retention policy

Files likely affected:
- `composeApp/src/wasmJsMain/.../BrowserSessionManager.kt`
- `composeApp/src/androidMain/.../AndroidBiometricSessionManager.kt`
- `composeApp/src/iosMain/.../IosBiometricSessionManager.kt`
- `composeApp/src/desktopMain/.../DesktopBiometricSessionManager.kt`

Work:
1. Inject or create a `SessionPasskeyCache` in each manager.
2. On successful unlock, populate the cache only when retention policy is `RETAIN_FOR_CURRENT_SESSION`.
3. On `getSavedPasskey()`, check the session cache before prompting again.
4. On disable / clear / enrollment reset, clear the cache.
5. Keep secure persistent enrollment/encrypted-storage behavior unchanged.

### Phase 4 - Implement browser-extension backend using extension `storage.session`

This phase is where plan 41 becomes relevant.

Files likely affected:
- `composeApp/src/wasmJsMain/typescript/src/`
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/session/interop/`
- `composeApp/src/wasmJsMain/.../BrowserSessionManager.kt`

Work:
1. Add Chrome/Firefox session-storage interop.
2. Implement a `SessionPasskeyCache` backend backed by extension `storage.session`.
3. Use that backend only when running in an extension context.
4. Fall back to in-memory cache or unsupported behavior on non-extension web targets, per policy decisions.

### Phase 5 - Expose a common settings/UI flow with capability gating

Files likely affected:
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/viewmodels/SettingsViewModel.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`
- string resources

Work:
1. Keep secure unlock enablement as the primary setting.
2. Add a second shared setting for retention policy only when the session manager supports it.
3. Let common UI decide visibility based on shared capability methods, not platform type checks.
4. Allow copy to stay platform-specific:
   - browser extension: “Remember authentication for this browser session”
   - installed apps: likely “Keep unlocked for this app session” or similar
5. Hide the setting on targets that do not support or product-enable it yet.

### Phase 6 - Tests and verification

Add tests for both shared policy logic and platform behavior.

#### Shared/common tests
1. policy defaults to `PROMPT_EVERY_TIME`
2. cache is only used when policy is `RETAIN_FOR_CURRENT_SESSION`
3. clearing secure unlock clears session cache
4. unsupported platforms hide/ignore the policy cleanly

#### Browser tests
1. popup reopen reuses cache when enabled
2. browser restart loses cache
3. website/PWA path does not incorrectly use extension APIs

#### Android/iOS/Desktop tests
1. second unlock within same app process can reuse the cached passkey when enabled
2. fresh app process start does not reuse session cache
3. disabling secure unlock clears session cache

## Relationship to existing plans

### Plan 41
`41-browser-extension-session-auth-plan.md` should be treated as the **browser backend implementation slice** of this broader plan.

### Plan 19
`19-common-secure-unlock-genericity-research.md` already points in the same direction:
- keep policy in commonMain,
- keep platform details in platform source sets,
- avoid web-specific coupling in shared flows.

This new plan extends that idea from “generic secure unlock readiness” to “generic secure unlock retention policy”.

### Plan 24
`24-consolidate-passkey-secure-session-toggles-plan.md` is also aligned.
Once session retention becomes a shared capability, the settings UI can model:
- secure unlock enablement
- optional current-session retention

without hard-coding browser-only concepts into the main UI flow.

## Acceptance criteria

1. The product concept “prompt every time vs retain for current session” is represented in `commonMain`.
2. Browser extensions use `storage.session` as their backend implementation.
3. Android, iOS, and Desktop can opt into the same shared policy using their own backend implementations.
4. Shared UI/ViewModel code is driven by common capability APIs, not by browser-specific type checks.
5. The website/PWA remains unaffected until explicitly designed.

## Open questions

1. For installed apps, what exactly is the boundary of “current session”?
   - process lifetime,
   - foreground lifetime,
   - until explicit lock,
   - or a timeout?
2. Do we want desktop/mobile wording to differ from browser-extension wording?
3. Should the first installed-app backend be pure in-memory only, or should any platform use a stronger OS-managed short-lived credential cache?
4. Should the website/PWA later get a separate “remember until this tab/app instance closes” mode?

## Final recommendation

Do **not** implement this only as an extension-specific feature first and then try to generalize later.

Instead:
1. add the shared policy/capability/cache abstractions first,
2. wire browser extension `storage.session` as the first backend,
3. then let Android/iOS/Desktop adopt the same shared policy with their own session backends.

That best matches the architecture you described: **shared logic in common code, platform-specific storage underneath.**
