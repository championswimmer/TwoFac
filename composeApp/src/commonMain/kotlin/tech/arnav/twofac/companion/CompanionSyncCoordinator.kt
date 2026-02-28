package tech.arnav.twofac.companion

import tech.arnav.twofac.lib.uri.OtpAuthURI
import tech.arnav.twofac.lib.watchsync.WatchSyncAccount
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot
import tech.arnav.twofac.lib.TwoFacLib

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

suspend fun loadCompanionSyncSourceAccounts(
    twoFacLib: TwoFacLib,
    onEmpty: (suspend () -> Unit)? = null,
): List<CompanionSyncSourceAccount>? {
    val accounts = twoFacLib.getAllAccounts()
    if (accounts.isEmpty()) {
        onEmpty?.invoke()
        return null
    }
    val uris = twoFacLib.exportAccountURIs()
    return accounts.zip(uris).map { (account, uri) ->
        CompanionSyncSourceAccount(
            accountId = account.accountID,
            accountLabel = account.accountLabel,
            otpAuthUri = uri,
        )
    }
}

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
