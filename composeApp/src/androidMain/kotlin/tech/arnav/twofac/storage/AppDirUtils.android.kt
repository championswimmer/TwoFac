package tech.arnav.twofac.storage

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.internal.androidAppDirs
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.lib.storage.StoredTag

actual fun createAccountsStore(): KStore<List<StoredAccount>> {
    val dir = androidAppDirs.getUserDataDir()
    SystemFileSystem.createDirectories(Path(dir))
    val filePath = Path(dir, ACCOUNTS_STORAGE_FILE)
    ensureStorageFileExists(filePath)

    return storeOf(
        file = filePath,
        default = emptyList()
    )
}

actual fun createTagsStore(): KStore<List<StoredTag>> {
    val dir = androidAppDirs.getUserDataDir()
    SystemFileSystem.createDirectories(Path(dir))
    val filePath = Path(dir, TAGS_STORAGE_FILE)
    ensureStorageFileExists(filePath)

    return storeOf(
        file = filePath,
        default = emptyList()
    )
}

actual fun getStoragePath(): String {
    val dir = androidAppDirs.getUserDataDir()
    return Path(dir, ACCOUNTS_STORAGE_FILE).toString()
}

actual suspend fun deleteAccountsStorage(): Boolean {
    val dir = androidAppDirs.getUserDataDir()
    return deleteStorageFile(Path(dir, ACCOUNTS_STORAGE_FILE))
}
