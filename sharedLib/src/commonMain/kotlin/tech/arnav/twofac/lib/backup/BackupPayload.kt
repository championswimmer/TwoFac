package tech.arnav.twofac.lib.backup

import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.PublicApi

/**
 * Versioned payload format for TwoFac backups.
 *
 * v1 format contains a list of plaintext otpauth:// URIs.
 * v2 adds support for carrying already-encrypted stored account entries.
 */
@PublicApi
@Serializable
data class EncryptedAccountEntry(
    val accountLabel: String,
    val salt: String,
    val encryptedURI: String,
)

@PublicApi
@Serializable
data class BackupPayload(
    val schemaVersion: Int = 2,
    val createdAt: Long,
    val encrypted: Boolean = false,
    val accounts: List<String> = emptyList(),
    val encryptedAccounts: List<EncryptedAccountEntry> = emptyList(),
)
