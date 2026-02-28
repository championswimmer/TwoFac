---
name: Delete Account Storage Option
status: Planned
progress:
  - "[ ] Phase 0 - Confirm deletion contract and UX copy"
  - "[ ] Phase 1 - Add sharedLib storage deletion API"
  - "[ ] Phase 2 - Implement deletion in composeApp storage backends"
  - "[ ] Phase 3 - Add Settings delete action and confirmation dialog"
  - "[ ] Phase 4 - Add CLI storage deletion command"
  - "[ ] Phase 5 - Add tests and API snapshots"
  - "[ ] Phase 6 - Validate behavior and update docs"
---

# Delete Account Storage Option

## Goal

Add a first-class option to delete account storage (`accounts.json` or platform-equivalent storage) from both Compose Settings and CLI, with clear irreversible-warning UX and shared business-level API wiring through `sharedLib`.

## Current baseline

- `sharedLib` exposes `Storage` with read/write account operations only (`getAccountList`, `getAccount`, `saveAccount`).
- Compose app uses `FileStorage(KStore<List<StoredAccount>>)` and Settings currently shows storage path but has no delete action.
- CLI uses its own `FileStorage(Path)` and currently has commands for display/add/info/backup only.
- Platform storage creation already recreates storage on startup (`ensureStorageFileExists` on file platforms, localStorage key on wasm), so post-delete recreation can remain lazy.

## Proposed architecture

1. Add a shared deletion capability in `sharedLib` `Storage` interface.
2. Expose deletion through `TwoFacLib` so UI/CLI call one shared-lib-level entry point.
3. Implement storage deletion in Compose and CLI storage implementations.
4. Add Compose Settings delete icon + confirm dialog with strong warning.
5. Add CLI command path for delete with explicit confirmation.

## Step-by-step roadmap

### Phase 0 - Confirm deletion contract and UX copy

1. Define method contract in `Storage`:
   - Recommended signature: `suspend fun deleteAllAccounts(): Boolean`
   - Success means backing storage is deleted/cleared and subsequent reads return empty list.
2. Decide `TwoFacLib` behavior after deletion:
   - Clear in-memory `accountList`.
   - Keep passkey unchanged (library may stay unlocked) unless product decision says otherwise.
3. Lock warning copy used by both Compose dialog and CLI prompt:
   - "This deletes all existing accounts and cannot be undone unless you have a backup."
   - "A fresh storage file will be created on next run/use."

### Phase 1 - Add sharedLib storage deletion API

1. Update `sharedLib/src/commonMain/.../storage/Storage.kt` with deletion method.
2. Implement in `MemoryStorage` by clearing the in-memory list.
3. Add `TwoFacLib` method (e.g., `suspend fun deleteAllAccountsFromStorage(): Boolean`) that:
   - delegates to `storage.deleteAllAccounts()`
   - resets in-memory cache to `emptyList()` on success.
4. Update `sharedLib` API dumps (`sharedLib/api/sharedLib.api` and `sharedLib.klib.api`).

### Phase 2 - Implement deletion in composeApp storage backends

1. Extend Compose storage layer to support deleting storage container:
   - Add expected helper in `AppDirUtils.kt` (e.g., `expect suspend fun deleteAccountsStorage(): Boolean`) or equivalent injectable abstraction.
2. Implement per target:
   - Android/Desktop/iOS: delete `accounts.json` at `getStoragePath()` (or truncate to `[]` if file deletion is not possible), ensure next `createAccountsStore()` recreates as needed.
   - wasmJs: clear/remove `ACCOUNTS_STORAGE_KEY` from localStorage.
3. Update `composeApp` `FileStorage` to implement new shared `Storage.deleteAllAccounts`.

### Phase 3 - Add Settings delete action and confirmation dialog

1. Update `SettingsScreen` storage card UI:
   - Keep storage path text.
   - Add Material delete icon button in the card header/row (`Icons.Default.Delete`).
2. Add confirmation dialog state + `AlertDialog` flow:
   - Title: destructive action.
   - Body explicitly warns accounts are unrecoverable without backup and new storage will be created later.
   - Actions: Cancel / Delete.
3. On confirm:
   - invoke `TwoFacLib` delete API in coroutine.
   - show success/failure snackbar.
   - optionally notify companion sync coordinator about account changes if deletion succeeds.
4. Add loading guard to prevent double-submit.

### Phase 4 - Add CLI storage deletion command

1. Introduce a CLI command for storage deletion, recommended shape:
   - `twofac storage delete`
   - optional convenience alias `twofac delete-storage` if desired.
2. Confirmation model:
   - default interactive confirmation prompt with explicit warning text.
   - support non-interactive `--yes` flag for scripts/automation.
3. Command execution:
   - call shared `TwoFacLib`/`Storage` deletion API (no manual file deletion in command layer).
   - print clear success/failure messages.
   - mention next run recreates empty storage.
4. Register command in `MainCommand(...).subcommands(...)`.

### Phase 5 - Add tests and API snapshots

1. `sharedLib` tests:
   - `MemoryStorageTest`: deletion clears entries.
   - `TwoFacLibTest`: deletion clears cached account state.
2. `composeApp` tests:
   - storage implementation test(s) for delete success path where feasible.
   - optional UI test for settings dialog visibility and warning text.
3. `cliApp` tests:
   - command test for confirmation and invocation path.
   - `KoinVerificationTest` updates if DI changes.
4. Ensure API snapshot updates are committed for sharedLib public surface changes.

### Phase 6 - Validate behavior and update docs

1. Run module tests/checks relevant to touched areas (`sharedLib`, `composeApp`, `cliApp`).
2. Manual checks:
   - Compose: delete from Settings -> reopen app -> empty accounts.
   - CLI: run delete command -> next display/add flow works with fresh storage.
3. Update user-facing docs/help text:
   - Settings destructive warning.
   - CLI help entry for deletion command and `--yes`.

## Notes and guardrails

- Keep deletion action explicit and hard to trigger accidentally.
- Do not silently swallow deletion errors; surface actionable messages.
- Reuse existing storage-path utilities rather than duplicating path logic.
- Preserve existing unlock/add/display flows after deletion.
