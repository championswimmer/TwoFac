---
name: Duress PIN / Panic Wipe Plan
status: Planned
progress:
  - "[ ] Confirm product scope and duress semantics"
  - "[ ] Add duress PIN configuration storage and verifier model"
  - "[ ] Add a local panic-wipe coordinator that clears vault, sessions, and local security state"
  - "[ ] Route manual unlock flows through duress detection without affecting backup/import dialogs"
  - "[ ] Add settings UX and copy for configuring, rotating, and removing the duress PIN"
  - "[ ] Add companion/watch wipe propagation for empty-vault scenarios"
  - "[ ] Add tests and validation for wipe, unlock, and migration paths"
---

# Duress PIN / Panic Wipe Plan

## Goal

Add an optional **Duress PIN** that the user can enter in place of the normal vault passkey. When it matches, the app should **wipe local vault data instead of unlocking**, leaving the app in a clean post-reset state.

## Recommended scope

### In scope for the first implementation

1. **Compose app unlock flows** (`HomeScreen`, `AccountsScreen`, and any other vault-unlock entrypoint that is explicitly unlocking the main account store).
2. **Local-only wipe** of the vault plus locally stored unlock material.
3. **Settings UX** to enable, disable, and rotate the Duress PIN.
4. **Companion/watch cleanup** so a wipe does not leave synced OTP data on the wearable cache.

### Explicitly out of scope for the first pass

1. **Backup/import passkey prompts** in Settings. Those dialogs reuse `PasskeyDialog`, but they are not “unlock the authenticator” flows and should not trigger a panic wipe.
2. **Remote backup deletion**. The feature should wipe only local device state.
3. **CLI parity by default**. The CLI has its own passkey prompts, but it is a different threat model; treat CLI support as a follow-up unless product wants it in the same rollout.

## Current constraints in the codebase

1. Manual vault unlock today goes through `AccountsViewModel.loadAccountsWithOtps(passkey)` from:
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/HomeScreen.kt`
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AccountsScreen.kt`
2. `PasskeyDialog` is also reused for backup/export/import and secure-enrollment prompts in `SettingsScreen`, so duress handling cannot be attached blindly to the dialog component itself.
3. The current wipe path is `TwoFacLib.deleteAllAccountsFromStorage()`, but it **does not re-lock** the in-memory library state; existing tests assert the library remains unlocked after deletion.
4. Remembered passkeys / secure-unlock material live outside the vault storage:
   - Android biometric prefs + keystore material
   - iOS biometric preference + keychain entry
   - Browser local storage / WebAuthn enrollment blob
5. Companion sync currently assumes there are accounts to publish; empty state propagation needs explicit handling so the watch cache is cleared too.

## Product behavior to lock down first

1. The Duress PIN should be a **second secret**, distinct from the real vault passkey.
2. It should be checked **before** calling `TwoFacLib.unlock(...)`.
3. On a match, the app should:
   - wipe local vault data
   - clear remembered passkeys / secure-unlock enrollment state
   - clear the Duress PIN configuration itself
   - return to a neutral “fresh install / empty vault” state
4. The resulting UX should **not advertise that a duress action occurred**. The safe end-state is that the vault is gone locally and the app behaves like a reset install.
5. Secure auto-unlock should **not** evaluate the Duress PIN, because there is no manual secret entry in that path.

## Phase 1: Confirm semantics and boundaries

1. Decide whether the Duress PIN is:
   - numeric-only PIN, or
   - arbitrary text secret.
2. Require it to be **different from the vault passkey** at setup time.
3. Decide whether wipe should also reset non-security preferences (recommended: no; only security/vault state).
4. Decide whether CLI support is deferred.
5. Define the exact post-wipe UX:
   - close dialog and show empty-state flow, or
   - navigate to onboarding/reset state silently.

## Phase 2: Add Duress PIN configuration storage

1. Extend app preference storage with Duress PIN configuration metadata.
2. Store a **derived verifier**, not the raw PIN:
   - versioned format
   - per-device salt
   - slow hash / KDF-derived verifier
3. Add repository methods to:
   - check if a duress PIN is configured
   - save/rotate it
   - clear it
   - verify a candidate input
