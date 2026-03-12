package tech.arnav.twofac.cli.backup

import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import tech.arnav.twofac.lib.backup.BackupBlob
import tech.arnav.twofac.lib.backup.BackupDescriptor
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupTransport

/**
 * A [BackupTransport] that stores backup files as plain JSON files in a local directory.
 *
 * Each backup is a single file named after the descriptor's [BackupDescriptor.id].
 * The directory is created if it does not exist.
 */
class LocalFileBackupTransport(private val directory: Path) : BackupTransport {

    override val id: String = "local"
    override val displayName: String = "Local Files"

    override suspend fun isAvailable(): Boolean = true

    override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> {
        return try {
            if (!SystemFileSystem.exists(directory)) {
                return BackupResult.Success(emptyList())
            }
            val descriptors = SystemFileSystem.list(directory)
                .filter { it.name.startsWith("twofac-backup-") && it.name.endsWith(".json") }
                .map { path ->
                    val bytes = readFile(path)
                    BackupDescriptor(
                        id = path.name,
                        transportId = id,
                        createdAt = parseTimestampFromFilename(path.name),
                        byteSize = bytes.size.toLong(),
                    )
                }
            BackupResult.Success(descriptors)
        } catch (e: Exception) {
            BackupResult.Failure("Failed to list backups: ${e.message}", e)
        }
    }

    override suspend fun upload(
        content: ByteArray,
        descriptor: BackupDescriptor
    ): BackupResult<BackupDescriptor> {
        return try {
            SystemFileSystem.createDirectories(directory)
            val file = Path(directory, descriptor.id)
            writeFile(file, content)
            BackupResult.Success(descriptor)
        } catch (e: Exception) {
            BackupResult.Failure("Failed to write backup: ${e.message}", e)
        }
    }

    override suspend fun download(backupId: String): BackupResult<BackupBlob> {
        return try {
            val file = Path(directory, backupId)
            if (!SystemFileSystem.exists(file)) {
                return BackupResult.Failure("Backup file not found: $backupId")
            }
            val bytes = readFile(file)
            val descriptor = BackupDescriptor(
                id = backupId,
                transportId = id,
                createdAt = parseTimestampFromFilename(backupId),
                byteSize = bytes.size.toLong(),
            )
            BackupResult.Success(BackupBlob(bytes, descriptor))
        } catch (e: Exception) {
            BackupResult.Failure("Failed to read backup: ${e.message}", e)
        }
    }

    override suspend fun delete(backupId: String): BackupResult<Unit> {
        return try {
            val file = Path(directory, backupId)
            if (SystemFileSystem.exists(file)) {
                SystemFileSystem.delete(file)
            }
            BackupResult.Success(Unit)
        } catch (e: Exception) {
            BackupResult.Failure("Failed to delete backup: ${e.message}", e)
        }
    }

    private fun parseTimestampFromFilename(filename: String): Long {
        val raw = filename
            .removePrefix("twofac-backup-")
            .removeSuffix(".json")
        return raw.substringBefore('-').toLongOrNull() ?: 0L
    }

    private fun readFile(path: Path): ByteArray {
        val buffer = Buffer()
        SystemFileSystem.source(path).buffered().use { it.transferTo(buffer) }
        return buffer.readByteArray()
    }

    private fun writeFile(path: Path, bytes: ByteArray) {
        SystemFileSystem.sink(path).buffered().use { sink ->
            sink.write(bytes)
            sink.flush()
        }
    }
}

