package tech.arnav.twofac.cli.storage

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.cli.getPlatform

object AppDirUtils {
    const val ACCOUNTS_STORAGE_FILE_NAME = "accounts.json"
    const val DEFAULT_BACKUP_FILE_NAME = "twofac-backup.json"
    private val appDirs get() = getPlatform().appDirs

    fun getStorageFilePath(forceCreate: Boolean = false): Path {
        val dir = appDirs.getUserDataDir()
        if (forceCreate) {
            SystemFileSystem.createDirectories(Path(dir))
        }
        return Path(dir, ACCOUNTS_STORAGE_FILE_NAME)
    }

    fun getBackupDir(forceCreate: Boolean = false): Path {
        val dir = appDirs.getUserDataDir()
        if (forceCreate) {
            SystemFileSystem.createDirectories(Path(dir))
        }
        return Path(dir)
    }

    fun getDefaultBackupFilePath(forceCreate: Boolean = false): Path {
        val dir = getBackupDir(forceCreate)
        return Path(dir, DEFAULT_BACKUP_FILE_NAME)
    }
}
