package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.lib.backup.BackupPreferences
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import tech.arnav.twofac.lib.storage.StoredAccount

private fun getDocumentDirectory(): String {
    val documentDirectories = NSSearchPathForDirectoriesInDomains(
        directory = NSDocumentDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true
    )
    return documentDirectories.firstOrNull() as? String ?: ""
}

actual fun createAccountsStore(): KStore<List<StoredAccount>> {
    val dir = getDocumentDirectory()
    SystemFileSystem.createDirectories(Path(dir))
    val filePath = Path(dir, ACCOUNTS_STORAGE_FILE)
    ensureStorageFileExists(filePath)

    return storeOf(
        file = filePath,
        default = emptyList()
    )
}

actual fun createBackupPreferencesStore(): KStore<BackupPreferences> {
    val dir = getDocumentDirectory()
    SystemFileSystem.createDirectories(Path(dir))
    return storeOf(
        file = Path(dir, BACKUP_PREFERENCES_STORAGE_FILE),
        default = BackupPreferences(),
    )
}

actual fun getStoragePath(): String {
    val dir = getDocumentDirectory()
    return Path(dir, ACCOUNTS_STORAGE_FILE).toString()
}

actual suspend fun deleteAccountsStorage(): Boolean {
    val dir = getDocumentDirectory()
    return deleteStorageFile(Path(dir, ACCOUNTS_STORAGE_FILE))
}
