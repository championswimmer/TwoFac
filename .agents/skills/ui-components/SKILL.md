---
name: ui-components
description: Structure and routing guide for composeApp screens, components, and their relationships.
---
# Skill: Compose UI Components and Screens

Use this skill when working on UI in `composeApp`, especially to decide where to place new screens/components and how they connect.

## composeApp UI structure

- Route-level screens live in:
  - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/`
- Reusable UI components live in:
  - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/<domain>/`
- Navigation routes are declared in:
  - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/navigation/NavigationRoutes.kt`
- Route wiring is in:
  - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/App.kt`

Current component domains:

- `components/accounts`
- `components/home`
- `components/otp`
- `components/security`
- `components/settings`

## Screens and responsibilities

### `HomeScreen`
- Shows home-state UX:
  - loading state
  - empty vault state
  - locked state
  - OTP list when unlocked
- Triggers account loading and unlock flow (including saved passkey auto-unlock).

### `AccountsScreen`
- Shows account list and account-management entry points.
- Handles locked/error/loading branches for account browsing.
- Entry point to add-account and account-detail routes.

### `AddAccountScreen`
- Handles adding accounts by OTP URI.
- Supports camera QR import and clipboard QR import.
- Collects passkey when library is locked.

### `AccountDetailScreen`
- Shows selected account details.
- Generates OTP for selected account.
- Handles account deletion with confirmation dialog.
- Note: this screen currently uses Material primitives directly (no shared `components/*` imports).

### `SettingsScreen`
- Manages storage actions, remember/secure unlock, backup import/export, and companion sync.
- Coordinates passkey-gated actions for sensitive operations.
- Delegates platform-specific settings section to `PlatformSettingsContent`.

### `PlatformSettingsContent` (expect/actual)
- `commonMain`: expect declaration.
- `desktopMain`: tray/menu-bar toggle + quit button card.
- `androidMain`, `iosMain`, `wasmJsMain`: currently no-op actuals.

## Screen → component relationships

- `HomeScreen`
  - `components/home/HomeLoadingState`
  - `components/home/HomeEmptyState`
  - `components/home/HomeLockedState`
  - `components/otp/HomeOtpListSection`
  - `components/security/PasskeyDialog`

- `AccountsScreen`
  - `components/accounts/AccountsLockedState`
  - `components/accounts/AccountsErrorState`
  - `components/accounts/AccountsListContent`
  - `components/security/PasskeyDialog`

- `AddAccountScreen`
  - `components/accounts/OtpUriInputField`
  - `components/accounts/QrImportActions`
  - `components/accounts/AddAccountPasskeyField`
  - `components/accounts/InlineErrorMessage`

- `SettingsScreen`
  - `components/settings/StorageLocationCard`
  - `components/settings/RememberPasskeyCard`
  - `components/settings/BackupProvidersCard`
  - `components/settings/CompanionSyncCard`
  - `components/settings/DeleteStorageDialog`
  - `components/security/PasskeyDialog`
  - `screens/PlatformSettingsContent`

- `AccountDetailScreen`
  - No shared components yet (screen-owned UI)

## Component nesting relationships

- `HomeOtpListSection` → renders `OTPCard` rows.
- `AccountsListContent` → renders `AccountListItem` rows.
- `BackupProvidersCard` → renders `BackupProviderRow` rows.

## Where to add new components

1. Prefer reusable components in `components/<domain>/ComponentName.kt`.
2. Choose domain by feature ownership:
   - account management UI → `components/accounts`
   - home states/layouts → `components/home`
   - OTP presentation widgets → `components/otp`
   - auth/passkey dialogs → `components/security`
   - settings cards/rows/dialogs → `components/settings`
3. Keep route-specific glue/state logic in screen files; extract only reusable/presentational parts into `components/*`.
4. If you create a new domain, keep naming consistent: `tech.arnav.twofac.components.<domain>.ComponentName`.

## Where to add new screens

1. Add route-level composable in `screens/` (e.g. `NewFeatureScreen.kt`).
2. Add a typed route to `navigation/NavigationRoutes.kt` using `@Serializable` object/data class.
3. Wire the screen into `App.kt` `NavHost` with a `composable<...>` entry.
4. If it is top-level navigation, update:
   - `TopLevelDestination`
   - bottom bar selection logic (`shouldShowBottomBar`, `isSelected`)
5. Keep platform-specific variants as `expect/actual` only when platform API differences are required.

## Quick checklist before finishing UI changes

- Is route-level orchestration in `screens/` and reusable UI in `components/<domain>/`?
- Are typed routes declared in `NavigationRoutes.kt`?
- Is `App.kt` navigation wiring updated?
- Are component imports domain-consistent and not placed in `components` root?
