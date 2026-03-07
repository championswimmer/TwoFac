package tech.arnav.twofac.lib.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupRemoteMarker(
    val identifier: String,
    val modifiedAtEpochSeconds: Long? = null,
    val versionTag: String? = null,
)

@Serializable
data class BackupProviderPreferences(
    val lastSuccessfulBackupAtEpochSeconds: Long? = null,
    val lastSuccessfulRestoreAtEpochSeconds: Long? = null,
    val lastRemoteMarker: BackupRemoteMarker? = null,
)

@Serializable
data class BackupPreferences(
    val selectedAutomaticRestoreProviderId: String? = null,
    val providerPreferences: Map<String, BackupProviderPreferences> = emptyMap(),
)

fun BackupPreferences.providerPreferences(providerId: String): BackupProviderPreferences {
    return providerPreferences[providerId] ?: BackupProviderPreferences()
}

fun BackupPreferences.withSelectedAutomaticRestoreProvider(
    providerId: String?,
    availableProviders: List<BackupProviderInfo>,
): BackupPreferences {
    if (providerId == null) {
        return copy(selectedAutomaticRestoreProviderId = null)
    }

    val provider = availableProviders.firstOrNull { it.id == providerId }
        ?: throw IllegalArgumentException("Unknown backup provider: $providerId")
    require(provider.supportsAutomaticRestore) {
        "Provider '$providerId' does not support automatic restore"
    }

    return copy(selectedAutomaticRestoreProviderId = providerId)
}

fun BackupPreferences.recordBackupSuccess(
    providerId: String,
    completedAtEpochSeconds: Long,
    remoteMarker: BackupRemoteMarker? = null,
): BackupPreferences {
    return updateProviderPreferences(providerId) { current ->
        current.copy(
            lastSuccessfulBackupAtEpochSeconds = completedAtEpochSeconds,
            lastRemoteMarker = remoteMarker ?: current.lastRemoteMarker,
        )
    }
}

fun BackupPreferences.recordRestoreSuccess(
    providerId: String,
    completedAtEpochSeconds: Long,
    remoteMarker: BackupRemoteMarker? = null,
): BackupPreferences {
    return updateProviderPreferences(providerId) { current ->
        current.copy(
            lastSuccessfulRestoreAtEpochSeconds = completedAtEpochSeconds,
            lastRemoteMarker = remoteMarker ?: current.lastRemoteMarker,
        )
    }
}

private fun BackupPreferences.updateProviderPreferences(
    providerId: String,
    transform: (BackupProviderPreferences) -> BackupProviderPreferences,
): BackupPreferences {
    val updatedPreferences = transform(providerPreferences(providerId))
    return copy(
        providerPreferences = providerPreferences + (providerId to updatedPreferences)
    )
}
