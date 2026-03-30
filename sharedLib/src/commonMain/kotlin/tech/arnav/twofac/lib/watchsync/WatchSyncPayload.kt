package tech.arnav.twofac.lib.watchsync

import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.presentation.issuer.IssuerIconCatalog

@Serializable
data class WatchSyncSnapshot(
    val version: Int = WatchSyncContract.SCHEMA_VERSION,
    val generatedAtEpochSec: Long,
    val accounts: List<WatchSyncAccount>,
)

@Serializable
data class WatchSyncAccount(
    val accountId: String,
    val issuer: String?,
    val issuerIconKey: String = IssuerIconCatalog.resolveIssuerIconKey(issuer),
    val accountLabel: String,
    val otpAuthUri: String,
)
