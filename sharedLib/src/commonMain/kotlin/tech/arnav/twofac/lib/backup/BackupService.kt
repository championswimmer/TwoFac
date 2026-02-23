package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi
import tech.arnav.twofac.lib.TwoFacLib
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Orchestrates backup and restore operations using a [BackupTransport].
 *
 * Export flow: read account URIs from [TwoFacLib] → encode to [BackupPayload] → upload via transport.
 * Restore flow: download from transport → decode [BackupPayload] → import URIs into [TwoFacLib].
 */
@PublicApi
class BackupService(private val twoFacLib: TwoFacLib) {

    @OptIn(ExperimentalTime::class)
    suspend fun createBackup(transport: BackupTransport): BackupResult<BackupDescriptor> {
        val uris = try {
            twoFacLib.exportAccountURIs()
        } catch (e: Exception) {
            return BackupResult.Failure("Failed to read accounts: ${e.message}", e)
        }

        val createdAt = Clock.System.now().epochSeconds
        val payload = BackupPayload(createdAt = createdAt, accounts = uris)
        val bytes = BackupPayloadCodec.encode(payload)
        val id = "twofac-backup-$createdAt.json"
        val descriptor = BackupDescriptor(
            id = id,
            transportId = transport.id,
            createdAt = createdAt,
            byteSize = bytes.size.toLong(),
        )
        return transport.upload(bytes, descriptor)
    }

    suspend fun listBackups(transport: BackupTransport): BackupResult<List<BackupDescriptor>> {
        return transport.listBackups()
    }

    suspend fun restoreBackup(transport: BackupTransport, backupId: String): BackupResult<Int> {
        val blobResult = transport.download(backupId)
        if (blobResult is BackupResult.Failure) return blobResult

        val blob = (blobResult as BackupResult.Success).value
        val payload = try {
            BackupPayloadCodec.decode(blob.content)
        } catch (e: Exception) {
            return BackupResult.Failure("Failed to decode backup payload: ${e.message}", e)
        }

        var imported = 0
        for (uri in payload.accounts) {
            try {
                twoFacLib.addAccount(uri)
                imported++
            } catch (_: Exception) {
                // skip individual failures; caller can compare count to total
            }
        }
        return BackupResult.Success(imported)
    }
}
