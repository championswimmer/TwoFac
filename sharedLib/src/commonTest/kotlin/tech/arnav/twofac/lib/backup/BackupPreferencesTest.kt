package tech.arnav.twofac.lib.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BackupPreferencesTest {
    private val localProvider = LocalBackupProviderInfo
    private val iCloudProvider = ICloudBackupProviderInfo

    @Test
    fun onlyAutomaticRestoreEligibleProvidersCanBeSelected() {
        val error = assertFailsWith<IllegalArgumentException> {
            BackupPreferences().withSelectedAutomaticRestoreProvider(
                providerId = localProvider.id,
                availableProviders = listOf(localProvider, iCloudProvider),
            )
        }

        assertEquals("Provider 'local' does not support automatic restore", error.message)
    }

    @Test
    fun selectingAnotherProviderReplacesPreviousAutomaticRestoreSelection() {
        val preferences = BackupPreferences(
            selectedAutomaticRestoreProviderId = iCloudProvider.id,
        ).withSelectedAutomaticRestoreProvider(
            providerId = GoogleDriveAppDataBackupProviderInfo.id,
            availableProviders = listOf(localProvider, iCloudProvider, GoogleDriveAppDataBackupProviderInfo),
        )

        assertEquals(
            GoogleDriveAppDataBackupProviderInfo.id,
            preferences.selectedAutomaticRestoreProviderId,
        )
    }

    @Test
    fun clearingAutomaticRestoreLeavesNoProviderSelected() {
        val preferences = BackupPreferences(
            selectedAutomaticRestoreProviderId = iCloudProvider.id,
        ).withSelectedAutomaticRestoreProvider(
            providerId = null,
            availableProviders = listOf(localProvider, iCloudProvider),
        )

        assertNull(preferences.selectedAutomaticRestoreProviderId)
    }

    @Test
    fun providerSpecificMetadataTracksBackupAndRestoreMarkers() {
        val marker = BackupRemoteMarker(
            identifier = "backup-1",
            modifiedAtEpochSeconds = 1234,
            versionTag = "etag-1",
        )

        val preferences = BackupPreferences()
            .recordBackupSuccess(
                providerId = iCloudProvider.id,
                completedAtEpochSeconds = 1000,
                remoteMarker = marker,
            )
            .recordRestoreSuccess(
                providerId = iCloudProvider.id,
                completedAtEpochSeconds = 2000,
            )

        val providerState = preferences.providerPreferences(iCloudProvider.id)
        assertEquals(1000, providerState.lastSuccessfulBackupAtEpochSeconds)
        assertEquals(2000, providerState.lastSuccessfulRestoreAtEpochSeconds)
        assertEquals(marker, providerState.lastRemoteMarker)
    }
}
