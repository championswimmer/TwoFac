---
name: Modular Onboarding Guide
status: Planned
progress:
  - "[ ] Phase 0 - Lock onboarding shape, first-run triggers, and required-vs-optional step policy"
  - "[ ] Phase 1 - Add common onboarding domain models, registry, and progress persistence"
  - "[ ] Phase 2 - Add onboarding UI route, screen, and reusable step components"
  - "[ ] Phase 3 - Add common onboarding steps for first account, manage accounts, and import/export guidance"
  - "[ ] Phase 4 - Add platform-specific contributors for secure unlock and other capability-gated steps"
  - "[ ] Phase 5 - Wire contextual entry points and completion updates across Home, Add Account, and Settings"
  - "[ ] Phase 6 - Add tests and validate Android, iOS, Desktop, and Web assembly"
---

# Modular Onboarding Guide

## Goal

Add a non-blocking onboarding guide for the Compose app that:

1. is broken into modular steps instead of a single monolithic walkthrough
2. keeps a shared cross-platform structure and ordering in `commonMain`
3. lets each platform contribute, replace, or omit steps based on capability
4. uses platform-specific copy for steps such as secure unlock
5. can be resumed later from the app instead of trapping the user in a mandatory first-run wizard

## Current onboarding-related flow in the app

The app already has the operational pieces a new user needs, but they are spread across existing screens instead of being presented as a guided flow:

1. **First app open**
   - `composeApp/.../App.kt` starts at `Home`.
   - `HomeScreen` loads accounts and branches into loading, locked, empty, or OTP-list states.
   - There is no dedicated welcome/onboarding route today.

2. **Unlock / secure session**
   - `HomeLockedState` offers secure unlock when `SecureSessionManager.isSecureUnlockReady()` is true.
   - Manual unlock uses `PasskeyDialog`.
   - Android and iOS use biometric session managers; web uses secure unlock/WebAuthn-backed browser session handling; desktop currently has no secure unlock path.

3. **Add first account**
   - `AddAccountScreen` already supports adding by:
     - QR scan on mobile
     - pasted QR image where clipboard readers exist
     - manual/pasted `otpauth://` URI entry
   - `AccountsViewModel.addAccount(...)` is the shared entry point.

4. **Manage accounts**
   - `AccountsScreen` lists accounts and routes to `AccountDetailScreen`.
   - Current management scope is browse/view OTPs/delete.
   - Rename/reorder/tags/folders are not present today and should not be implied by onboarding copy.

5. **Import / export / backup / restore**
   - `SettingsScreen` already exposes backup import/export through `BackupService`.
   - Provider availability is platform-dependent.
   - The shared library also contains third-party import adapters, but there is no general Compose UI flow for those adapters yet.

6. **Companion / secondary device sync**
   - Companion sync is surfaced from settings when supported.
   - This is relevant as an optional onboarding step, not a mandatory first-run blocker.

## Research summary and product implications

Internet research on app onboarding patterns strongly points to the following direction:

1. **Get to first value quickly**
   - The guide should help users reach the first meaningful win fast.
   - For this app, that first win is adding the first account and seeing a live OTP.

2. **Prefer progressive disclosure over a hard-blocking tutorial**
   - A checklist or step-based guide is a better fit than a mandatory carousel.
   - Secondary setup items should appear as optional next steps after the first account is added.

3. **Separate required vs optional steps clearly**
   - Required: reach an actually usable vault state.
   - Optional: secure unlock, backup setup, companion sync, advanced import/export education.

4. **Use contextual help for sensitive or permission-gated features**
   - Camera, biometric, and WebAuthn flows should be primed with a short explanation before platform prompts.
   - Guidance should appear near the related action, not only in an up-front wall of text.

5. **Keep cross-platform structure consistent, but let content vary by platform**
   - Users should recognize the same guide shape across platforms.
   - The actual copy and step availability should reflect Android/iOS/Web/Desktop capabilities.

## Product direction for this repository

The onboarding guide should follow these locked defaults:

1. Do **not** replace the existing app startup/navigation with a separate mandatory onboarding stack.
2. Add a dedicated **Getting Started / Onboarding Guide** route that can also be opened from:
   - `HomeEmptyState`
   - `SettingsScreen`
   - optional post-action nudges after the first account is added or after unlock succeeds