4. Keep the verifier in app-local preferences rather than inside the encrypted vault so it can be checked while locked.

## Phase 3: Add a local panic-wipe coordinator

Create one orchestration path responsible for the whole wipe so the behavior is consistent across platforms.

It should coordinate:

1. **Vault storage wipe**
   - use `TwoFacLib.deleteAllAccountsFromStorage()`
2. **In-memory lock/reset**
   - add an explicit `lock()` / `reset()` style API to `TwoFacLib`, or equivalent, because deleting storage alone currently leaves the library unlocked
3. **Session/security cleanup**
   - disable remember-passkey / secure unlock preferences
   - clear saved encrypted passkeys / keychain entries / browser blobs
4. **Duress config cleanup**
   - clear the stored duress verifier after the wipe
5. **UI/viewmodel cleanup**
   - clear loaded account state and errors so the app returns to a locked/empty state

This should be a single high-level operation rather than scattered deletes from UI code.

## Phase 4: Wire duress detection into unlock-only flows

1. Introduce an unlock-specific submission path above `PasskeyDialog`, instead of putting duress logic inside the reusable dialog component.
2. For `HomeScreen` and `AccountsScreen`:
   - on manual passkey submit, first check the candidate against the duress verifier
   - if it matches, run the panic-wipe coordinator
   - if it does not match, continue with the normal `AccountsViewModel.loadAccountsWithOtps(...)` flow
3. Keep backup/import/enrollment dialogs unchanged by giving them the normal submit path.
4. Decide whether secure-unlock fallback failures should still open the same manual unlock flow (recommended: yes, so duress remains available when the user is forced into manual entry).

## Phase 5: Add settings UX for Duress PIN management

1. Add a dedicated security section/card in `SettingsScreen`.
2. Support:
   - enable/set Duress PIN
   - rotate/change Duress PIN
   - disable/remove Duress PIN
3. Require the user to confirm the current vault passkey before saving a new Duress PIN.
4. Show clear warning copy that:
   - the Duress PIN wipes local data
   - it must not match the normal vault passkey
   - remote/cloud backups are unaffected
5. Add new string resources in the security/settings resource files and keep naming aligned with existing conventions.

## Phase 6: Clear companion/watch data on wipe

The current companion sync flow does not push an empty snapshot when there are zero accounts, so panic wipe needs explicit cleanup behavior.

Plan:

1. Add a phone-side path to publish an **empty companion snapshot** or explicit clear command.
2. Update watch-side persistence to treat an empty snapshot as “clear local OTP cache”.
3. Reuse the same path for normal “delete all accounts” flows where appropriate, so behavior stays consistent beyond duress.

## Phase 7: Tests and validation

### Shared / common tests

1. Preference serialization and verifier migration tests.
2. Duress verifier positive/negative match tests.
3. `TwoFacLib` reset/lock tests after wipe.
4. ViewModel/coordinator tests proving:
   - normal passkey still unlocks
   - duress PIN wipes instead of unlocking
   - backup/import dialogs do not trigger wipe

### Platform-focused tests

1. Android/iOS/browser session cleanup tests for remembered passkeys.
2. Browser tests for secure-unlock blob cleanup.
3. Watch sync tests for empty snapshot handling.

### Validation targets for implementation

1. `./gradlew --no-daemon :sharedLib:allTests`
2. `./gradlew --no-daemon :composeApp:allTests`
3. Any targeted platform tests added around secure session managers and watch sync.

## Main risks

1. **False trigger risk** if the Duress PIN is not clearly separated from the real passkey policy.
2. **Partial wipe risk** if vault storage is deleted but session material or watch caches survive.
3. **State mismatch risk** because `TwoFacLib.deleteAllAccountsFromStorage()` currently clears accounts but keeps the library unlocked.
4. **Over-broad trigger risk** if duress logic is attached to all `PasskeyDialog` usage instead of only vault unlock flows.

## Recommended implementation order

1. Land the shared wipe/reset primitive first.
2. Add duress verifier storage second.
3. Integrate unlock-flow detection third.
4. Add settings UX fourth.
5. Finish with companion/watch cleanup and tests.

That order reduces the chance of shipping a Duress PIN that triggers only a partial wipe.
