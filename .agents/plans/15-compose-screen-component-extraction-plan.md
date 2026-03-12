---
name: Compose Screen Component Extraction Plan
status: Planned
progress:
  - "[ ] Phase 0 - Lock extraction boundaries and file placement"
  - "[ ] Phase 1 - Extract preview-friendly Home and Accounts leaf components"
  - "[ ] Phase 2 - Extract Add Account form and QR action components"
  - "[ ] Phase 3 - Extract Settings cards, rows, and dialogs"
  - "[ ] Phase 4 - Add previews with mocked UI state and sample data"
  - "[ ] Phase 5 - Rewire screens and run Compose validation"
---

# Compose Screen Component Extraction Plan

## Goal

Reduce the size of the Compose screen files in `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens` by extracting small, UI-focused composables out of the screen entrypoints without changing behavior.

The refactor should favor:

1. small leaf components first
2. stateless component APIs that receive plain UI state + callbacks
3. previews for extracted components wherever the component can be rendered from mocked data instead of Koin/viewmodels/services

## Research summary

### Current screen sizes

- `SettingsScreen.kt`: ~737 lines
- `AddAccountScreen.kt`: ~252 lines
- `HomeScreen.kt`: ~203 lines
- `AccountDetailScreen.kt`: ~180 lines
- `AccountsScreen.kt`: ~154 lines

### Existing extraction / packaging patterns

1. Reusable shared UI already lives in `tech.arnav.twofac.components`:
   - `OTPCard.kt`
   - `PasskeyDialog.kt`
2. `PlatformSettingsContent` is already split out of `SettingsScreen` into its own file while staying under the `screens` package, which is a good precedent for screen-owned extractions.
3. Current previews are lightweight and local:
   - `OTPCardPreview()` uses inline mocked `StoredAccount.DisplayAccount` data and wraps content in `TwoFacTheme`
   - `PasskeyDialogPreview()` previews a single leaf dialog with literal values
   - `SettingsScreenPreview()` currently previews the full screen directly, which is not a strong pattern for future screen refactors because the screen owns a lot of runtime dependencies and state

### Safe extraction heuristic

The first pass should extract components that are:

1. mostly layout + presentation
2. already isolated in a visually distinct block (`Card`, dialog, state section, row, action group)
3. easy to express with primitive props / small UI models
4. previewable with fake strings, booleans, and sample lists

Stateful orchestration should remain in the screen entrypoint until the UI is successfully split.

## Recommended file placement

Use the `screens` package for screen-specific extracted composables and reserve `components` for UI that is reused across multiple screens.

Recommended shape:

- keep `*Screen.kt` files as thin screen entrypoints
- add sibling files in `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/`, for example:
  - `HomeScreenSections.kt`
  - `AccountsScreenSections.kt`
  - `AddAccountScreenSections.kt`
  - `SettingsScreenSections.kt`

If any extracted composable becomes genuinely cross-screen later, it can then be promoted to `tech.arnav.twofac.components`.

## Candidate extraction map

## 1. `HomeScreen.kt`

Best first candidates:

1. `HomeLoadingState`
   - current content: app title, subtitle, progress indicator
   - easy preview with no dependencies
2. `HomeEmptyState`
   - current content: logo, empty-state copy, manage-accounts CTA
   - previewable with a no-op callback
3. `HomeLockedState`
   - current content: title + subtitle shown before unlock
   - easy preview with static data
4. `HomeOtpListSection`
   - current content: "Your Accounts" header + `LazyColumn` of `OTPCard`
   - can take `List<Pair<StoredAccount.DisplayAccount, String>>` plus `onRefreshOtp`

Why this is a good phase-1 target:

- the screen currently mixes 4 distinct UI states inline
- the extracted pieces are leaf-ish and behavior-safe
- three of the four pieces are trivially previewable

## 2. `AccountsScreen.kt`

Best candidates:

1. `AccountsLockedState`
   - text + unlock button
2. `AccountsErrorState`
   - reusable inline error block for this screen
3. `AccountsListContent`
   - wraps the `LazyColumn`
