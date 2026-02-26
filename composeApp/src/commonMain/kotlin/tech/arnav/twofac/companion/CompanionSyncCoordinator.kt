package tech.arnav.twofac.companion

import tech.arnav.twofac.lib.uri.OtpAuthURI
import tech.arnav.twofac.lib.watchsync.WatchSyncAccount
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot

interface CompanionSyncCoordinator {
    val companionDisplayName: String
        get() = "Watch"

    suspend fun isCompanionActive(): Boolean
    suspend fun forceDiscoverCompanion(): Boolean = isCompanionActive()
    suspend fun syncNow(manual: Boolean): Boolean
    suspend fun onAccountsUnlocked()
    suspend fun onAccountsChanged()
}

data class CompanionSyncSourceAccount(
    val accountId: String,
    val accountLabel: String,
    val otpAuthUri: String,
)

fun buildCompanionSyncSnapshot(
    sourceAccounts: List<CompanionSyncSourceAccount>,
    generatedAtEpochSec: Long,
): WatchSyncSnapshot {
    val companionAccounts = sourceAccounts.map { source ->
        WatchSyncAccount(
            accountId = source.accountId,
            issuer = runCatching { OtpAuthURI.parse(source.otpAuthUri).issuer }.getOrNull(),
            accountLabel = source.accountLabel,
            otpAuthUri = source.otpAuthUri,
        )
    }
    return WatchSyncSnapshot(
        generatedAtEpochSec = generatedAtEpochSec,
        accounts = companionAccounts,
    )
}

fun isSyncToCompanionEnabled(
    isCompanionActive: Boolean,
    isSyncInProgress: Boolean,
): Boolean = isCompanionActive && !isSyncInProgress
