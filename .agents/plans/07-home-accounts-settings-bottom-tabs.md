---
name: Home / Accounts / Settings Bottom Tabs
status: Planned
progress:
  - "[ ] Phase 0 - Lock UX scope and tab behavior"
  - "[ ] Phase 1 - Define top-level tab destinations and metadata"
  - "[ ] Phase 2 - Move bottom navigation to app-level Scaffold"
  - "[ ] Phase 3 - Refactor screen contracts for tab-first navigation"
  - "[ ] Phase 4 - Wire Accounts subflows (details, add, delete)"
  - "[ ] Phase 5 - Add/adjust tests for tab navigation and back stack behavior"
  - "[ ] Phase 6 - Run validation and UX/accessibility regression checks"
---

# Home / Accounts / Settings Bottom Tabs

## Goal

Replace the current Home screen bottom button row ("Manage Accounts" + "Settings") with a Material bottom navigation bar containing 3 top-level tabs:

1. Home
2. Accounts
3. Settings

Each tab must show icon + text, and navigation should be structured as top-level tab destinations with stable back stack behavior.

## Research summary (Compose Material + navigation + icons)

1. **Material guidance for bottom navigation**
   - Use a bottom navigation bar for **3 to 5 top-level destinations** of equal importance.
   - Compose Material3 API is `NavigationBar` + `NavigationBarItem`, with `selected`, `onClick`, `icon`, and `label`.
2. **Navigation behavior guidance**
   - Keep composables decoupled from the `NavController`; pass navigation callbacks into screens.
   - For top-level destinations, avoid duplicate stack entries (`launchSingleTop`) and preserve state (`restoreState`) when switching tabs.
3. **Icon guidance**
   - Keep one icon style family consistently across the app (e.g., Filled).
   - Use explicit `contentDescription` values for accessibility.
   - This repo already includes `material-icons-extended`, so full Material icon set is available.

## Recommended tab icon mapping

1. **Home**: `Icons.Filled.Home`
2. **Accounts**: `Icons.Filled.ManageAccounts` (fallback: `Icons.Filled.AccountCircle` if needed)
3. **Settings**: `Icons.Filled.Settings`

## Current baseline in repository

1. `App.kt` defines current routes: `Home`, `Accounts`, `AddAccount`, `AccountDetail`, `Settings`.
2. `HomeScreen` currently renders a custom `bottomBar` row with two buttons ("Manage Accounts", "Settings").
3. `AccountsScreen` and `SettingsScreen` are currently push-style screens with top-app-bar back arrows.
4. Accounts flow already supports:
   - account list (`AccountsScreen`)
   - add (`AddAccountScreen`)
   - details (`AccountDetailScreen`)
5. **Gap vs requested scope**: per-account delete is not currently exposed in account management UI/viewmodel/shared API (only delete-all-storage exists in settings/storage flows).

## Proposed navigation architecture

1. Make `Home`, `Accounts`, and `Settings` explicit **top-level tab destinations**.
2. Move bottom navigation ownership from `HomeScreen` to `App.kt` (app shell level).
3. Keep `AddAccount` and `AccountDetail` as nested child flows under Accounts behavior.
4. Show bottom bar on top-level routes (`Home`, `Accounts`, `Settings`); hide it on deeper account flows (`AddAccount`, `AccountDetail`) to reduce clutter.
5. Preserve typed navigation routes already used by this app (`composable<T>()`, `toRoute<T>()`).

## Detailed implementation roadmap

### Phase 0 - Lock UX scope and tab behavior

1. Finalize top-level destinations: Home / Accounts / Settings.
2. Lock bottom-bar visibility rule:
   - visible on `Home`, `Accounts`, `Settings`
   - hidden on `AddAccount`, `AccountDetail`
3. Lock label text and accessibility strings for all three tabs.
4. Confirm Accounts tab scope includes list, detail, add, and delete actions.

### Phase 1 - Define top-level tab destinations and metadata

1. Add tab metadata model (in navigation package or `App.kt` local model):
   - route type
   - label
   - icon
   - content description
