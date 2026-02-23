package tech.arnav.twofac.lib.backup

import kotlinx.serialization.Serializable

data class BackupDescriptor(
    val id: String,
    val transportId: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val byteSize: Long,
    val schemaVersion: Int,
    val encryption: BackupEncryption,
    val checksum: String? = null,
)

data class BackupBlob(
    val bytes: ByteArray,
    val contentType: String = "application/json",
    val checksum: String? = null,
    val etag: String? = null,
)

sealed class BackupEncryption {
    data object Plaintext : BackupEncryption()
    data class Encrypted(val algorithm: String = "AES-GCM") : BackupEncryption()
}

data class UploadBackupRequest(
    val blob: BackupBlob,
    val backupName: String? = null,
    val schemaVersion: Int,
    val encryption: BackupEncryption,
    val createdAtMillis: Long,
)

data class BackupImportSummary(
    val imported: Int,
    val failed: Int,
)

@Serializable
data class BackupAccountSnapshot(
    val id: String,
    val label: String,
    val otpAuthUri: String,
)

@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val createdAtMillis: Long,
    val appVersion: String? = null,
    val accounts: List<BackupAccountSnapshot>,
)

interface BackupTransport {
    val id: String
    suspend fun isAvailable(): Boolean
    suspend fun listBackups(): BackupResult<List<BackupDescriptor>>
    suspend fun upload(request: UploadBackupRequest): BackupResult<BackupDescriptor>
    suspend fun download(backupId: String): BackupResult<BackupBlob>
    suspend fun delete(backupId: String): BackupResult<Unit>
}

interface BackupTransportRegistry {
    fun all(): List<BackupTransport>
    fun get(id: String): BackupTransport?
}
