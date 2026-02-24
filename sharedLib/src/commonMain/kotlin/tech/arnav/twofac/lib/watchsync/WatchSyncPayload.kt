package tech.arnav.twofac.lib.watchsync

import kotlinx.serialization.Serializable

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
    val accountLabel: String,
    val otpAuthUri: String,
)
