package tech.arnav.twofac.lib.backup

import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.PublicApi

@PublicApi
@Serializable
data class BackupDescriptor(
    val id: String,
    val transportId: String,
    val createdAt: Long,
    val schemaVersion: Int = 1,
    val byteSize: Long,
    val remoteId: String? = null,
    val checksum: String? = null,
)
