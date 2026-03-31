package tech.arnav.twofac.lib.backup

/**
 * Test-only data class capturing the expected capabilities of a backup provider.
 *
 * Used exclusively by [BackupRestorePolicyTest] to verify that provider
 * implementations declare the right capability flags.
 */
data class BackupProviderCapabilities(
    val supportsManualBackup: Boolean,
    val supportsManualRestore: Boolean,
    val supportsAutomaticRestore: Boolean,
)

/**
 * Test-only helper that codifies the expected capability matrix for well-known providers.
 *
 * Lives in `commonTest` so it does not pollute the production ABI surface.
 */
object BackupProviderCapabilityRules {
    private val rules = mapOf(
        BackupProviderIds.LOCAL to BackupProviderCapabilities(
            supportsManualBackup = true,
            supportsManualRestore = true,
            supportsAutomaticRestore = false,
        ),
        BackupProviderIds.ICLOUD to BackupProviderCapabilities(
            supportsManualBackup = true,
            supportsManualRestore = true,
            supportsAutomaticRestore = true,
        ),
        BackupProviderIds.GOOGLE_DRIVE_APPDATA to BackupProviderCapabilities(
            supportsManualBackup = true,
            supportsManualRestore = true,
            supportsAutomaticRestore = true,
        ),
    )

    fun expectedCapabilitiesFor(providerId: String): BackupProviderCapabilities? {
        return rules[providerId]
    }
}