3. Auto-present the guide only when it is useful:
   - after first successful unlock when the account list is empty
   - or from the empty-state CTA
4. Make **Add your first account** the primary required step.
5. Keep **secure unlock**, **backup/restore**, **import/export education**, and **companion sync** as optional capability-driven steps.
6. Only show steps that the current platform/runtime can actually support.
7. Persist both:
   - a guide-level **has seen initial onboarding** flag
   - per-step seen/dismissed/completed state keyed by stable step ID
8. Auto-show behavior should distinguish between:
   - a first-time user who should see the full initial guide
   - a returning user who has already seen the guide
   - a returning user who has already seen old steps, but now has a newly introduced unseen step
9. When a new feature adds a new onboarding step with a new stable ID, only that unseen step should be surfaced to existing users instead of replaying the whole guide.

## Proposed architecture

### 1) Shared onboarding domain (`composeApp/src/commonMain/.../onboarding/`)

Add a new common package for onboarding orchestration, for example:

- `OnboardingStepSlot.kt`
- `OnboardingGuideStep.kt`
- `OnboardingGuideAction.kt`
- `OnboardingGuideContext.kt`
- `OnboardingStepContributor.kt`
- `OnboardingGuideRegistry.kt`
- `OnboardingProgressStore.kt`

Recommended responsibilities:

1. **Stable step slots / IDs**
   - Define canonical slots and order in common code, for example:
     - `ADD_FIRST_ACCOUNT`
     - `MANAGE_ACCOUNTS`
     - `SECURE_UNLOCK`
     - `BACKUP_AND_RESTORE`
     - `COMPANION_SYNC`
   - Shared ordering keeps the guide coherent across platforms.

2. **Contributor-based step resolution**
   - Common code resolves the final guide from registered contributors.
   - A contributor can:
     - provide a step for a slot
     - override the copy/action for a slot
     - return no step when unsupported
   - This directly supports the requested behavior where secure unlock text differs on Android/iOS/Web and is absent on Desktop.

3. **Context-driven availability**
   - `OnboardingGuideContext` should expose the runtime conditions needed to decide whether a step exists or is complete:
     - current account count
     - whether secure unlock exists and is ready
     - whether camera QR import exists
     - whether clipboard QR import exists
     - available backup providers
     - companion sync availability

4. **Progress persistence**
   - Persist guide-level onboarding state plus step-level completion/seen state.
   - Recommended stored shape:
     - `hasSeenInitialOnboardingGuide: Boolean`
     - `stepStates: Map<StepId, StepState>`
   - `StepState` should support at least:
     - `seenAt`
     - `dismissedAt`
     - `completedAt`
     - optional `contentRevisionSeen`
   - The guide-level flag answers “has this user ever gone through the initial onboarding experience?”
   - The per-step map answers “which specific steps has this user already seen or completed?”
   - Prefer deriving completion from real app state when possible:
     - first account step complete when account count > 0
     - secure unlock step complete when secure unlock is ready/enrolled
     - backup step can derive from existing backup presence where feasible
   - For informational-only steps, allow explicit local completion/dismissal flags.
   - Stable step IDs are required so future features can add a new step `D` and surface only `D` to existing users who already saw `A`, `B`, and `C`.
   - Optional `contentRevisionSeen` gives us a future escape hatch if we ever intentionally want to re-surface a materially changed existing step without inventing a new step ID.

### 2) UI structure (`screens/` + `components/onboarding/`)

Following the existing Compose routing structure:

1. Add a new route-level screen:
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/OnboardingGuideScreen.kt`

2. Add reusable UI components under a new domain:
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/onboarding/`
   - likely components:
     - `OnboardingGuideHeader`
     - `OnboardingProgressCard`
     - `OnboardingStepCard`
     - `OnboardingChecklistSection`
     - `OnboardingEmptyGuideState`

3. Add typed navigation:
   - `navigation/NavigationRoutes.kt`
   - `App.kt`

4. Keep route orchestration in the screen and reusable/presentational pieces in `components/onboarding`.

### 3) DI and platform extension points

Add onboarding contributors through DI so platforms can extend common behavior without polluting `commonMain` UI logic:

