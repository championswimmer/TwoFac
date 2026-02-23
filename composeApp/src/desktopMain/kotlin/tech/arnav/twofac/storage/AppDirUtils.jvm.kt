package tech.arnav.twofac.storage

import ca.gosyer.appdirs.AppDirs
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.lib.storage.StoredAccount

private val appDirs = AppDirs {
    appName = "TwoFac"
    appAuthor = "tech.arnav"
    macOS.useSpaceBetweenAuthorAndApp = false
}
private const val DEFAULT_BACKUP_FILE = "twofac-backup.json"

actual fun createAccountsStore(): KStore<List<StoredAccount>> {
    val dir = appDirs.getUserDataDir()
    SystemFileSystem.createDirectories(Path(dir))
    val filePath = Path(dir, ACCOUNTS_STORAGE_FILE)

    return storeOf(
        file = filePath,
        default = emptyList()
    )
}

actual fun getStoragePath(): String {
    val dir = appDirs.getUserDataDir()
    return Path(dir, ACCOUNTS_STORAGE_FILE).toString()
}

fun getBackupDir(forceCreate: Boolean = false): Path {
    val dir = appDirs.getUserDataDir()
    val path = Path(dir)
    if (forceCreate) {
        SystemFileSystem.createDirectories(path)
    }
    return path
}

fun getDefaultBackupFilePath(forceCreate: Boolean = false): Path {
    val dir = getBackupDir(forceCreate)
    return Path(dir, DEFAULT_BACKUP_FILE)
}
