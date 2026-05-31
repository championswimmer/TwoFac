---
name: Account Color Tags Plan
status: Completed
progress:
  - "[x] Phase 0 - Finalize Canonical Palette and Storage Semantics"
  - "[x] Phase 1 - Add Shared Account Color Domain Model"
  - "[x] Phase 2 - Persist Optional Color on Stored Accounts"
  - "[x] Phase 3 - Expose Color Update APIs and ViewModel State"
  - "[x] Phase 4 - Apply Colors in Compose UI Cards and Account Management"
  - "[x] Phase 5 - Add CLI/TUI Color Selection and Display"
  - "[x] Phase 6 - Propagate Color Through Backups and Companion Sync"
  - "[x] Phase 7 - Testing, ABI Updates, and Cross-Platform Validation"
---

# Account Color Tags Plan

## Goal
Add optional, user-selected color tags to every account. A color tag is chosen from a small canonical palette owned by `sharedLib`, persisted as a flattened nullable field on the stored account model, and rendered consistently across graphical UI surfaces and the CLI/TUI.

The feature must be backward compatible with existing account stores: older stored account JSON objects that do not contain the color field must continue to decode successfully and should behave exactly like accounts with `color = null`.

## Requirements Summary

- Define a canonical palette of about 7-8 non-black/non-white account colors.
- Each palette entry has:
  - a stable enum/id used for storage and serialization,
  - a human-readable label,
  - a muted light-mode color,
  - a muted dark-mode color,
  - optional terminal color metadata for CLI rendering.
- Store account color directly on `StoredAccount` as a flattened nullable field, e.g. `color: AccountColor? = null`.
- Existing stores with no color field must remain valid.
- Color selection is optional; accounts may have no color.
- Users pick the color from the account detail/manage screen.
- Compose UI cards should use the selected color as the card/background tint:
  - light theme uses the light muted shade,
  - dark theme uses the dark muted shade,
  - untagged accounts retain normal theme/default card colors.
- CLI/TUI should show color without making the whole row hard to read:
  - add a narrow color column/swatch,
  - optionally tint only the swatch or a small part of the account label,
  - account row text remains readable.

## Proposed Canonical Palette

Initial enum candidates in `sharedLib`:

| Enum | Light muted shade | Dark muted shade | Notes |
|------|-------------------|------------------|-------|
| `RED` | muted rose/red | deep muted red | Error red is not reused directly; keep softer than danger. |
| `ORANGE` | muted amber/orange | deep muted orange | Adds an eighth warm color and avoids relying on yellow only. |
| `YELLOW` | muted sand/yellow | dark ochre | Must remain readable in light mode. |
| `GREEN` | muted sage/green | deep forest green | Distinct from timer green if possible. |
| `TEAL` | muted teal | deep teal | Requested teal color. |
| `BLUE` | muted blue | deep blue | Distinct from brand blue if possible. |
| `PURPLE` | muted lavender | deep purple | Requested purple color. |
| `BROWN` | muted tan/brown | deep brown | Requested brown color. |

Black and white are intentionally excluded so the absence of a selected color can continue to map naturally to default light/dark cards.

Exact ARGB values should be finalized in Phase 0 by checking contrast against `TwoFacThemeTokens.light` and `.dark` foreground tokens. Store the selected enum, not raw color values, so palette shades can evolve safely in future releases.

## Architecture

### Shared Library (`sharedLib`)

Add the reusable domain/color model in `sharedLib`, likely under `tech.arnav.twofac.lib.theme` or a new account presentation package:

- `AccountColorTag` enum with stable serialized names.
- `AccountColorTokens` or resolver helpers for light/dark `ThemeColor` values.
- Convenience functions such as:
  - `AccountColorTag.lightColor: ThemeColor`
  - `AccountColorTag.darkColor: ThemeColor`
  - `AccountColorTag.displayName: String`
  - `AccountColorTag.fromSerializedName(...)` if needed.

Update `StoredAccount`:

```kotlin
@Serializable
data class StoredAccount(
    val accountID: Uuid,
    val accountLabel: String,
    val salt: String,
    val encryptedURI: String,
    val color: AccountColorTag? = null,
)
```

Key compatibility rule: the new property must have a default value of `null` so old JSON without `color` decodes cleanly.

Update `StoredAccount.DisplayAccount` and `forDisplay(...)` so UI/CLI consumers receive the color without re-reading encrypted storage.

Add a public `TwoFacLib` operation to update an account color without changing the encrypted secret:

```kotlin
suspend fun updateAccountColor(accountId: String, color: AccountColorTag?): Boolean
```

Implementation should:

1. require unlocked storage,
2. parse account id safely,
3. find account in the in-memory list,
4. save `account.copy(color = color)`,
5. refresh `accountList`,
6. return `false` if the account does not exist.

