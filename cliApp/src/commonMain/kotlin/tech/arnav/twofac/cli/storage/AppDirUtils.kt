package tech.arnav.twofac.cli.storage

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.cli.getPlatform

object AppDirUtils {
    const val ACCOUNTS_STORAGE_FILE_NAME = "accounts.json"
    const val BACKUP_DIR_NAME = "backups"
    const val CLI_CONFIG_FILE_NAME = "cli-config.json"
    private val appDirs get() = getPlatform().appDirs

    fun getStorageFilePath(forceCreate: Boolean = false): Path {
        val dir = appDirs.getUserDataDir()
        if (forceCreate) {
            SystemFileSystem.createDirectories(Path(dir))
        }
        return Path(dir, ACCOUNTS_STORAGE_FILE_NAME)
    }

    fun getBackupDirPath(forceCreate: Boolean = false): Path {
        val dir = Path(appDirs.getUserDataDir(), BACKUP_DIR_NAME)
        if (forceCreate) {
            SystemFileSystem.createDirectories(dir)
        }
        return dir
    }

    fun getCliConfigFilePath(forceCreate: Boolean = false): Path {
        val dir = appDirs.getUserDataDir()
        if (forceCreate) {
            SystemFileSystem.createDirectories(Path(dir))
        }
        return Path(dir, CLI_CONFIG_FILE_NAME)
    }

    fun initializeEmptyStorageFile() {
        val storagePath = getStorageFilePath(forceCreate = true)
        SystemFileSystem.sink(storagePath).buffered().use { sink ->
            sink.write("[]".encodeToByteArray())
            sink.flush()
        }
    }
}