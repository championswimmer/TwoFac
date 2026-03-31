package tech.arnav.twofac.companion

import kotlinx.coroutines.flow.StateFlow
import tech.arnav.twofac.lib.uri.OtpAuthURI
import tech.arnav.twofac.lib.watchsync.WatchSyncAccount
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.presentation.issuer.IssuerIconCatalog

interface CompanionSyncCoordinator {
    val companionDisplayName: String
        get() = "Watch"

    /** Reactive flow of companion availability. Null on platforms without watch support. */
    val companionActiveFlow: StateFlow<Boolean>?
        get() = null

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
    val uris = twoFacLib.exportAccountsPlaintext()
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
        val issuer = runCatching { OtpAuthURI.parse(source.otpAuthUri).issuer }.getOrNull()
        WatchSyncAccount(
            accountId = source.accountId,
            issuer = issuer,
            issuerIconKey = IssuerIconCatalog.resolveIssuerIconKey(issuer),
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
