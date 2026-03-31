package tech.arnav.twofac.lib.watchsync

import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.presentation.issuer.IssuerIconCatalog
import kotlin.time.Clock

@OptIn(kotlin.time.ExperimentalTime::class)
@PublicApi
object IosWatchSyncHelper {

    /**
     * Generates a JSON payload string of all current accounts suitable for syncing to watchOS.
     * Returns null if the library is locked or there are no accounts to sync.
     *
     * Delegates encoding to [WatchSyncSnapshotCodec] so that `encodeDefaults = true` is
     * applied consistently — ensuring the `version` field is always present in the payload.
     */
    @Throws(Exception::class)
    suspend fun getWatchSyncPayloadString(lib: TwoFacLib): String? {
        if (!lib.isUnlocked()) return null

        val accounts = lib.getAllAccounts()
        if (accounts.isEmpty()) return null

        val uris = lib.exportAccountsPlaintext()

        val watchAccounts = accounts.zip(uris).map { (account, uri) ->
            val issuer = account.issuer ?: runCatching { tech.arnav.twofac.lib.uri.OtpAuthURI.parse(uri).issuer }.getOrNull()
            WatchSyncAccount(
                accountId = account.accountID,
                issuer = issuer,
                issuerIconKey = IssuerIconCatalog.resolveIssuerIconKey(issuer),
                accountLabel = account.accountLabel,
                otpAuthUri = uri
            )
        }

        val snapshot = WatchSyncSnapshot(
            generatedAtEpochSec = Clock.System.now().epochSeconds,
            accounts = watchAccounts
        )

        return WatchSyncSnapshotCodec.encode(snapshot).decodeToString()
    }

    /**
     * Decodes a JSON payload string received from iOS into a [WatchSyncSnapshot].
     *
     * Delegates to [WatchSyncSnapshotCodec] so the schema-version check and
     * `encodeDefaults = true` behaviour are handled in one place.
     */
    @Throws(Exception::class)
    fun decodeWatchSyncPayloadString(payload: String): WatchSyncSnapshot {
        return WatchSyncSnapshotCodec.decode(payload.encodeToByteArray())
    }
}
