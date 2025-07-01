package tech.arnav.twofac.cli.storage

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.cli.getPlatform

object AppDirUtils {
    const val ACCOUNTS_STORAGE_FILE_NAME = "accounts.json"
    private val appDirs get() = getPlatform().appDirs

    fun getStorageFilePath(forceCreate: Boolean = false): Path {
        val dir = appDirs.getUserDataDir()
        if (forceCreate) {
            SystemFileSystem.createDirectories(Path(dir))
        }
        return Path(dir, ACCOUNTS_STORAGE_FILE_NAME)
    }
}