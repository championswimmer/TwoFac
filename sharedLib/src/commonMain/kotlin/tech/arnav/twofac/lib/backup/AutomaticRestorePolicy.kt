package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

@PublicApi
enum class AutomaticRestoreDecision {
    DISABLED,
    NO_REMOTE_MARKER,
    ALREADY_CONSUMED,
    REQUIRES_USER_CONFIRMATION,
    READY,
}

@PublicApi
object AutomaticRestorePolicy {

    fun selectAutomaticRestoreProvider(
        preferences: BackupPreferences,
        providers: List<BackupProvider>,
        providerId: String?,
    ): BackupResult<BackupPreferences> {
        if (providerId == null) {
            return BackupResult.Success(
                preferences.copy(selectedAutomaticRestoreProviderId = null)
            )
        }

        val provider = providers.firstOrNull { it.id == providerId }
            ?: return BackupResult.Failure("Backup provider not found: $providerId")

        if (!provider.supportsAutomaticRestore) {
            return BackupResult.Failure("Provider '$providerId' does not support automatic restore")
        }

        return BackupResult.Success(
            preferences.copy(selectedAutomaticRestoreProviderId = providerId)
        )
    }

    fun evaluateAutomaticRestore(
        preferences: BackupPreferences,
        localAccountCount: Int,
        remoteMarker: BackupRemoteMarker?,
        userConfirmedOnNonEmptyVault: Boolean = false,
    ): AutomaticRestoreDecision {
        require(localAccountCount >= 0) { "localAccountCount cannot be negative" }

        val providerId = preferences.selectedAutomaticRestoreProviderId
            ?: return AutomaticRestoreDecision.DISABLED

        if (remoteMarker == null) {
            return AutomaticRestoreDecision.NO_REMOTE_MARKER
        }

        val metadata = preferences.providerMetadata[providerId]
        if (metadata?.lastConsumedRemoteMarker == remoteMarker) {
            return AutomaticRestoreDecision.ALREADY_CONSUMED
        }

        if (localAccountCount > 0 && !userConfirmedOnNonEmptyVault) {
            return AutomaticRestoreDecision.REQUIRES_USER_CONFIRMATION
        }

        return AutomaticRestoreDecision.READY
    }

    fun markSuccessfulBackup(
        preferences: BackupPreferences,
        providerId: String,
        completedAt: Long,
    ): BackupPreferences {
        return preferences.updateProviderMetadata(providerId) {
            it.copy(lastSuccessfulBackupAt = completedAt)
        }
    }

    fun markObservedRemoteMarker(
        preferences: BackupPreferences,
        providerId: String,
        marker: BackupRemoteMarker,
    ): BackupPreferences {
        return preferences.updateProviderMetadata(providerId) {
            it.copy(lastObservedRemoteMarker = marker)
        }
    }

    fun markSuccessfulRestore(
        preferences: BackupPreferences,
        providerId: String,
        completedAt: Long,
        consumedRemoteMarker: BackupRemoteMarker?,
    ): BackupPreferences {
        return preferences.updateProviderMetadata(providerId) {
            it.copy(
                lastSuccessfulRestoreAt = completedAt,
                lastConsumedRemoteMarker = consumedRemoteMarker,
            )
        }
    }

    private inline fun BackupPreferences.updateProviderMetadata(
        providerId: String,
        transform: (BackupProviderMetadata) -> BackupProviderMetadata,
    ): BackupPreferences {
        val existing = providerMetadata[providerId] ?: BackupProviderMetadata()
        val updated = transform(existing)
        return copy(providerMetadata = providerMetadata + (providerId to updated))
    }
}
