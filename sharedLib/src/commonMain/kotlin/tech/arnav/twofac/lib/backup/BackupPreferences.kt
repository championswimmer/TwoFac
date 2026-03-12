package tech.arnav.twofac.lib.backup

import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.PublicApi

@PublicApi
@Serializable
data class BackupPreferences(
    val selectedAutomaticRestoreProviderId: String? = null,
    val providerMetadata: Map<String, BackupProviderMetadata> = emptyMap(),
)

@PublicApi
@Serializable
data class BackupProviderMetadata(
    val lastSuccessfulBackupAt: Long? = null,
    val lastSuccessfulRestoreAt: Long? = null,
    val lastObservedRemoteMarker: BackupRemoteMarker? = null,
    val lastConsumedRemoteMarker: BackupRemoteMarker? = null,
)

/**
 * Provider-agnostic remote snapshot marker used for deduping automatic restore.
 *
 * Suggested mappings:
 * - Google Drive appDataFolder: [remoteId] = fileId, [modifiedAt] = modifiedTime epoch seconds,
 *   [versionToken] = revision id if available.
 * - Apple CloudKit: [remoteId] = recordName, [modifiedAt] = modificationDate epoch seconds,
 *   [versionToken] = server change token / record change tag when available.
 */
@PublicApi
@Serializable
data class BackupRemoteMarker(
    val remoteId: String,
    val modifiedAt: Long? = null,
    val versionToken: String? = null,
)
