package tech.arnav.twofac.backup

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.delay
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.lastPathComponent
import platform.Foundation.writeToURL
import platform.posix.memcpy
import tech.arnav.twofac.lib.backup.BackupBlob
import tech.arnav.twofac.lib.backup.BackupDescriptor
import tech.arnav.twofac.lib.backup.BackupProviderIds
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupTransport

/**
 * iOS-only simple iCloud backup transport that writes backup files to the app's ubiquity
 * container Documents directory. iOS handles sync automatically from there.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class ICloudBackupTransport(
    private val fileManager: NSFileManager = NSFileManager.defaultManager,
) : BackupTransport {
    override val id: String = BackupProviderIds.ICLOUD
    override val displayName: String = "iCloud"
    override val supportsAutomaticRestore: Boolean = true
    override val requiresAuthentication: Boolean = true

    override suspend fun isAvailable(): Boolean {
        val backupDirectory = ensureBackupDirectoryExists()
        return backupDirectory != null
    }

    override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> {
        val backupDirectory = ensureBackupDirectoryExists() ?: return unavailableFailure()
        val files = fileManager.contentsOfDirectoryAtURL(
            url = backupDirectory,
            includingPropertiesForKeys = null,
            options = 0u,
            error = null,
        ).orEmpty()

        val descriptors = files
            .mapNotNull { it as? NSURL }
            .mapNotNull { url ->
                val fileName = url.lastPathComponent ?: return@mapNotNull null
                if (!fileName.endsWith(".json")) return@mapNotNull null
                val filePath = url.path ?: return@mapNotNull null
                val attrs = fileManager.attributesOfItemAtPath(filePath, error = null)
                val byteSize = (attrs?.get(NSFileSize) as? NSNumber)?.longLongValue ?: 0L
                BackupDescriptor(
                    id = fileName,
                    transportId = id,
                    createdAt = parseTimestampFromFilename(fileName),
                    byteSize = byteSize,
                )
            }
            .sortedByDescending { it.createdAt }

        return BackupResult.Success(descriptors)
    }

    override suspend fun upload(
        content: ByteArray,
        descriptor: BackupDescriptor,
    ): BackupResult<BackupDescriptor> {
        val backupFileUrl = backupFileUrl(descriptor.id) ?: return unavailableFailure()
        val data = content.toNSData()
            ?: return BackupResult.Failure("Failed to prepare backup bytes for iCloud write")
        val written = data.writeToURL(backupFileUrl, atomically = true)
        if (!written) {
            return BackupResult.Failure("Failed to write backup file to iCloud container")
        }
        return BackupResult.Success(descriptor)
    }

    override suspend fun download(backupId: String): BackupResult<BackupBlob> {
        val backupFileUrl = backupFileUrl(backupId) ?: return unavailableFailure()
        val filePath = backupFileUrl.path
            ?: return BackupResult.Failure("Unable to resolve iCloud backup path")

        if (!fileManager.fileExistsAtPath(filePath)) {
            return BackupResult.Failure("Backup '$backupId' not found in iCloud")
        }

        // Trigger download of cloud-only placeholders if needed.
        fileManager.startDownloadingUbiquitousItemAtURL(backupFileUrl, error = null)

        val data = NSData.create(contentsOfFile = filePath)
            ?: return BackupResult.Failure(
                "Backup '$backupId' is not available locally yet. Wait for iCloud sync/download and try again."
            )

        val descriptor = BackupDescriptor(
            id = backupId,
            transportId = id,
            createdAt = parseTimestampFromFilename(backupId),
            byteSize = data.length.toLong(),
        )
        return BackupResult.Success(
            BackupBlob(
                content = data.toByteArray(),
                descriptor = descriptor,
            )
        )
    }

    override suspend fun delete(backupId: String): BackupResult<Unit> {
        val backupFileUrl = backupFileUrl(backupId) ?: return unavailableFailure()
        val removed = fileManager.removeItemAtURL(backupFileUrl, error = null)
        if (!removed) {
            return BackupResult.Failure("Failed to delete iCloud backup '$backupId'")
        }
        return BackupResult.Success(Unit)
    }

    private suspend fun backupFileUrl(fileName: String): NSURL? {
        return ensureBackupDirectoryExists()?.URLByAppendingPathComponent(fileName)
    }

    private suspend fun ensureBackupDirectoryExists(): NSURL? {
        val container = resolveUbiquityContainerUrl() ?: return null
        val documents = container.URLByAppendingPathComponent("Documents", isDirectory = true) ?: return null
        val backups = documents.URLByAppendingPathComponent(ICLOUD_BACKUPS_DIRECTORY, isDirectory = true) ?: return null

        val path = backups.path ?: return null
        if (!fileManager.fileExistsAtPath(path)) {
            val created = fileManager.createDirectoryAtURL(
                url = backups,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            if (!created) return null
        }
        return backups
    }

    private suspend fun resolveUbiquityContainerUrl(): NSURL? {
        repeat(CONTAINER_RESOLVE_ATTEMPTS) { attempt ->
            val explicit = fileManager.URLForUbiquityContainerIdentifier(ICLOUD_CONTAINER_ID)
            if (explicit != null) return explicit
            val fallback = fileManager.URLForUbiquityContainerIdentifier(null)
            if (fallback != null) return fallback
            if (attempt < CONTAINER_RESOLVE_ATTEMPTS - 1) {
                delay(CONTAINER_RESOLVE_DELAY_MS)
            }
        }
        return null
    }

    private suspend fun unavailableFailure(): BackupResult.Failure {
        val hasToken = fileManager.ubiquityIdentityToken != null
        if (!hasToken) {
            return BackupResult.Failure(
                "iCloud account is not active for this app session yet. Open iOS Settings and confirm iCloud Drive is enabled, then retry."
            )
        }
        return BackupResult.Failure(
            "iCloud container is unavailable for '${ICLOUD_CONTAINER_ID}'. Verify the iCloud container ID and app entitlements."
        )
    }

    private fun parseTimestampFromFilename(filename: String): Long {
        val raw = filename.removePrefix("twofac-backup-").substringBefore('-').removeSuffix(".json")
        return raw.toLongOrNull() ?: 0L
    }

    private fun ByteArray.toNSData(): NSData? {
        if (isEmpty()) return NSData.create(bytes = null, length = 0u)
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        if (length == 0UL) return ByteArray(0)
        val output = ByteArray(length.toInt())
        output.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
        return output
    }
}

private const val ICLOUD_BACKUPS_DIRECTORY = "TwoFacBackups"
private const val ICLOUD_CONTAINER_ID = "iCloud.tech.arnav.twofac.app"
private const val CONTAINER_RESOLVE_ATTEMPTS = 5
private const val CONTAINER_RESOLVE_DELAY_MS = 400L
