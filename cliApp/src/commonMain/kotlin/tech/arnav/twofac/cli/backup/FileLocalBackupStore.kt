package tech.arnav.twofac.cli.backup

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.io.write
import tech.arnav.twofac.lib.backup.LocalBackupFile
import tech.arnav.twofac.lib.backup.LocalBackupStore

class FileLocalBackupStore(
    private val baseDir: Path,
) : LocalBackupStore {

    override suspend fun list(): List<LocalBackupFile> {
        if (!SystemFileSystem.exists(baseDir)) return emptyList()
        return SystemFileSystem.list(baseDir).map { path ->
            LocalBackupFile(
                id = fileName(path),
                name = fileName(path),
            )
        }
    }

    override suspend fun write(name: String, bytes: ByteArray): LocalBackupFile {
        SystemFileSystem.createDirectories(baseDir)
        val path = Path(baseDir, name)
        SystemFileSystem.write(path) {
            write(bytes)
        }
        return LocalBackupFile(
            id = fileName(path),
            name = name,
            sizeBytes = bytes.size.toLong(),
        )
    }

    override suspend fun read(id: String): ByteArray? {
        val path = Path(baseDir, id)
        if (!SystemFileSystem.exists(path)) return null
        return SystemFileSystem.read(path) { readByteArray() }
    }

    override suspend fun delete(id: String): Boolean {
        val path = Path(baseDir, id)
        if (!SystemFileSystem.exists(path)) return false
        SystemFileSystem.delete(path)
        return true
    }

    private fun fileName(path: Path): String {
        val value = path.toString()
        val separatorIndex = value.lastIndexOfAny(charArrayOf('/', '\\'))
        return if (separatorIndex >= 0) {
            value.substring(separatorIndex + 1)
        } else {
            value
        }
    }
}
