package tech.arnav.twofac.backup

import tech.arnav.twofac.lib.backup.BackupBlob
import tech.arnav.twofac.lib.backup.BackupDescriptor
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupTransport

/**
 * A [BackupTransport] that asks the user where to export/import backup files.
 *
 * Export flow: opens a destination picker and writes the backup file there.
 * Import flow: opens a file picker and exposes the selected file as a one-item backup list.
 */
class LocalFileBackupTransport : BackupTransport {
    override val id: String = "local"
    override val displayName: String = "Local Files"

    private val importedBlobsById = LinkedHashMap<String, BackupBlob>()

    override suspend fun isAvailable(): Boolean = true

    override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> {
        val picked = try {
            pickBackupFileWithPicker()
        } catch (e: Exception) {
            return BackupResult.Failure("Failed to pick backup file: ${e.message}", e)
        }

        if (picked == null) {
            return BackupResult.Failure("Import cancelled")
        }

        val backupId = picked.id.ifBlank { "twofac-backup-import.json" }
        val descriptor = BackupDescriptor(
            id = backupId,
            transportId = id,
            createdAt = parseTimestampFromFilename(backupId),
            byteSize = picked.content.size.toLong(),
        )

        importedBlobsById.clear()
        importedBlobsById[backupId] = BackupBlob(
            content = picked.content,
            descriptor = descriptor,
        )

        return BackupResult.Success(listOf(descriptor))
    }

    override suspend fun upload(
        content: ByteArray,
        descriptor: BackupDescriptor,
    ): BackupResult<BackupDescriptor> {
        val wasSaved = try {
            saveBackupFileWithPicker(
                suggestedFileName = descriptor.id,
                content = content,
            )
        } catch (e: Exception) {
            return BackupResult.Failure("Failed to export backup: ${e.message}", e)
        }

        if (!wasSaved) {
            return BackupResult.Failure("Export cancelled")
        }

        return BackupResult.Success(descriptor)
    }

    override suspend fun download(backupId: String): BackupResult<BackupBlob> {
        val blob = importedBlobsById[backupId]
            ?: return BackupResult.Failure("No selected backup file available. Please pick a backup file again.")
        return BackupResult.Success(blob)
    }

    override suspend fun delete(backupId: String): BackupResult<Unit> {
        importedBlobsById.remove(backupId)
        return BackupResult.Success(Unit)
    }

    private fun parseTimestampFromFilename(filename: String): Long {
        val raw = filename
            .removePrefix("twofac-backup-")
            .removeSuffix(".json")
        return raw.substringBefore('-').toLongOrNull() ?: 0L
    }
}
