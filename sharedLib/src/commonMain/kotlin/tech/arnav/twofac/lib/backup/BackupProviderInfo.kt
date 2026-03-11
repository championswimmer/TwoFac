package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

@PublicApi
data class BackupProviderInfo(
    val id: String,
    val displayName: String,
    val supportsManualBackup: Boolean,
    val supportsManualRestore: Boolean,
    val supportsAutomaticRestore: Boolean,
    val requiresAuthentication: Boolean,
    val isAvailable: Boolean,
)
