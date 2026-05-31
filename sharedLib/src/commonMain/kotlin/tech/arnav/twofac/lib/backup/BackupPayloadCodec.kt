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
        require(decoded.schemaVersion in 1..3) {
            "Unsupported backup schema version: ${decoded.schemaVersion}"
        }
        validate(decoded)
        return decoded
    }

    private fun validate(payload: BackupPayload) {
        when (payload.schemaVersion) {
            1 -> require(!payload.encrypted && payload.encryptedAccounts.isEmpty() && payload.plaintextAccounts.isEmpty()) {
                "Schema version 1 backups do not support encrypted accounts or metadata entries"
            }

            2 -> if (payload.encrypted) {
                require(payload.accounts.isEmpty() && payload.plaintextAccounts.isEmpty()) {
                    "Encrypted backups must not contain plaintext accounts"
                }
                require(payload.encryptedAccounts.isNotEmpty()) {
                    "Encrypted backups must contain encrypted accounts"
                }
            } else {
                require(payload.encryptedAccounts.isEmpty() && payload.plaintextAccounts.isEmpty()) {
                    "Plaintext backups must not contain encrypted accounts or metadata entries"
                }
            }

            3 -> if (payload.encrypted) {
                require(payload.accounts.isEmpty() && payload.plaintextAccounts.isEmpty()) {
                    "Encrypted backups must not contain plaintext accounts"
                }
                require(payload.encryptedAccounts.isNotEmpty()) {
                    "Encrypted backups must contain encrypted accounts"
                }
            } else {
                require(payload.encryptedAccounts.isEmpty()) {
                    "Plaintext backups must not contain encrypted accounts"
                }
                require(payload.accounts.isEmpty()) {
                    "Schema version 3 plaintext backups must use plaintext account entries"
                }
            }
        }
    }
}
