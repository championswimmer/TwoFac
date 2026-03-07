package tech.arnav.twofac.backup

import io.github.xxfast.kstore.KStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.arnav.twofac.lib.backup.BackupDescriptor
import tech.arnav.twofac.lib.backup.BackupPreferences
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.lib.backup.BackupRemoteMarker
import tech.arnav.twofac.lib.backup.recordBackupSuccess
import tech.arnav.twofac.lib.backup.recordRestoreSuccess
import tech.arnav.twofac.lib.backup.withSelectedAutomaticRestoreProvider

class BackupPreferencesManager(
    private val store: KStore<BackupPreferences>,
) {
    val updates: Flow<BackupPreferences> = store.updates.map { it ?: BackupPreferences() }

    suspend fun get(): BackupPreferences = store.get() ?: BackupPreferences()

    suspend fun setAutomaticRestoreProvider(
        providerId: String?,
        providers: List<BackupProvider>,
    ): BackupPreferences {
        return update { current ->
            current.withSelectedAutomaticRestoreProvider(
                providerId = providerId,
                availableProviders = providers.map { it.info },
            )
        }
    }

    suspend fun recordBackupSuccess(
        provider: BackupProvider,
        descriptor: BackupDescriptor,
    ): BackupPreferences {
        return update { current ->
            current.recordBackupSuccess(
                providerId = provider.info.id,
                completedAtEpochSeconds = descriptor.createdAt,
                remoteMarker = descriptor.toRemoteMarker(),
            )
        }
    }

    suspend fun recordRestoreSuccess(
        provider: BackupProvider,
        descriptor: BackupDescriptor,
    ): BackupPreferences {
        return update { current ->
            current.recordRestoreSuccess(
                providerId = provider.info.id,
                completedAtEpochSeconds = descriptor.createdAt,
                remoteMarker = descriptor.toRemoteMarker(),
            )
        }
    }

    private suspend fun update(
        transform: (BackupPreferences) -> BackupPreferences,
    ): BackupPreferences {
        val updated = transform(get())
        store.set(updated)
        return updated
    }
}

private fun BackupDescriptor.toRemoteMarker(): BackupRemoteMarker {
    return BackupRemoteMarker(
        identifier = remoteId ?: id,
        modifiedAtEpochSeconds = createdAt,
        versionTag = checksum,
    )
}
