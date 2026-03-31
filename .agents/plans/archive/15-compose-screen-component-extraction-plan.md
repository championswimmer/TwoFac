---
name: Compose Screen Component Extraction Plan
status: Completed
progress:
  - "[x] Phase 0 - Lock domain package map and migration boundaries"
  - "[x] Phase 1 - Move existing root components into domain packages"
  - "[x] Phase 2 - Extract preview-friendly Home and Accounts domain components"
  - "[x] Phase 3 - Extract Add Account and Settings domain components"
  - "[x] Phase 4 - Add previews with mocked UI state in each domain package"
  - "[x] Phase 5 - Rewire screen entrypoints and run validation"
---

# Compose Screen Component Extraction Plan

## Goal

Reduce the size of the Compose screen files in `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens` by extracting small, UI-focused composables out of the screen entrypoints without changing behavior.

The refactor should favor:

1. small leaf components first
2. stateless component APIs that receive plain UI state + callbacks
3. previews for extracted components wherever the component can be rendered from mocked data instead of Koin/viewmodels/services
4. component placement under `tech.arnav.twofac.components.<domain>.ComponentName`

## Research summary

### Current screen sizes

- `SettingsScreen.kt`: ~737 lines
- `AddAccountScreen.kt`: ~252 lines
- `HomeScreen.kt`: ~203 lines
- `AccountDetailScreen.kt`: ~180 lines
- `AccountsScreen.kt`: ~154 lines

### Existing extraction / packaging patterns

1. Reusable shared UI already lives in `tech.arnav.twofac.components`, but a couple of files are still bare in the package root:
   - `OTPCard.kt`
   - `PasskeyDialog.kt`
2. `PlatformSettingsContent` is already split out of `SettingsScreen` into its own file while staying under the `screens` package.
3. Current previews are lightweight and local:
   - `OTPCardPreview()` uses inline mocked `StoredAccount.DisplayAccount` data and wraps content in `TwoFacTheme`
   - `PasskeyDialogPreview()` previews a single leaf dialog with literal values
   - `SettingsScreenPreview()` currently previews the full screen directly, which is not a strong pattern for future screen refactors because the screen owns a lot of runtime dependencies and state

### Desired component architecture

This plan should treat componentization as a domain-packaging cleanup, not only a file-size cleanup.

All extracted UI should live under:

- `tech.arnav.twofac.components.accounts.*`
- `tech.arnav.twofac.components.home.*`
- `tech.arnav.twofac.components.otp.*`
- `tech.arnav.twofac.components.security.*`
- `tech.arnav.twofac.components.settings.*`

The exact set of domains can grow, but the rule should stay the same: no new extracted component should remain in the root `tech.arnav.twofac.components` package.

### Safe extraction heuristic

The first pass should extract components that are:

1. mostly layout + presentation
2. already isolated in a visually distinct block (`Card`, dialog, state section, row, action group)
3. easy to express with primitive props / small UI models
4. previewable with fake strings, booleans, and sample lists

Stateful orchestration should remain in the screen entrypoint until the UI is successfully split.

## Recommended file placement

Keep `*Screen.kt` files as thin entrypoints, but place extracted composables directly into domain packages under `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/`.

Recommended mapping:

- `tech.arnav.twofac.components.home.*`
  - home-only state sections and static empty/locked/loading content
- `tech.arnav.twofac.components.accounts.*`
  - account list items, add-account form pieces, account-management dialogs
- `tech.arnav.twofac.components.otp.*`
  - OTP display components and OTP-focused list sections
- `tech.arnav.twofac.components.security.*`
  - unlock / passkey / secure-entry UI reused across flows
- `tech.arnav.twofac.components.settings.*`
  - settings cards, provider rows, companion sync blocks, destructive dialogs

This means the existing root-level components should also be moved:

- `tech.arnav.twofac.components.otp.OTPCard`
- `tech.arnav.twofac.components.security.PasskeyDialog`

After this refactor, the root `components` package should ideally contain no leaf composables directly.

## Domain migration notes

1. Move existing bare components first so the new structure is established before more UI is extracted.
2. Update screen imports as part of the same phase that moves a component.
3. Keep preview helpers beside the domain component they support unless the mock data is shared within the same domain.