### Compose App (`composeApp`)

Use `sharedLib` colors for all graphical surfaces.

Planned UI changes:

- Account detail/manage screen:
  - add a “Color” section below account metadata/actions,
  - show a “None/default” option plus one swatch per canonical color,
  - selected swatch is visually marked,
  - tapping a swatch calls the ViewModel update API.
- Accounts list:
  - pass `account.color` into list rows,
  - optionally show a small swatch/chip so the user sees the selected tag from the account list.
- Home OTP cards:
  - `OTPCard` should use the selected account color for the card container when present,
  - pick the palette shade based on current light/dark mode,
  - keep text/icon/progress colors readable; do not blindly use the same `onSurface` if contrast is poor.
- Previews:
  - add preview accounts with colors.

`AccountsViewModel` additions:

- expose `updateAccountColor(accountId: String, color: AccountColorTag?, onComplete: (Boolean) -> Unit = {})`,
- refresh `_accounts` and `_accountOtps` after successful update,
- notify `companionSyncCoordinator?.onAccountsChanged()` after successful update.

Localization:

- add strings in `composeApp/src/commonMain/composeResources/values/strings_accounts.xml`, for example:
  - `account_detail_color_title`,
  - `account_detail_color_none`,
  - `account_color_red`, etc.
- keep locale parity across existing `values-*` directories before validation, or explicitly plan a localization follow-up if this branch only changes English first.

### CLI/TUI (`cliApp`)

Add color support to terminal UX while preserving readability.

Planned TUI changes:

- Extend `TuiOtpEntry` with `color: AccountColorTag?`.
- Map `StoredAccount.DisplayAccount.color` into `TuiOtpEntry` in `TuiApp.fetchOtpEntries()`.
- Add a narrow `Color` column in `HomeScreen`.
  - Empty/default color renders as `-` or blank.
  - Selected colors render as a small swatch, e.g. `██`, `■`, or `[ ]`, styled with Mordant.
  - Do not color the whole row.
- Add color information to `AccountScreen` details.
- Add keyboard-driven color picking on account details:
  - e.g. `c` opens color selection mode,
  - arrow keys cycle colors including `None`,
  - Enter saves, Escape cancels.
- Consider whether non-interactive `DisplayCommand` should also show a color column or only TUI should. If added to `DisplayCommand`, keep output compact and test snapshot stability.

CLI theme support:

- Add helper functions to map `AccountColorTag` to:
  - truecolor `TextStyle` using shared `ThemeColor`,
  - ANSI256 fallback,
  - ANSI16 fallback,
  - no-color fallback text.

### Backups, Sync, and Companion Apps

Color is account metadata and should not be stored inside `otpauth://` URIs. Preserve it alongside account entries where the format supports metadata.

Items to audit/implement:

- Encrypted backup export:
  - add optional `color` to `EncryptedAccountEntry` so encrypted backups preserve tags.
- Plaintext backup export:
  - current plaintext payload is a list of URI strings and cannot preserve per-account metadata by itself.
  - decide whether to introduce backup schema v3 with an account-entry object for plaintext exports, while continuing to decode v1/v2.
- Restore:
  - restore color metadata where present,
  - restore old backups with no colors as `null`.
- Watch/Wear companion sync:
  - add optional color to `WatchSyncAccount` if watch surfaces should render or cache tags,
  - update Android Wear UI and watchOS consumers if the color is exposed there.
- Browser extension/mobile/desktop all consume Compose common UI, so most graphical work should happen in `composeApp/src/commonMain`.

## Phase-by-Phase Implementation Roadmap

### Phase 0 - Finalize Canonical Palette and Storage Semantics

1. Confirm exact set of enum values: proposed `RED`, `ORANGE`, `YELLOW`, `GREEN`, `TEAL`, `BLUE`, `PURPLE`, `BROWN`.
2. Pick light/dark muted ARGB values and verify foreground contrast.
3. Decide serialized enum names, preferably stable lowercase or standard enum names with `@SerialName` if needed.
4. Confirm whether backup schema should be upgraded now or handled in a follow-up.
5. Confirm whether watch/Wear companion surfaces should render account colors in this initial implementation.

### Phase 1 - Add Shared Account Color Domain Model

1. Add `AccountColorTag` and palette token resolver in `sharedLib`.
2. Keep all color constants in `sharedLib` so Compose, CLI, watch, and future frontends share one canonical palette.
3. Add unit tests for:
   - palette size/order,
   - no black/white entries,
   - light/dark token lookup for every enum.

### Phase 2 - Persist Optional Color on Stored Accounts