1. Add shared contributor bindings in common DI modules.
2. Extend platform DI modules to register platform-specific contributors:
   - `androidMain/.../di/AndroidModules.kt`
   - `iosMain/.../di/IosModules.kt`
   - `wasmJsMain/.../di/WasmModules.kt`
   - `desktopMain/.../di/DesktopModules.kt`
3. Use deterministic resolution rules:
   - shared contributor provides default steps
   - platform contributor can replace a known slot
   - unsupported slot resolves to no card

### 4) Guide surfacing policy

The plan should explicitly support three user states:

1. **Brand-new user**
   - `hasSeenInitialOnboardingGuide = false`
   - auto-show the full initial guide when the first meaningful entry point is reached

2. **Returning user with no unseen steps**
   - `hasSeenInitialOnboardingGuide = true`
   - all currently available step IDs already have seen/completed state
   - do not auto-show onboarding; keep it manually accessible

3. **Returning user with newly added unseen steps**
   - `hasSeenInitialOnboardingGuide = true`
   - existing steps have state, but at least one newly resolved step ID does not
   - auto-surface a focused “new setup available” experience containing only unseen steps

This means the auto-show decision should be driven by:

- whether the user has ever seen the initial guide
- which current step IDs resolve for this runtime
- which of those step IDs are missing from persisted state

## Proposed onboarding step model

### Required first-win step

1. **Add your first account**
   - explain the currently supported input methods on this platform
   - route to `AddAccount`
   - mark complete when at least one account exists

This step should dynamically describe the available ways to add an account:

- Android/iOS: scan QR, paste/manual URI
- Web/Desktop: paste QR image where supported, paste/manual URI
- Fallback: manual/pasted `otpauth://` URI

### Shared optional steps

1. **Manage your accounts**
   - explain current real capabilities only: browse, view OTPs, delete accounts
   - do not promise rename/reorder/tags

2. **Import or restore existing data**
   - explain current backup import/export entry points in settings
   - keep copy honest about what exists today
   - do not expand scope to brand-new third-party import UI in v1

3. **Back up your vault**
   - explain why backups matter
   - route to settings backup providers
   - platform copy can reflect actual providers shown at runtime

### Platform-specific optional steps

1. **Secure unlock**
   - Android: biometric unlock copy and expectations
   - iOS: Face ID / Touch ID style copy
   - Web: WebAuthn / device credential copy
   - Desktop: omitted entirely for now

2. **Companion sync**
   - only shown where companion sync is available and relevant

## UX behavior recommendations

1. **Guide shape**
   - Use a checklist/progress view, not a full-screen slideshow.
   - Let users enter the app quickly and return to the guide later.

2. **Entry points**
   - `HomeEmptyState`: primary first-time CTA
   - `SettingsScreen`: always-available re-entry
   - optional success nudges after first account add or after unlock

3. **Permission priming**
   - If a step launches QR scan, biometric enrollment, or WebAuthn enrollment, explain the benefit before the system prompt appears.

4. **Progress**
   - Show required vs optional sections distinctly.
   - Keep the progress model lightweight and state-derived where possible.

5. **Dismiss / resume**
   - Users should be able to skip the guide without losing access to the app.
   - The guide should remain discoverable later.

6. **First-time vs returning behavior**
   - First-time onboarding should be broader and cover the initial getting-started path.
   - Returning users should only be nudged for genuinely unseen steps.
   - If a new feature adds step `D`, existing users who already saw `A/B/C` should only get `D`.

## Implementation roadmap

### Phase 0 - Lock onboarding shape, first-run triggers, and required-vs-optional step policy

1. Confirm that v1 is a non-blocking guide, not a forced startup wizard.
2. Lock the initial step slots and ordering.
3. Lock first-run trigger rules:
   - empty vault after unlock
   - empty-state CTA
   - settings re-entry
4. Lock the persistence model:
   - guide-level initial-onboarding flag
   - per-step seen/dismissed/completed flags
   - stable step IDs
   - whether `contentRevisionSeen` is included in v1
5. Lock auto-show rules for:
   - first-time initial guide
   - returning user with no unseen steps
   - returning user with newly added unseen steps only
6. Lock scope boundaries for v1:
   - guide existing features only
   - no new account-management capabilities
   - no new third-party import UI

