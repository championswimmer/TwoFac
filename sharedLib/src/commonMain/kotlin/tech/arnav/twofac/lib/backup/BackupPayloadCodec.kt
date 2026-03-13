package tech.arnav.twofac.lib.backup

import kotlinx.serialization.json.Json
import tech.arnav.twofac.lib.PublicApi

@PublicApi
object BackupPayloadCodec {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: BackupPayload): ByteArray {
        return json.encodeToString(BackupPayload.serializer(), payload).encodeToByteArray()
    }

    fun decode(bytes: ByteArray): BackupPayload {
        val decoded = json.decodeFromString(BackupPayload.serializer(), bytes.decodeToString())
        require(decoded.schemaVersion == 1 || decoded.schemaVersion == 2) {
            "Unsupported backup schema version: ${decoded.schemaVersion}"
        }
        validate(decoded)
        return decoded
    }

    private fun validate(payload: BackupPayload) {
        when (payload.schemaVersion) {
            1 -> require(!payload.encrypted && payload.encryptedAccounts.isEmpty()) {
                "Schema version 1 backups do not support encrypted accounts"
            }

            2 -> if (payload.encrypted) {
                require(payload.accounts.isEmpty()) {
                    "Encrypted backups must not contain plaintext accounts"
                }
                require(payload.encryptedAccounts.isNotEmpty()) {
                    "Encrypted backups must contain encrypted accounts"
                }
            } else {
                require(payload.encryptedAccounts.isEmpty()) {
                    "Plaintext backups must not contain encrypted accounts"
                }
            }
        }
    }
}