## Candidate extraction map

## 1. `HomeScreen.kt`

Best first candidates:

1. `tech.arnav.twofac.components.home.HomeLoadingState`
   - current content: app title, subtitle, progress indicator
   - easy preview with no dependencies
2. `tech.arnav.twofac.components.home.HomeEmptyState`
   - current content: logo, empty-state copy, manage-accounts CTA
   - previewable with a no-op callback
3. `tech.arnav.twofac.components.home.HomeLockedState`
   - current content: title + subtitle shown before unlock
   - easy preview with static data
4. `tech.arnav.twofac.components.otp.HomeOtpListSection`
   - current content: "Your Accounts" header + `LazyColumn` of `OTPCard`
   - can take `List<Pair<StoredAccount.DisplayAccount, String>>` plus `onRefreshOtp`

Why this is a good phase-1 target:

- the screen currently mixes 4 distinct UI states inline
- the extracted pieces are leaf-ish and behavior-safe
- three of the four pieces are trivially previewable

## 2. `AccountsScreen.kt`

Best candidates:

1. `tech.arnav.twofac.components.accounts.AccountsLockedState`
   - text + unlock button
2. `tech.arnav.twofac.components.accounts.AccountsErrorState`
   - reusable inline error block for this screen
3. `tech.arnav.twofac.components.accounts.AccountsListContent`
   - wraps the `LazyColumn`
4. `tech.arnav.twofac.components.accounts.AccountListItem`
   - single clickable account card

Why this is a good target:

- the screen is not huge, but extracting the list item and state blocks will make the entrypoint read like a screen coordinator
- `AccountListItem` is an especially good preview candidate with mocked account labels

## 3. `AddAccountScreen.kt`

Best candidates:

1. `tech.arnav.twofac.components.accounts.OtpUriInputField`
   - encapsulates text field wiring, focus tracking hooks, and paste-shortcut plumbing
2. `tech.arnav.twofac.components.accounts.QrImportActions`
   - camera and clipboard action buttons
   - props can be plain booleans plus callbacks
3. `tech.arnav.twofac.components.accounts.AddAccountPasskeyField`
   - only rendered when unlock is required
4. `tech.arnav.twofac.components.accounts.InlineErrorMessage`
   - used for QR error and account-add error

Notes:

- keep QR read coroutine orchestration in the screen for the first refactor
- extract only the rendered controls and messages
- this phase makes the file smaller without moving platform-specific logic away from the screen yet

## 4. `AccountDetailScreen.kt`

Possible candidates, but lower priority than Settings/Add Account:

1. `tech.arnav.twofac.components.accounts.AccountDetailContent`
2. `tech.arnav.twofac.components.otp.GenerateOtpSection`
3. `tech.arnav.twofac.components.accounts.DeleteAccountDialog`

This screen is already smaller, so it should be treated as optional in the first pass unless we want consistency after the larger screens are cleaned up.

## 5. `SettingsScreen.kt`

This is the main refactor target.

Best extraction candidates:

1. `tech.arnav.twofac.components.settings.StorageLocationCard`
   - storage title, storage path, delete-all icon button
2. `tech.arnav.twofac.components.settings.RememberPasskeyCard`
   - remember-passkey / secure-unlock copy and toggle
   - include nested biometric toggle row if that keeps the parent card cohesive
3. `tech.arnav.twofac.components.settings.BackupProvidersCard`
   - card title, description, empty state, provider list
4. `tech.arnav.twofac.components.settings.BackupProviderRow`
   - provider display name, availability line, export/import buttons
5. `tech.arnav.twofac.components.settings.CompanionSyncCard`
   - status text, sync button, discover button
6. `tech.arnav.twofac.components.settings.DeleteStorageDialog`
   - confirmation dialog extracted as a focused leaf

Secondary extraction candidates after the above:

1. small helpers for repeated `tech.arnav.twofac.components.security.PasskeyDialog` invocations
2. dedicated screen-state models for settings sections if the prop lists get too large

Why these are the best targets:

