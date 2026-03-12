package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.otp.OTP
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
    private data class AccountFingerprint(
        val issuer: String,
        val account: String,
        val secret: String,
    )

    private var backupSequence = 0L

    suspend fun listProviders(): List<BackupProvider> = transportRegistry.providerInfo()

    @OptIn(ExperimentalTime::class)
    suspend fun createBackup(providerId: String): BackupResult<BackupDescriptor> {
        val transport = resolveTransport(providerId) ?: return BackupResult.Failure(
            "Backup provider not found: $providerId"
        )
        if (!transport.supportsManualBackup) {
            return BackupResult.Failure("Provider '$providerId' does not support manual backup")
        }

        val uris = try {
            twoFacLib.exportAccountURIs()
        } catch (e: Exception) {
            return BackupResult.Failure("Failed to read accounts: ${e.message}", e)
        }

        val createdAt = Clock.System.now().epochSeconds
        val payload = BackupPayload(createdAt = createdAt, accounts = uris)
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

    suspend fun restoreBackup(providerId: String, backupId: String): BackupResult<Int> {
        val transport = resolveTransport(providerId) ?: return BackupResult.Failure(
            "Backup provider not found: $providerId"
        )
        if (!transport.supportsManualRestore) {
            return BackupResult.Failure("Provider '$providerId' does not support manual restore")
        }

        val blobResult = transport.download(backupId)
        if (blobResult is BackupResult.Failure) return blobResult

        val blob = (blobResult as BackupResult.Success).value
        val payload = try {
            BackupPayloadCodec.decode(blob.content)
        } catch (e: Exception) {
            return BackupResult.Failure("Failed to decode backup payload: ${e.message}", e)
        }

        val uris = payload.accounts
        val parsedBackupAccounts = try {
            uris.map(OtpAuthURI::parse)
        } catch (e: Exception) {
            return BackupResult.Failure("Backup payload contains invalid account URI: ${e.message}", e)
        }
        val existingFingerprints = try {
            twoFacLib.exportAccountURIs()
                .map(OtpAuthURI::parse)
                .map(::fingerprint)
                .toMutableSet()
        } catch (e: Exception) {
            return BackupResult.Failure("Failed to read existing accounts: ${e.message}", e)
        }

        var imported = 0
        for ((uri, parsedBackupAccount) in uris.zip(parsedBackupAccounts)) {
            val backupFingerprint = fingerprint(parsedBackupAccount)
            if (backupFingerprint in existingFingerprints) continue
            try {
                val added = twoFacLib.addAccount(uri)
                if (added) {
                    existingFingerprints += backupFingerprint
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

    private fun fingerprint(otp: OTP): AccountFingerprint {
        return AccountFingerprint(
            issuer = otp.issuer ?: "",
            account = otp.accountName,
            secret = otp.secret,
        )
    }
}
