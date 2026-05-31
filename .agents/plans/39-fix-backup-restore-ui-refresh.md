# Fix: Account List Not Updating After Backup Restore

## Problem

When accounts are restored from a backup (e.g. importing a local file), the newly
imported accounts do not appear in the Home or Accounts screens until the app is
restarted.

## Root Cause

The restore flow in `SettingsScreen` calls `BackupService.restoreBackup()` which
internally adds accounts to `TwoFacLib` and persists them to storage. However,
the `AccountsViewModel` — which holds the `StateFlow<List<DisplayAccount>>` that
the Home and Accounts screens observe — is never notified to reload its data.

### Current flow (broken)

```
SettingsScreen.executeBackupImport()
  → BackupService.restoreBackup()          // accounts saved to disk + TwoFacLib memory
  → companionSyncCoordinator.onAccountsChanged()  // watch synced ✅
  → snackbar shown                                 // user told "imported N accounts" ✅
  ✘ AccountsViewModel._accounts NOT updated        // UI stale ✗
```

There are **two** code paths that trigger a restore, and both are missing the
ViewModel refresh:

1. **Plaintext / already-unlocked restore** — `executeBackupImport()` at line ~169
2. **Encrypted restore (current passkey dialog callback)** — inline `service.restoreBackup()`
   at line ~595

By contrast, `AccountsViewModel.addAccount()` and `deleteAccount()` correctly
refresh `_accounts` and `_accountOtps` after mutating data.

## Proposed Fix

### Approach: Add a `reloadAccounts()` method to `AccountsViewModel` and call it after restore

This is the minimal, non-invasive fix that mirrors how `addAccount()` / `deleteAccount()`
already work.

### Changes

#### 1. `AccountsViewModel.kt` — add a public reload method

Add a new method `reloadAccounts()` that refreshes both `_accounts` and
`_accountOtps` (if the vault is unlocked):

```kotlin
fun reloadAccounts() {
    viewModelScope.launch {
        try {
            val accountList = twoFacLib.getAllAccounts()
            _accounts.value = accountList
            if (twoFacLibUnlocked) {
                val accountOtpList = twoFacLib.getAllAccountOTPs()
                _accountOtps.value = accountOtpList
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Failed to reload accounts"
        }
    }
}
```

This is intentionally lightweight: no loading spinner, no unlock attempt — it just
re-reads from the already-updated `TwoFacLib` in-memory state.

#### 2. `SettingsScreen.kt` — obtain `AccountsViewModel` and call reload after restore

**a)** Resolve `AccountsViewModel` from Koin (it's already registered as a `single`):

```kotlin
// alongside the existing koin.getOrNull<TwoFacLib>() calls (~line 78)
val accountsViewModel = remember { koin.getOrNull<AccountsViewModel>() }
```

**b)** Call `accountsViewModel.reloadAccounts()` in both restore paths:

- After `executeBackupImport()` succeeds (line ~170, alongside `companionSyncCoordinator?.onAccountsChanged()`):

  ```kotlin
  if (result is BackupResult.Success) {
      companionSyncCoordinator?.onAccountsChanged()
      accountsViewModel?.reloadAccounts()   // ← NEW
  }
  ```

- After the encrypted restore passkey dialog callback succeeds (line ~606):

  ```kotlin
  is BackupResult.Success -> {
      snackbarHostState.showSnackbar(...)
      companionSyncCoordinator?.onAccountsChanged()
      accountsViewModel?.reloadAccounts()   // ← NEW
      backupProviders = service.listProviders()
      encryptedImportRequest = null
  }
  ```

### Files changed

| File | Change |
|------|--------|
| `composeApp/src/commonMain/kotlin/tech/arnav/twofac/viewmodels/AccountsViewModel.kt` | Add `reloadAccounts()` method |
| `composeApp/src/commonMain/kotlin/tech/arnav/twofac/screens/SettingsScreen.kt` | Resolve `AccountsViewModel` via Koin; call `reloadAccounts()` in both restore-success paths |

### Why not other approaches?

| Alternative | Why not |
|-------------|---------|
| Have `TwoFacLib` expose a `Flow` of accounts | Large refactor, breaks the current pull-based architecture across all platforms |
| Navigate away / force-reload HomeScreen | Fragile, bad UX (user loses scroll position), doesn't fix AccountsScreen |
| Use a shared event bus | Over-engineered for this use case; Koin singleton already gives shared access |

## Testing

- Manually verify: import a backup → home screen immediately shows the new accounts
- Manually verify: import an encrypted backup → same result
- Ensure existing add/delete flows still work as before
- Ensure the watch sync still fires after restore
