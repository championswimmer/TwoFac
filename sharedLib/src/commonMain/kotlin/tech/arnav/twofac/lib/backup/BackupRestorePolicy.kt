package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

/**
 * Outcome of evaluating whether a backup restore flow should start automatically.
 */
@PublicApi
enum class BackupRestoreDecison {
    /** No automatic restore provider is configured in preferences. */
    DISABLED,

    /** A provider is configured, but the remote source currently has no marker to restore from. */
    NO_REMOTE_MARKER,

    /** The current remote marker has already been consumed by a successful restore. */
    ALREADY_CONSUMED,

    /** A restore is possible, but user confirmation is required because local data exists. */
    REQUIRES_USER_CONFIRMATION,

    /** Preconditions are satisfied and restore can proceed immediately. */
    READY,
}

/**
 * Central policy for selecting, evaluating, and recording backup restore behavior.
 *
 * The policy is intentionally stateless; callers provide [BackupPreferences] and contextual
 * inputs, and receive either updated preferences or a decision describing the next action.
 */
@PublicApi
object BackupRestorePolicy {

    /**
     * Validates and stores the provider selected for automatic restore.
     *
     * Passing `null` clears the current selection.
     */
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

    /**
     * Evaluates whether an automatic restore should run for the currently selected provider.
     *
     * Decision order:
     * 1. Ensure automatic restore is enabled by selection.
     * 2. Ensure a remote marker exists.
     * 3. Skip markers already restored in the past.
     * 4. Require user confirmation when local accounts already exist.
     * 5. Otherwise, allow restore to start.
     */
    fun evaluateAutomaticRestore(
        preferences: BackupPreferences,
        localAccountCount: Int,
        remoteMarker: BackupRemoteMarker?,
        userConfirmedOnNonEmptyVault: Boolean = false,
    ): BackupRestoreDecison {
        require(localAccountCount >= 0) { "localAccountCount cannot be negative" }

        val providerId = preferences.selectedAutomaticRestoreProviderId
            ?: return BackupRestoreDecison.DISABLED

        if (remoteMarker == null) {
            return BackupRestoreDecison.NO_REMOTE_MARKER
        }

        val metadata = preferences.providerMetadata[providerId]
        if (metadata?.lastConsumedRemoteMarker == remoteMarker) {
            return BackupRestoreDecison.ALREADY_CONSUMED
        }

        if (localAccountCount > 0 && !userConfirmedOnNonEmptyVault) {
            return BackupRestoreDecison.REQUIRES_USER_CONFIRMATION
        }

        return BackupRestoreDecison.READY
    }

    /**
     * Records the timestamp of a successful backup for [providerId].
     */
    fun markSuccessfulBackup(
        preferences: BackupPreferences,
        providerId: String,
        completedAt: Long,
    ): BackupPreferences {
        return preferences.updateProviderMetadata(providerId) {
            it.copy(lastSuccessfulBackupAt = completedAt)
        }
    }

    /**
     * Records the latest remote marker observed for [providerId], regardless of restore outcome.
     */
    fun markObservedRemoteMarker(
        preferences: BackupPreferences,
        providerId: String,
        marker: BackupRemoteMarker,
    ): BackupPreferences {
        return preferences.updateProviderMetadata(providerId) {
            it.copy(lastObservedRemoteMarker = marker)
        }
    }

    /**
     * Records successful restore metadata and optionally marks a remote marker as consumed.
     */
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

    /**
     * Utility for atomically updating per-provider metadata within immutable preferences.
     */
    private inline fun BackupPreferences.updateProviderMetadata(
        providerId: String,
        transform: (BackupProviderMetadata) -> BackupProviderMetadata,
    ): BackupPreferences {
        val existing = providerMetadata[providerId] ?: BackupProviderMetadata()
        val updated = transform(existing)
        return copy(providerMetadata = providerMetadata + (providerId to updated))
    }
}
