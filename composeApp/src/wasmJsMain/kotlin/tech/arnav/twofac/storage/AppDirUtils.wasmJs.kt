package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf
import tech.arnav.twofac.lib.backup.BackupPreferences
import tech.arnav.twofac.lib.storage.StoredAccount

actual fun createAccountsStore(): KStore<List<StoredAccount>> {
    return storeOf(
        key = ACCOUNTS_STORAGE_KEY,
        default = emptyList()
    )
}

actual fun createBackupPreferencesStore(): KStore<BackupPreferences> {
    return storeOf(
        key = BACKUP_PREFERENCES_STORAGE_KEY,
        default = BackupPreferences(),
    )
}

actual fun getStoragePath(): String {
    return "Browser LocalStorage (key: $ACCOUNTS_STORAGE_KEY)"
}

actual suspend fun deleteAccountsStorage(): Boolean {
    return try {
        // Use the kstore's own delete function for platform-agnostic deletion
        createAccountsStore().delete()
        true
    } catch (_: Throwable) {
        false
    }
}