### Phase 1 - Add common onboarding domain models, registry, and progress persistence

1. Add the common onboarding package and data model.
2. Add contributor contracts and slot resolution logic.
3. Add lightweight progress storage for:
   - initial guide seen flag
   - per-step seen/dismissed/completed state
4. Add auto-show resolution logic for full-guide vs unseen-steps-only modes.
5. Add tests for:
   - ordering
   - override rules
   - unsupported-step omission
   - first-time user sees full guide
   - returning user with unseen new step sees only unseen steps

### Phase 2 - Add onboarding UI route, screen, and reusable step components

1. Add `OnboardingGuideScreen`.
2. Add `components/onboarding/*` UI pieces.
3. Add a typed route in `NavigationRoutes.kt`.
4. Wire the route into `App.kt`.
5. Add UI states for:
   - loading
   - no available steps
   - mixed required/optional steps
   - completed guide

### Phase 3 - Add common onboarding steps for first account, manage accounts, and import/export guidance

1. Add the shared contributor for the common step slots.
2. Build dynamic copy for add-account methods based on QR/clipboard capability.
3. Route relevant CTAs to existing screens:
   - `AddAccount`
   - `Accounts`
   - `Settings`
4. Keep all copy accurate to current product behavior.

### Phase 4 - Add platform-specific contributors for secure unlock and other capability-gated steps

1. Android contributor:
   - secure unlock step with biometric wording
2. iOS contributor:
   - secure unlock step with Face ID / Touch ID wording
3. Web contributor:
   - secure unlock step with WebAuthn/device credential wording
4. Desktop contributor:
   - omit secure unlock slot
5. Extend contributor logic later for backup-provider- or companion-specific messaging if needed.

### Phase 5 - Wire contextual entry points and completion updates across Home, Add Account, and Settings

1. `HomeScreen`
   - surface onboarding entry after first unlock when the vault is empty
2. `HomeEmptyState`
   - add a direct “Getting Started” / guide CTA
3. `SettingsScreen`
   - add re-entry into onboarding guide
4. `AddAccountScreen` / `AccountsViewModel`
   - refresh onboarding completion when the first account is added
5. Secure unlock settings flows
   - refresh onboarding completion when enrollment succeeds

### Phase 6 - Add tests and validate Android, iOS, Desktop, and Web assembly

1. Common tests:
   - step ordering
   - contributor override behavior
   - completion derivation rules
2. Platform tests where feasible:
   - secure unlock step present on Android/iOS/Web
   - secure unlock step absent on Desktop
3. Manual validation matrix:
   - first open with empty vault
   - first account add by available methods
   - secure unlock enabled/disabled
   - backup providers available/unavailable
   - guide dismissed and reopened
   - existing user with all old steps seen gets no auto-show
   - existing user with new step `D` only gets step `D`

## Files likely impacted during implementation

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/App.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/navigation/NavigationRoutes.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/HomeScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AddAccountScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/home/HomeEmptyState.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/onboarding/*` (new)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/OnboardingGuideScreen.kt` (new)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/onboarding/*` (new)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/di/modules.kt`
- `composeApp/src/androidMain/kotlin/tech/arnav/twofac/di/AndroidModules.kt`
- `composeApp/src/iosMain/kotlin/tech/arnav/twofac/di/IosModules.kt`
- `composeApp/src/wasmJsMain/kotlin/tech/arnav/twofac/di/WasmModules.kt`
- `composeApp/src/desktopMain/kotlin/tech/arnav/twofac/di/DesktopModules.kt`

## Explicitly out of scope for v1

1. Replacing the whole app with a forced onboarding wizard.
2. Adding new account-management capabilities such as rename, reorder, folders, or tags.
3. Adding a brand-new third-party import UI on top of the existing importer adapters.
4. Introducing a large analytics/telemetry system as a prerequisite for shipping the guide.

## Summary

The safest plan is to add a shared, resume-friendly onboarding checklist that sits on top of the current routing structure, with step resolution driven by common slots plus platform contributors.

That gives the app a coherent onboarding story without lying about unsupported features, and it cleanly supports the requested secure-unlock example where Android, iOS, and Web each contribute their own version of the same conceptual step while Desktop contributes none.
