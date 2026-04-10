package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.otp.isEquivalent
import tech.arnav.twofac.lib.uri.OtpAuthURI
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Orchestrates backup and restore operations using transports from [BackupTransportRegistry].
 *
 * Export flow: read account URIs from [TwoFacLib] → encode to [BackupPayload] → upload via transport.
 * Restore flow: download from transport → decode [BackupPayload] → import URIs into [TwoFacLib].
 */
@PublicApi
class BackupService(
    private val twoFacLib: TwoFacLib,
    private val transportRegistry: BackupTransportRegistry,
) {
    private var backupSequence = 0L

    suspend fun listProviders(): List<BackupProvider> = transportRegistry.providerInfo()

    @OptIn(ExperimentalTime::class)
    suspend fun createBackup(providerId: String, encrypted: Boolean = false): BackupResult<BackupDescriptor> {
        val transport = resolveTransport(providerId) ?: return BackupResult.Failure(
            "Backup provider not found: $providerId"
        )
        if (!transport.supportsManualBackup) {
            return BackupResult.Failure("Provider '$providerId' does not support manual backup")
        }

        val payload = try {
            if (encrypted) {
                val encryptedAccounts = twoFacLib.exportAccountsEncrypted().map { account ->
                    EncryptedAccountEntry(
                        accountLabel = account.accountLabel,
                        salt = account.salt,
                        encryptedURI = account.encryptedURI,
                        iterations = account.iterations,
                    )
                }
                BackupPayload(
                    createdAt = Clock.System.now().epochSeconds,
                    encrypted = true,
                    encryptedAccounts = encryptedAccounts,
                )
            } else {
                val uris = twoFacLib.exportAccountsPlaintext()
                BackupPayload(
                    createdAt = Clock.System.now().epochSeconds,
                    accounts = uris,
                )
            }
        } catch (e: Throwable) {
            return BackupResult.Failure("Failed to read accounts: ${e.message}", e)
        }

        val createdAt = payload.createdAt
        val bytes = BackupPayloadCodec.encode(payload)
        val id = "twofac-backup-$createdAt-${backupSequence++}.json"
        val descriptor = BackupDescriptor(
            id = id,
            transportId = transport.id,
            createdAt = createdAt,
            byteSize = bytes.size.toLong(),
        )
        return transport.upload(bytes, descriptor)
    }

    suspend fun listBackups(providerId: String): BackupResult<List<BackupDescriptor>> {
        val transport = resolveTransport(providerId) ?: return BackupResult.Failure(
            "Backup provider not found: $providerId"
        )
        if (!transport.supportsManualRestore) {
            return BackupResult.Failure("Provider '$providerId' does not support manual restore")
        }
        return transport.listBackups()
    }

    suspend fun inspectBackup(providerId: String, backupId: String): BackupResult<BackupPayload> {
        val transport = resolveTransport(providerId) ?: return BackupResult.Failure(
            "Backup provider not found: $providerId"
        )
        if (!transport.supportsManualRestore) {
            return BackupResult.Failure("Provider '$providerId' does not support manual restore")
        }
        val blobResult = transport.download(backupId)
        if (blobResult is BackupResult.Failure) return blobResult

        val blob = (blobResult as BackupResult.Success).value
        return try {
            BackupResult.Success(BackupPayloadCodec.decode(blob.content))
        } catch (e: Throwable) {
            BackupResult.Failure("Failed to decode backup payload: ${e.message}", e)
        }
    }

    suspend fun restoreBackup(
        providerId: String,
        backupId: String,
        backupPasskey: String? = null,
        currentPasskey: String? = null,
    ): BackupResult<Int> {
        val transport = resolveTransport(providerId) ?: return BackupResult.Failure(
            "Backup provider not found: $providerId"
        )
        if (!transport.supportsManualRestore) {
            return BackupResult.Failure("Provider '$providerId' does not support manual restore")
        }

        val inspectResult = inspectBackup(providerId, backupId)
        if (inspectResult is BackupResult.Failure) return inspectResult
        val payload = (inspectResult as BackupResult.Success).value
        val normalizedCurrentPasskey = currentPasskey?.takeIf(String::isNotBlank)
        if (normalizedCurrentPasskey != null) {
            try {
                twoFacLib.unlock(normalizedCurrentPasskey)
            } catch (e: Throwable) {
                return BackupResult.Failure("Failed to unlock app storage: ${e.message}", e)
            }
        }

        val uris = if (payload.encrypted) {
            val normalizedBackupPasskey = backupPasskey?.takeIf(String::isNotBlank)
                ?: return BackupResult.Failure("Encrypted backups require the backup passkey")
            if (normalizedCurrentPasskey == null) {
                return BackupResult.Failure("Encrypted backups require the current app passkey")
            }
            val decryptedUris = try {
                payload.encryptedAccounts.map { entry ->
                    twoFacLib.decryptEncryptedBackupAccount(entry, normalizedBackupPasskey)
                }
            } catch (e: Throwable) {
                return BackupResult.Failure(
                    "Incorrect backup passkey — could not decrypt the backup accounts.",
                    e,
                )
            }
            decryptedUris
        } else {
            payload.accounts
        }
        val parsedBackupAccounts = try {
            uris.map(OtpAuthURI::parse)
        } catch (e: Throwable) {
            val message = if (payload.encrypted) {
                "Incorrect backup passkey — could not decrypt the backup accounts."
            } else {
                "Backup payload contains invalid account URI: ${e.message}"
            }
            return BackupResult.Failure(message, e)
        }
        val existingAccounts = try {
            if (!twoFacLib.isUnlocked()) {
                return BackupResult.Failure(
                    "Plaintext backups require an unlocked vault or current app passkey"
                )
            }
            twoFacLib.exportAccountsPlaintext()
                .map(OtpAuthURI::parse)
                .toMutableList()
        } catch (e: Throwable) {
            return BackupResult.Failure("Failed to read existing accounts: ${e.message}", e)
        }

        var imported = 0
        for ((uri, parsedBackupAccount) in uris.zip(parsedBackupAccounts)) {
            if (existingAccounts.any { it.isEquivalent(parsedBackupAccount) }) continue
            try {
                val added = twoFacLib.addAccount(uri)
                if (added) {
                    existingAccounts += parsedBackupAccount
                    imported++
                }
            } catch (_: Exception) {
                // skip individual failures; caller can compare count to total
            }
        }
        return BackupResult.Success(imported)
    }

    private fun resolveTransport(providerId: String): BackupTransport? {
        return transportRegistry.findById(providerId)
    }
}
