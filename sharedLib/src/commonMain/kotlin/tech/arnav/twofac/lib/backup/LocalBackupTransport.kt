package tech.arnav.twofac.lib.backup

data class LocalBackupFile(
    val id: String,
    val name: String,
    val updatedAtMillis: Long? = null,
    val sizeBytes: Long? = null,
)

interface LocalBackupStore {
    suspend fun list(): List<LocalBackupFile>
    suspend fun write(name: String, bytes: ByteArray): LocalBackupFile
    suspend fun read(id: String): ByteArray?
    suspend fun delete(id: String): Boolean
}

class LocalBackupTransport(
    private val store: LocalBackupStore,
) : BackupTransport {
    override val id: String = "local"

    override suspend fun isAvailable(): Boolean = true

    override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> = try {
        val descriptors = store.list().map { file ->
            BackupDescriptor(
                id = file.id,
                transportId = id,
                createdAtMillis = file.updatedAtMillis ?: 0L,
                updatedAtMillis = file.updatedAtMillis ?: 0L,
                byteSize = file.sizeBytes ?: 0L,
                schemaVersion = BackupPayloadCodec.CURRENT_SCHEMA_VERSION,
                encryption = BackupEncryption.Plaintext,
            )
        }
        BackupResult.Success(descriptors)
    } catch (e: Exception) {
        BackupResult.Failure(
            BackupError(
                code = BackupErrorCode.StorageError,
                message = "Failed to list local backups: ${e.message}",
                cause = e,
            )
        )
    }

    override suspend fun upload(request: UploadBackupRequest): BackupResult<BackupDescriptor> = try {
        val fileName = (request.backupName ?: "twofac-backup-${request.createdAtMillis}.json")
            .let { name -> if (name.endsWith(".json")) name else "$name.json" }

        val file = store.write(fileName, request.blob.bytes)
        BackupResult.Success(
            BackupDescriptor(
                id = file.id,
                transportId = id,
                createdAtMillis = request.createdAtMillis,
                updatedAtMillis = file.updatedAtMillis ?: request.createdAtMillis,
                byteSize = file.sizeBytes ?: request.blob.bytes.size.toLong(),
                schemaVersion = request.schemaVersion,
                encryption = request.encryption,
                checksum = request.blob.checksum,
            )
        )
    } catch (e: Exception) {
        BackupResult.Failure(
            BackupError(
                code = BackupErrorCode.StorageError,
                message = "Failed to write local backup: ${e.message}",
                cause = e,
            )
        )
    }

    override suspend fun download(backupId: String): BackupResult<BackupBlob> = try {
        val bytes = store.read(backupId)
            ?: return BackupResult.Failure(
                BackupError(
                    code = BackupErrorCode.NotFound,
                    message = "Backup $backupId not found",
                )
            )
        BackupResult.Success(
            BackupBlob(
                bytes = bytes,
                contentType = "application/json",
            )
        )
    } catch (e: Exception) {
        BackupResult.Failure(
            BackupError(
                code = BackupErrorCode.StorageError,
                message = "Failed to read local backup: ${e.message}",
                cause = e,
            )
        )
    }

    override suspend fun delete(backupId: String): BackupResult<Unit> = try {
        val deleted = store.delete(backupId)
        if (!deleted) {
            BackupResult.Failure(
                BackupError(
                    code = BackupErrorCode.NotFound,
                    message = "Backup $backupId not found",
                )
            )
        } else {
            BackupResult.Success(Unit)
        }
    } catch (e: Exception) {
        BackupResult.Failure(
            BackupError(
                code = BackupErrorCode.StorageError,
                message = "Failed to delete local backup: ${e.message}",
                cause = e,
            )
        )
    }
}