2. Add helper logic to determine current selected tab from `NavBackStackEntry`.
3. Keep route ownership in `commonMain` navigation layer.

### Phase 2 - Move bottom navigation to app-level Scaffold

1. In `App.kt`, wrap `NavHost` inside an app-level `Scaffold`.
2. Replace Home-owned bottom row with `NavigationBar` in `App` bottomBar slot.
3. Render 3 `NavigationBarItem`s with icon + label.
4. Implement tab-switch nav options:
   - `launchSingleTop = true`
   - `restoreState = true`
   - `popUpTo(startDestination)` with save-state behavior for top-level tabs
5. Keep typed destination registrations unchanged for now (`Home`, `Accounts`, `Settings`, `AddAccount`, `AccountDetail`).

### Phase 3 - Refactor screen contracts for tab-first navigation

1. `HomeScreen`
   - remove custom bottom bar button row
   - keep content behavior unchanged (loading/unlock/accounts list)
   - keep optional in-content CTA to open Accounts when no accounts are present
2. `AccountsScreen` and `SettingsScreen`
   - make top-app-bar back icon conditional or hidden for tab-entry usage
   - preserve existing behavior for flows that still need explicit back actions
3. Update call sites in `App.kt` to pass new parameters/callbacks.

### Phase 4 - Wire Accounts subflows (details, add, delete)

1. Keep existing nested flow:
   - `Accounts` -> `AccountDetail` / `AddAccount`
2. Ensure child-flow back returns to Accounts tab naturally.
3. Add per-account delete capability (to satisfy requested Accounts tab behavior):
   - shared library API support for deleting one account
   - viewmodel method and refresh flow
   - UI affordance + confirmation (prefer detail screen delete action)
4. On successful delete, refresh account list and navigate back to Accounts list.

### Phase 5 - Add/adjust tests for tab navigation and back stack behavior

1. UI/navigation test coverage:
   - bottom bar shows 3 tabs with expected labels/icons
   - selecting each tab shows correct screen
   - selected-state highlighting follows current route
   - tab reselection does not create duplicate top-level destinations
2. Child-flow behavior checks:
   - Accounts -> Add/Detail hides bottom bar (if scope locked in Phase 0)
   - back from Add/Detail returns to Accounts tab
3. If delete is implemented in this scope:
   - delete action updates list and handles error/success states.

### Phase 6 - Run validation and UX/accessibility regression checks

1. Run compose module checks/tests already present in the repo.
2. Verify no regressions in unlock flow and OTP refresh behavior on Home.
3. Verify accessibility:
   - meaningful `contentDescription`s for each tab icon
   - readable labels on all tabs
4. Validate consistency across supported Compose targets (Android/Desktop/iOS/Wasm where applicable).

## Files likely impacted

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/App.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/navigation/NavigationRoutes.kt` (or new tab metadata file in same package)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/HomeScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AccountsScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AccountDetailScreen.kt` (if delete action added here)
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModel.kt` (for delete flow)
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/TwoFacLib.kt` + related storage interfaces/impls (if per-account delete is included now)
- Relevant test files in `composeApp/src/commonTest` and/or platform test source sets

## References used

1. Compose Material3 navigation bar guidance:
   - https://developer.android.com/develop/ui/compose/components/navigation-bar?hl=en
2. Compose navigation guidance:
   - https://developer.android.com/develop/ui/compose/navigation?hl=en
3. Compose icons guidance:
   - https://developer.android.com/develop/ui/compose/graphics/images/material?hl=en
   - https://developer.android.com/reference/kotlin/androidx/compose/material/icons/Icons?hl=en
4. Repo baseline files:
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/App.kt`
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/HomeScreen.kt`
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AccountsScreen.kt`
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AccountDetailScreen.kt`
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AddAccountScreen.kt`
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt`
   - `composeApp/src/commonMain/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModel.kt`
   - `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/TwoFacLib.kt`
   - `composeApp/build.gradle.kts`
