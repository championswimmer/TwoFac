package tech.arnav.twofac.lib.backup

import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.crypto.CryptoTools

/**
 * Versioned payload format for TwoFac backups.
 *
 * v1 format contains a list of plaintext otpauth:// URIs.
 * v2 adds support for carrying already-encrypted stored account entries.
 * Plaintext backups (v1 and v2 with encrypted=false) contain secrets in cleartext.
 */
@PublicApi
@Serializable
data class EncryptedAccountEntry(
    val accountLabel: String,
    val salt: String,
    val encryptedURI: String,
    val iterations: Int = CryptoTools.LEGACY_HASH_ITERATIONS,
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
