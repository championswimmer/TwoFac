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
        require(decoded.schemaVersion == 1) {
            "Unsupported backup schema version: ${decoded.schemaVersion}"
        }
        return decoded
    }
}