4. `AccountListItem`
   - single clickable account card

Why this is a good target:

- the screen is not huge, but extracting the list item and state blocks will make the entrypoint read like a screen coordinator
- `AccountListItem` is an especially good preview candidate with mocked account labels

## 3. `AddAccountScreen.kt`

Best candidates:

1. `OtpUriInputField`
   - encapsulates text field wiring, focus tracking hooks, and paste-shortcut plumbing
2. `QrImportActions`
   - camera and clipboard action buttons
   - props can be plain booleans plus callbacks
3. `AddAccountPasskeyField`
   - only rendered when unlock is required
4. `InlineErrorMessage`
   - used for QR error and account-add error

Notes:

- keep QR read coroutine orchestration in the screen for the first refactor
- extract only the rendered controls and messages
- this phase makes the file smaller without moving platform-specific logic away from the screen yet

## 4. `AccountDetailScreen.kt`

Possible candidates, but lower priority than Settings/Add Account:

1. `AccountDetailContent`
2. `GenerateOtpSection`
3. `DeleteAccountDialog`

This screen is already smaller, so it should be treated as optional in the first pass unless we want consistency after the larger screens are cleaned up.

## 5. `SettingsScreen.kt`

This is the main refactor target.

Best extraction candidates:

1. `StorageLocationCard`
   - storage title, storage path, delete-all icon button
2. `RememberPasskeyCard`
   - remember-passkey / secure-unlock copy and toggle
   - include nested biometric toggle row if that keeps the parent card cohesive
3. `BackupProvidersCard`
   - card title, description, empty state, provider list
4. `BackupProviderRow`
   - provider display name, availability line, export/import buttons
5. `CompanionSyncCard`
   - status text, sync button, discover button
6. `DeleteStorageDialog`
   - confirmation dialog extracted as a focused leaf

Secondary extraction candidates after the above:

1. small helpers for repeated passkey dialog invocations
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

If repeated mock data is needed across more than one preview, add a small screen-local preview data file such as `SettingsScreenPreviewData.kt`; otherwise keep the samples private and inline.

## Proposed implementation roadmap

### Phase 0 - Lock extraction boundaries and file placement

1. Keep behavior unchanged; this is a UI decomposition task, not a flow rewrite.
2. Keep orchestration state, coroutines, and service access inside the screen entrypoints for the first pass.
3. Use sibling files in the `screens` package for screen-owned extracted composables.
4. Only move a component into `components/` if it is reused by multiple screens.

### Phase 1 - Extract preview-friendly Home and Accounts leaf components

1. Split `HomeScreen` into named state sections.
2. Split `AccountsScreen` list items and inline states.
3. Add previews for the extracted Home and Accounts leaf composables.
4. Confirm the entrypoints still read as simple state coordinators.

### Phase 2 - Extract Add Account form and QR action components

1. Extract the URI field UI.
2. Extract QR action buttons into a focused action group.
3. Extract screen-local error/prompt blocks where it reduces duplication.
4. Add previews for the action group and passive form components.

### Phase 3 - Extract Settings cards, rows, and dialogs

1. Start with the purest UI pieces:
   - `StorageLocationCard`
   - `DeleteStorageDialog`
   - `BackupProviderRow`
2. Then extract the section cards:
   - `RememberPasskeyCard`
   - `BackupProvidersCard`
   - `CompanionSyncCard`
3. Keep `PasskeyDialog` reuse as-is unless a small helper meaningfully reduces repetition without hiding behavior.

### Phase 4 - Add previews with mocked UI state and sample data

1. Add previews only for extracted leaf components with stable fake data.
2. Standardize previews on `TwoFacTheme` when color/typography matter.
3. Prefer one useful happy-path preview per component, with extra previews only where state changes are visually important:
   - enabled vs disabled action row
   - available vs unavailable backup provider
   - active vs inactive companion status

### Phase 5 - Rewire screens and run Compose validation

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
3. New extracted components are stateless or near-stateless and easy to preview.
4. No user-visible behavior changes are introduced.