1. Add nullable `color` to `StoredAccount` with default `null`.
2. Add nullable `color` to `StoredAccount.DisplayAccount`.
3. Update `forDisplay(...)` to include the stored color.
4. Ensure `StorageUtils.toStoredAccount(...)` creates new accounts with `color = null`.
5. Add serialization tests proving:
   - old JSON without `color` decodes as `null`,
   - new JSON with `color` decodes correctly,
   - encoding does not break existing required fields.

### Phase 3 - Expose Color Update APIs and ViewModel State

1. Add `TwoFacLib.updateAccountColor(accountId, color)`.
2. Ensure all persistent storage implementations update existing accounts rather than append duplicates.
   - `composeApp` storage already replaces by account id.
   - `sharedLib` memory storage already replaces by account id.
   - `cliApp` file storage currently needs review because `saveAccount` appends; color updates need replacement semantics.
3. Add ViewModel wrapper in `AccountsViewModel`.
4. Refresh loaded account and OTP state after updates.
5. Notify companion sync after color changes.

### Phase 4 - Apply Colors in Compose UI Cards and Account Management

1. Create Compose mapping from `ThemeColor` to `androidx.compose.ui.graphics.Color` if no shared helper exists.
2. Update `OTPCard` card container color when `account.color != null`.
3. Add color swatch selector to `AccountDetailScreen`.
4. Add optional swatch/chip in account list rows.
5. Add previews for colored and uncolored accounts.
6. Add strings for color labels and selector actions.

### Phase 5 - Add CLI/TUI Color Selection and Display

1. Extend `TuiOtpEntry` with optional color.
2. Render a dedicated `Color` column/swatch in `HomeScreen`.
3. Render color details in `AccountScreen`.
4. Implement account-detail color picker state/actions in `TuiScreen.kt`, `TuiNavigator`, and `TuiApp`.
5. Persist changes via `TwoFacLib.updateAccountColor(...)`.
6. Add tests for TUI rendering and state transitions.

### Phase 6 - Propagate Color Through Backups and Companion Sync

1. Add color metadata to encrypted backups.
2. Decide and implement plaintext backup metadata preservation if in scope.
3. Keep old backup decode paths working with `color = null`.
4. Add optional color to companion/watch sync payloads if in scope.
5. Update watch app display/cache models if companion sync carries color.

### Phase 7 - Testing, ABI Updates, and Cross-Platform Validation

1. Update shared library ABI dumps if public APIs or data models change.
2. Run focused tests:
   - `./gradlew :sharedLib:test`
   - `./gradlew :composeApp:test`
   - `./gradlew :cliApp:test`
3. Run broader checks if implementation touches shared models and multiple frontends:
   - `./gradlew check`
4. Manually verify:
   - old account store JSON still loads,
   - new account defaults to no color,
   - selecting and clearing a color persists after restart,
   - Compose cards use light/dark shades correctly,
   - CLI/TUI swatches are visible without harming table readability.

## Files Likely Touched

### `sharedLib`

- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/storage/StoredAccount.kt`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/storage/StorageUtils.kt`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/TwoFacLib.kt`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/theme/...` (new account color palette file)
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/BackupPayload.kt`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/backup/BackupService.kt`
- `sharedLib/src/commonMain/kotlin/tech/arnav/twofac/lib/watchsync/WatchSyncPayload.kt` (if companion sync is in scope)
- `sharedLib/src/commonTest/kotlin/...` serialization/API tests
- `sharedLib/api/sharedLib.api`
- `sharedLib/api/sharedLib.klib.api`

### `composeApp`

- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModel.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/AccountDetailScreen.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/otp/OTPCard.kt`
- `composeApp/src/commonMain/kotlin/tech/arnav/twofac/components/accounts/...`
- `composeApp/src/commonMain/composeResources/values*/strings_accounts.xml`

### `cliApp`

- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/storage/FileStorage.kt`
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/tui/TuiScreen.kt`
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/tui/TuiApp.kt`
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/tui/TuiNavigator.kt`
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/tui/HomeScreen.kt`
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/tui/AccountScreen.kt`
- `cliApp/src/commonMain/kotlin/tech/arnav/twofac/cli/theme/CliTheme.kt`
- `cliApp/src/commonTest/kotlin/tech/arnav/twofac/cli/tui/...`

### `watchApp` / Apple watch consumers

- Update only if Phase 6 includes companion sync rendering for watch surfaces.

## Scope Guardrails

- Do not store raw user-provided colors; users can only pick from the canonical enum.
- Do not add black or white palette entries.
- Do not require color selection when adding an account.
- Do not encrypt/decrypt account secrets just to update the color; color is stored as separate metadata.
- Do not break old account store JSON or old backups.
- Keep CLI readability: only the swatch/small marker should be colored, not the full row.
