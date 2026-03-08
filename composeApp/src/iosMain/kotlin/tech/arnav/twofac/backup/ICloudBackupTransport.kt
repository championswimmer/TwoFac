package tech.arnav.twofac.backup

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.posix.memcpy
import tech.arnav.twofac.lib.backup.BackupBlob
import tech.arnav.twofac.lib.backup.BackupDescriptor
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupTransport

class ICloudBackupTransport(
    private val containerIdentifier: String? = null,
    private val fileManager: NSFileManager = NSFileManager.defaultManager,
) : BackupTransport {
    override val id: String = "icloud"

    override suspend fun isAvailable(): Boolean {
        return resolveBackupsDirectoryPath(createIfMissing = false) != null
    }

    override suspend fun availabilityDetail(): String? {
        return if (resolveBackupsDirectoryPath(createIfMissing = false) == null) {
            "Your iCloud app container is unavailable on this device or Apple account."
        } else {
            "Your iCloud app container is available."
        }
    }

    override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> {
        val directory = resolveBackupsDirectoryPath(createIfMissing = false)
            ?: return BackupResult.Failure("iCloud container is unavailable")

        return try {
            val names = fileManager.contentsOfDirectoryAtPath(directory, error = null)
                ?.filterIsInstance<String>()
                .orEmpty()
                .filter { it.nameLooksLikeBackup() }

            val descriptors = names.map { name ->
                val fullPath = "$directory/$name"
                val attributes = fileManager.attributesOfItemAtPath(fullPath, error = null)
                val byteSize = (attributes?.get(NSFileSize) as? Number)?.toLong() ?: 0L
                val modifiedAt = (attributes?.get(NSFileModificationDate) as? NSDate)
                    ?.timeIntervalSince1970
                    ?.toLong()
                    ?: parseTimestampFromFilename(name)

                BackupDescriptor(
                    id = name,
                    transportId = id,
                    createdAt = modifiedAt,
                    byteSize = byteSize,
                )
            }.sortedByDescending { it.createdAt }

            BackupResult.Success(descriptors)
        } catch (e: Exception) {
            BackupResult.Failure("Failed to list iCloud backups: ${e.message}", e)
        }
    }

    override suspend fun upload(
        content: ByteArray,
        descriptor: BackupDescriptor,
    ): BackupResult<BackupDescriptor> {
        val directory = resolveBackupsDirectoryPath(createIfMissing = true)
            ?: return BackupResult.Failure("iCloud container is unavailable")

        return try {
            val fullPath = "$directory/${descriptor.id}"
            val wrote = content.toNSData().writeToFile(fullPath, atomically = true)
            if (!wrote) {
                return BackupResult.Failure("Failed to write iCloud backup file")
            }
            BackupResult.Success(descriptor)
        } catch (e: Exception) {
            BackupResult.Failure("Failed to upload iCloud backup: ${e.message}", e)
        }
    }

    override suspend fun download(backupId: String): BackupResult<BackupBlob> {
        val directory = resolveBackupsDirectoryPath(createIfMissing = false)
            ?: return BackupResult.Failure("iCloud container is unavailable")

        return try {
            val fullPath = "$directory/$backupId"
            val data = NSData.dataWithContentsOfFile(fullPath)
                ?: return BackupResult.Failure("Backup file not found: $backupId")
            val attributes = fileManager.attributesOfItemAtPath(fullPath, error = null)
            val modifiedAt = (attributes?.get(NSFileModificationDate) as? NSDate)
                ?.timeIntervalSince1970
                ?.toLong()
                ?: parseTimestampFromFilename(backupId)
            val byteSize = (attributes?.get(NSFileSize) as? Number)?.toLong() ?: data.length.toLong()
            BackupResult.Success(
                BackupBlob(
                    content = data.toByteArray(),
                    descriptor = BackupDescriptor(
                        id = backupId,
                        transportId = id,
                        createdAt = modifiedAt,
                        byteSize = byteSize,
                    ),
                )
            )
        } catch (e: Exception) {
            BackupResult.Failure("Failed to download iCloud backup: ${e.message}", e)
        }
    }

    override suspend fun delete(backupId: String): BackupResult<Unit> {
        val directory = resolveBackupsDirectoryPath(createIfMissing = false)
            ?: return BackupResult.Failure("iCloud container is unavailable")

        return try {
            val fullPath = "$directory/$backupId"
            if (fileManager.fileExistsAtPath(fullPath)) {
                if (!fileManager.removeItemAtPath(fullPath, error = null)) {
                    return BackupResult.Failure("Failed to delete iCloud backup: $backupId")
                }
            }
            BackupResult.Success(Unit)
        } catch (e: Exception) {
            BackupResult.Failure("Failed to delete iCloud backup: ${e.message}", e)
        }
    }

    private fun resolveBackupsDirectoryPath(createIfMissing: Boolean): String? {
        val containerPath = fileManager.URLForUbiquityContainerIdentifier(containerIdentifier)?.path
            ?: return null
        val backupsPath = "$containerPath/Documents/TwoFacBackups"
        if (createIfMissing && !fileManager.fileExistsAtPath(backupsPath)) {
            fileManager.createDirectoryAtPath(
                path = backupsPath,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
        return backupsPath
    }

    private fun String.nameLooksLikeBackup(): Boolean {
        return startsWith("twofac-backup-") && endsWith(".json")
    }

    private fun parseTimestampFromFilename(filename: String): Long {
        val token = filename
            .removePrefix("twofac-backup-")
            .removeSuffix(".json")
            .substringBefore('-')
        return token.toLongOrNull() ?: 0L
    }
}

private fun ByteArray.toNSData(): NSData {
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).also { destination ->
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}