- each block is already visually segmented as a `Card` or dialog
- each block can be made previewable from mocked strings/booleans/provider data
- extracting these pieces will remove a large amount of inline layout noise from the screen without forcing a viewmodel rewrite

## Preview strategy

Preview support should be added only for extracted leaf components, not for the full dependency-heavy screens.

Recommended approach:

1. Wrap previews in `TwoFacTheme` where the component depends on app theming.
2. Prefer inline fake data or small private sample builders near the component.
3. Keep preview models UI-only:
   - strings
   - booleans
   - callback lambdas
   - tiny fake provider/account rows
4. Avoid Koin, `AccountsViewModel`, `BackupService`, `SessionManager`, or runtime unlock flows in preview code.

High-value preview targets:

1. `HomeEmptyState`
2. `HomeLockedState`
3. `AccountListItem`
4. `QrImportActions`
5. `StorageLocationCard`
6. `BackupProviderRow`
7. `CompanionSyncCard`
8. `DeleteStorageDialog`

If repeated mock data is needed across more than one preview, add a small domain-local preview data file such as `components/settings/SettingsPreviewData.kt`; otherwise keep the samples private and inline.

## Proposed implementation roadmap

### Phase 0 - Lock domain package map and migration boundaries

1. Keep behavior unchanged; this is a UI decomposition task, not a flow rewrite.
2. Lock the initial domain map: `home`, `accounts`, `otp`, `security`, `settings`.
3. Keep orchestration state, coroutines, and service access inside the screen entrypoints for the first pass.
4. Require all extracted components to land in `tech.arnav.twofac.components.<domain>`.

### Phase 1 - Move existing root components into domain packages

1. Move `OTPCard` from the root components package into `components/otp/`.
2. Move `PasskeyDialog` from the root components package into `components/security/`.
3. Update all screen imports to the domain package paths.
4. Keep existing previews working after the move.

### Phase 2 - Extract preview-friendly Home and Accounts domain components

1. Split `HomeScreen` into domain components under `components/home` and `components/otp`.
2. Split `AccountsScreen` list items and inline states under `components/accounts`.
3. Keep the screen files focused on state selection and callbacks.
4. Add previews for the extracted Home and Accounts components.

### Phase 3 - Extract Add Account and Settings domain components

1. Start with the purest UI pieces:
   - `components/accounts/QrImportActions`
   - `components/settings/StorageLocationCard`
   - `components/settings/DeleteStorageDialog`
   - `components/settings/BackupProviderRow`
2. Then extract the larger section cards:
   - `components/accounts/OtpUriInputField`
   - `components/accounts/AddAccountPasskeyField`
   - `components/settings/RememberPasskeyCard`
   - `components/settings/BackupProvidersCard`
   - `components/settings/CompanionSyncCard`
3. Keep `PasskeyDialog` reuse through `components.security.PasskeyDialog` unless a small helper meaningfully reduces repetition without hiding behavior.

### Phase 4 - Add previews with mocked UI state in each domain package

1. Add previews only for extracted leaf components with stable fake data.
2. Standardize previews on `TwoFacTheme` when color/typography matter.
3. Keep previews beside the component in the same domain package whenever possible.
4. Prefer one useful happy-path preview per component, with extra previews only where state changes are visually important:
   - enabled vs disabled action row
   - available vs unavailable backup provider
   - active vs inactive companion status

### Phase 5 - Rewire screen entrypoints and run validation

1. Replace inline layout blocks with the extracted composables.
2. Keep the screen files focused on:
   - state collection
   - event handling
   - coroutine launching
   - navigation callbacks
3. Run the existing Compose/build validation tasks after implementation work starts.

## Success criteria

The refactor should be considered successful when:

1. `SettingsScreen.kt` is substantially smaller and reads primarily as orchestration logic.
2. `HomeScreen.kt`, `AccountsScreen.kt`, and `AddAccountScreen.kt` each have clearer top-level structure with named UI sections.
3. Components are organized under `tech.arnav.twofac.components.<domain>` instead of remaining in the root package.
4. Existing bare root components have been migrated into domain packages.
5. New extracted components are stateless or near-stateless and easy to preview.
6. No user-visible behavior changes are introduced.
