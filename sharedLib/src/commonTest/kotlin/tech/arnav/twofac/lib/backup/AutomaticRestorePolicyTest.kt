package tech.arnav.twofac.lib.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AutomaticRestorePolicyTest {

    private fun provider(
        id: String,
        supportsAutomaticRestore: Boolean,
    ): BackupProvider {
        return BackupProvider(
            id = id,
            displayName = id,
            supportsManualBackup = true,
            supportsManualRestore = true,
            supportsAutomaticRestore = supportsAutomaticRestore,
            requiresAuthentication = false,
            isAvailable = true,
        )
    }

    @Test
    fun testAutomaticRestoreIsDisabledByDefault() {
        val decision = AutomaticRestorePolicy.evaluateAutomaticRestore(
            preferences = BackupPreferences(),
            localAccountCount = 0,
            remoteMarker = BackupRemoteMarker(remoteId = "remote-1"),
        )

        assertEquals(AutomaticRestoreDecision.DISABLED, decision)
    }

    @Test
    fun testAutomaticRestoreSelectionAllowsEligibleProvider() {
        val providers = listOf(
            provider(BackupProviderIds.LOCAL, supportsAutomaticRestore = false),
            provider(BackupProviderIds.ICLOUD, supportsAutomaticRestore = true),
        )

        val result = AutomaticRestorePolicy.selectAutomaticRestoreProvider(
            preferences = BackupPreferences(),
            providers = providers,
            providerId = BackupProviderIds.ICLOUD,
        )

        val success = assertIs<BackupResult.Success<BackupPreferences>>(result)
        assertEquals(
            BackupProviderIds.ICLOUD,
            success.value.selectedAutomaticRestoreProviderId,
        )
    }

    @Test
    fun testAutomaticRestoreSelectionRejectsIneligibleProvider() {
        val providers = listOf(provider(BackupProviderIds.LOCAL, supportsAutomaticRestore = false))

        val result = AutomaticRestorePolicy.selectAutomaticRestoreProvider(
            preferences = BackupPreferences(),
            providers = providers,
            providerId = BackupProviderIds.LOCAL,
        )

        assertIs<BackupResult.Failure>(result)
    }

    @Test
    fun testAutomaticRestoreSelectionCanBeCleared() {
        val providers = listOf(provider(BackupProviderIds.ICLOUD, supportsAutomaticRestore = true))

        val selected = AutomaticRestorePolicy.selectAutomaticRestoreProvider(
            preferences = BackupPreferences(),
            providers = providers,
            providerId = BackupProviderIds.ICLOUD,
        )
        val selectedPreferences = assertIs<BackupResult.Success<BackupPreferences>>(selected).value

        val cleared = AutomaticRestorePolicy.selectAutomaticRestoreProvider(
            preferences = selectedPreferences,
            providers = providers,
            providerId = null,
        )
        val clearedPreferences = assertIs<BackupResult.Success<BackupPreferences>>(cleared).value

        assertEquals(null, clearedPreferences.selectedAutomaticRestoreProviderId)
    }

    @Test
    fun testAutomaticRestoreSelectionKeepsSingleProviderOnly() {
        val providers = listOf(
            provider(BackupProviderIds.ICLOUD, supportsAutomaticRestore = true),
            provider(BackupProviderIds.GOOGLE_DRIVE_APPDATA, supportsAutomaticRestore = true),
        )

        val first = AutomaticRestorePolicy.selectAutomaticRestoreProvider(
            preferences = BackupPreferences(),
            providers = providers,
            providerId = BackupProviderIds.ICLOUD,
        )
        val firstSuccess = assertIs<BackupResult.Success<BackupPreferences>>(first)

        val second = AutomaticRestorePolicy.selectAutomaticRestoreProvider(
            preferences = firstSuccess.value,
            providers = providers,
            providerId = BackupProviderIds.GOOGLE_DRIVE_APPDATA,
        )
        val secondSuccess = assertIs<BackupResult.Success<BackupPreferences>>(second)
        assertEquals(
            BackupProviderIds.GOOGLE_DRIVE_APPDATA,
            secondSuccess.value.selectedAutomaticRestoreProviderId,
        )
    }

    @Test
    fun testAutomaticRestoreRequiresConfirmationForNonEmptyVault() {
        val preferences = BackupPreferences(selectedAutomaticRestoreProviderId = BackupProviderIds.ICLOUD)
        val marker = BackupRemoteMarker(remoteId = "record-1", modifiedAt = 123)

        val decisionWithoutConfirmation = AutomaticRestorePolicy.evaluateAutomaticRestore(
            preferences = preferences,
            localAccountCount = 2,
            remoteMarker = marker,
            userConfirmedOnNonEmptyVault = false,
        )
        assertEquals(AutomaticRestoreDecision.REQUIRES_USER_CONFIRMATION, decisionWithoutConfirmation)

        val decisionWithConfirmation = AutomaticRestorePolicy.evaluateAutomaticRestore(
            preferences = preferences,
            localAccountCount = 2,
            remoteMarker = marker,
            userConfirmedOnNonEmptyVault = true,
        )
        assertEquals(AutomaticRestoreDecision.READY, decisionWithConfirmation)
    }

    @Test
    fun testAutomaticRestoreSkipsAlreadyConsumedMarker() {
        val marker = BackupRemoteMarker(remoteId = "file-123", modifiedAt = 456)
        val preferences = BackupPreferences(
            selectedAutomaticRestoreProviderId = BackupProviderIds.GOOGLE_DRIVE_APPDATA,
            providerMetadata = mapOf(
                BackupProviderIds.GOOGLE_DRIVE_APPDATA to BackupProviderMetadata(
                    lastConsumedRemoteMarker = marker,
                )
            ),
        )

        val decision = AutomaticRestorePolicy.evaluateAutomaticRestore(
            preferences = preferences,
            localAccountCount = 0,
            remoteMarker = marker,
        )

        assertEquals(AutomaticRestoreDecision.ALREADY_CONSUMED, decision)
    }

    @Test
    fun testProviderCapabilityRulesForKnownProviders() {
        assertEquals(
            BackupProviderCapabilities(true, true, false),
            BackupProviderCapabilityRules.expectedCapabilitiesFor(BackupProviderIds.LOCAL),
        )
        assertEquals(
            BackupProviderCapabilities(true, true, true),
            BackupProviderCapabilityRules.expectedCapabilitiesFor(BackupProviderIds.ICLOUD),
        )
        assertEquals(
            BackupProviderCapabilities(true, true, true),
            BackupProviderCapabilityRules.expectedCapabilitiesFor(BackupProviderIds.GOOGLE_DRIVE_APPDATA),
        )
    }
}
