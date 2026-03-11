package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

@PublicApi
interface BackupTransport {
    val id: String
    val displayName: String
        get() = id
    val supportsManualBackup: Boolean
        get() = true
    val supportsManualRestore: Boolean
        get() = true
    val supportsAutomaticRestore: Boolean
        get() = false
    val requiresAuthentication: Boolean
        get() = false

    suspend fun isAvailable(): Boolean
    suspend fun listBackups(): BackupResult<List<BackupDescriptor>>
    suspend fun upload(content: ByteArray, descriptor: BackupDescriptor): BackupResult<BackupDescriptor>
    suspend fun download(backupId: String): BackupResult<BackupBlob>
    suspend fun delete(backupId: String): BackupResult<Unit>
}
