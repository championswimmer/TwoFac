package tech.arnav.twofac.lib.backup

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import kotlin.system.getTimeMillis

class BackupPayloadCodec(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
    private val cryptoProvider: CryptographyProvider = CryptographyProvider.Default,
) {

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }

    data class EncodedBackupPayload(
        val blob: BackupBlob,
        val createdAtMillis: Long,
        val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val backupName: String,
    )

    @Suppress("DEPRECATION")
    suspend fun encode(
        accounts: List<BackupAccountSnapshot>,
        appVersion: String? = null,
    ): BackupResult<EncodedBackupPayload> = try {
        val createdAt = getTimeMillis()
        val payload = BackupPayload(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            createdAtMillis = createdAt,
            appVersion = appVersion,
            accounts = accounts,
        )
        val payloadBytes = json.encodeToString(payload).encodeToByteArray()
        val checksum = checksum(payloadBytes)
        BackupResult.Success(
            EncodedBackupPayload(
                blob = BackupBlob(
                    bytes = payloadBytes,
                    contentType = "application/json",
                    checksum = checksum,
                ),
                createdAtMillis = createdAt,
                schemaVersion = CURRENT_SCHEMA_VERSION,
                backupName = "twofac-backup-$createdAt.json",
            )
        )
    } catch (e: Exception) {
        BackupResult.Failure(
            BackupError(
                code = BackupErrorCode.SerializationError,
                message = "Failed to encode backup payload: ${e.message}",
                cause = e,
            )
        )
    }

    fun decode(blob: BackupBlob): BackupResult<BackupPayload> {
        return try {
            val payload = json.decodeFromString<BackupPayload>(blob.bytes.decodeToString())
            if (payload.schemaVersion != CURRENT_SCHEMA_VERSION) {
                BackupResult.Failure(
                    BackupError(
                        code = BackupErrorCode.ValidationError,
                        message = "Unsupported backup schema version ${payload.schemaVersion}",
                    )
                )
            } else {
                BackupResult.Success(payload)
            }
        } catch (e: SerializationException) {
            BackupResult.Failure(
                BackupError(
                    code = BackupErrorCode.SerializationError,
                    message = "Failed to decode backup payload: ${e.message}",
                    cause = e,
                )
            )
        } catch (e: Exception) {
            BackupResult.Failure(
                BackupError(
                    code = BackupErrorCode.Unknown,
                    message = "Failed to decode backup payload",
                    cause = e,
                )
            )
        }
    }

    private suspend fun checksum(bytes: ByteArray): String {
        val hasher = cryptoProvider.get(SHA256)
        val digest = hasher.hasher().hash(bytes)
        return digest.joinToString("") { each ->
            each.toUByte().toString(16).padStart(2, '0')
        }
    }
}
