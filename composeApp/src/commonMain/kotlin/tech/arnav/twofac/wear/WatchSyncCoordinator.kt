package tech.arnav.twofac.wear

import tech.arnav.twofac.lib.uri.OtpAuthURI
import tech.arnav.twofac.lib.watchsync.WatchSyncAccount
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot

interface WatchSyncCoordinator {
    suspend fun isCompanionActive(): Boolean
    suspend fun syncNow(manual: Boolean): Boolean
    suspend fun onAccountsUnlocked()
    suspend fun onAccountsChanged()
}

data class WatchSyncSourceAccount(
    val accountId: String,
    val accountLabel: String,
    val otpAuthUri: String,
)

fun buildWatchSyncSnapshot(
    sourceAccounts: List<WatchSyncSourceAccount>,
    generatedAtEpochSec: Long,
): WatchSyncSnapshot {
    val watchAccounts = sourceAccounts.map { source ->
        WatchSyncAccount(
            accountId = source.accountId,
            issuer = runCatching { OtpAuthURI.parse(source.otpAuthUri).issuer }.getOrNull(),
            accountLabel = source.accountLabel,
            otpAuthUri = source.otpAuthUri,
        )
    }
    return WatchSyncSnapshot(
        generatedAtEpochSec = generatedAtEpochSec,
        accounts = watchAccounts,
    )
}

fun isSyncToWatchEnabled(
    isCompanionActive: Boolean,
    isSyncInProgress: Boolean,
): Boolean = isCompanionActive && !isSyncInProgress
