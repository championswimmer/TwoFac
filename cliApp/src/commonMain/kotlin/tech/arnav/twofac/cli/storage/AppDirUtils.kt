package tech.arnav.twofac.cli.storage

import ca.gosyer.appdirs.AppDirs
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

object AppDirUtils {
    const val ACCOUNTS_STORAGE_FILE_NAME = "accounts.json"
    private val appDirs = AppDirs {
        macOs { useSpaceBetweenAuthorAndApp = false }
        appName = "2fac"
        appAuthor = "tech.arnav"
    }

    fun getStorageFilePath(forceCreate: Boolean = false): String {
        val dir = appDirs.getUserDataDir()
        if (forceCreate) {
            SystemFileSystem.createDirectories(Path(dir))
        }
        return Path(dir, ACCOUNTS_STORAGE_FILE_NAME).toString()
    }
}