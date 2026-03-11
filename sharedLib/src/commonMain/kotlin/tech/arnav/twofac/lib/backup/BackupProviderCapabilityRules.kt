package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

@PublicApi
object BackupProviderIds {
    const val LOCAL = "local"
    const val ICLOUD = "icloud"
    const val GOOGLE_DRIVE_APPDATA = "gdrive-appdata"
}

@PublicApi
data class BackupProviderCapabilities(
    val supportsManualBackup: Boolean,
    val supportsManualRestore: Boolean,
    val supportsAutomaticRestore: Boolean,
)

@PublicApi
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
