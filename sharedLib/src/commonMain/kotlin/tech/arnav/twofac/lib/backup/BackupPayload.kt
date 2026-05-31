package tech.arnav.twofac.lib.backup

import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.theme.AccountColorTag

/**
 * Versioned payload format for TwoFac backups.
 *
 * v1 format contains a list of plaintext otpauth:// URIs.
 * v2 adds support for carrying already-encrypted stored account entries.
 * v3 adds optional account color metadata for both plaintext and encrypted entries.
 * Plaintext backups contain secrets in cleartext.
 */
@PublicApi
@Serializable
data class PlaintextAccountEntry(
    val uri: String,
    val color: AccountColorTag? = null,
)

@PublicApi
@Serializable
data class EncryptedAccountEntry(
    val accountLabel: String,
    val salt: String,
    val encryptedURI: String,
    val color: AccountColorTag? = null,
)

@PublicApi
@Serializable
data class BackupPayload(
    val schemaVersion: Int = 3,
    val createdAt: Long,
    val encrypted: Boolean = false,
    val accounts: List<String> = emptyList(),
    val plaintextAccounts: List<PlaintextAccountEntry> = emptyList(),
    val encryptedAccounts: List<EncryptedAccountEntry> = emptyList(),
)
