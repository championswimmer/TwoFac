package tech.arnav.twofac.lib.watchsync

import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.TwoFacLib
import kotlin.time.Clock

@OptIn(kotlin.time.ExperimentalTime::class)
@PublicApi
object IosWatchSyncHelper {

    /**
     * Generates a JSON payload string of all current accounts suitable for syncing to watchOS.
     * Returns null if the library is locked or there are no accounts to sync.
     */
    @Throws(Exception::class)
    suspend fun getWatchSyncPayloadString(lib: TwoFacLib): String? {
        if (!lib.isUnlocked()) return null
        
        val accounts = lib.getAllAccounts()
        if (accounts.isEmpty()) return null
        
        val uris = lib.exportAccountURIs()
        
        val watchAccounts = accounts.zip(uris).map { (account, uri) ->
            WatchSyncAccount(
                accountId = account.accountID,
                issuer = runCatching { tech.arnav.twofac.lib.uri.OtpAuthURI.parse(uri).issuer }.getOrNull(),
                accountLabel = account.accountLabel,
                otpAuthUri = uri
            )
        }
        
        val snapshot = WatchSyncSnapshot(
            generatedAtEpochSec = Clock.System.now().epochSeconds,
            accounts = watchAccounts
        )
        
        return kotlinx.serialization.json.Json.encodeToString(WatchSyncSnapshot.serializer(), snapshot)
    }

    /**
     * Decodes a JSON payload string received from iOS into a WatchSyncSnapshot.
     */
    @Throws(Exception::class)
    fun decodeWatchSyncPayloadString(payload: String): WatchSyncSnapshot {
        val decoded = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(WatchSyncSnapshot.serializer(), payload)
        require(decoded.version == WatchSyncContract.SCHEMA_VERSION) {
            "Unsupported watch sync schema version: ${decoded.version}"
        }
        return decoded
    }
}
