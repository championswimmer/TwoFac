package tech.arnav.twofac.lib.backup

import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.PublicApi

/**
 * Versioned payload format for TwoFac backups.
 *
 * v1 format contains a list of plaintext otpauth:// URIs.
 * All account secrets are included in plain text – use encrypted
 * transports or store backup files in a secure location.
 */
@PublicApi
@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val createdAt: Long,
    val accounts: List<String>,
)
