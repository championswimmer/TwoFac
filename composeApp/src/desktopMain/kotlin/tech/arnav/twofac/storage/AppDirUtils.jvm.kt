package tech.arnav.twofac.storage

import ca.gosyer.appdirs.AppDirs
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.backup.GoogleDriveAuthState
import tech.arnav.twofac.lib.backup.BackupPreferences
import tech.arnav.twofac.lib.storage.StoredAccount

private val appDirs = AppDirs {
    appName = "TwoFac"
    appAuthor = "tech.arnav"
    macOS.useSpaceBetweenAuthorAndApp = false
}

actual fun createAccountsStore(): KStore<List<StoredAccount>> {
    val dir = appDirs.getUserDataDir()
    SystemFileSystem.createDirectories(Path(dir))
    val filePath = Path(dir, ACCOUNTS_STORAGE_FILE)
    ensureStorageFileExists(filePath)

    return storeOf(
        file = filePath,
        default = emptyList()
    )
}

actual fun createBackupPreferencesStore(): KStore<BackupPreferences> {
    val dir = appDirs.getUserDataDir()
    SystemFileSystem.createDirectories(Path(dir))
    return storeOf(
        file = Path(dir, BACKUP_PREFERENCES_STORAGE_FILE),
        default = BackupPreferences(),
    )
}

actual fun createGoogleDriveAuthStore(): KStore<GoogleDriveAuthState> {
    val dir = appDirs.getUserDataDir()
    SystemFileSystem.createDirectories(Path(dir))
    return storeOf(
        file = Path(dir, GOOGLE_DRIVE_AUTH_STORAGE_FILE),
        default = GoogleDriveAuthState(),
    )
}

actual fun getStoragePath(): String {
    val dir = appDirs.getUserDataDir()
    return Path(dir, ACCOUNTS_STORAGE_FILE).toString()
}

actual suspend fun deleteAccountsStorage(): Boolean {
    val dir = appDirs.getUserDataDir()
    return deleteStorageFile(Path(dir, ACCOUNTS_STORAGE_FILE))
}
