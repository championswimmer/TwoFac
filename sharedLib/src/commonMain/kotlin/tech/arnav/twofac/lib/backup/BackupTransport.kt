package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

@PublicApi
interface BackupTransport {
    val id: String
    suspend fun isAvailable(): Boolean
    suspend fun availabilityDetail(): String? = null
    suspend fun listBackups(): BackupResult<List<BackupDescriptor>>
    suspend fun upload(content: ByteArray, descriptor: BackupDescriptor): BackupResult<BackupDescriptor>
    suspend fun download(backupId: String): BackupResult<BackupBlob>
    suspend fun delete(backupId: String): BackupResult<Unit>
}
