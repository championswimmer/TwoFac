package tech.arnav.twofac.lib.watchsync

import kotlinx.serialization.json.Json

object WatchSyncSnapshotCodec {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(snapshot: WatchSyncSnapshot): ByteArray {
        return json.encodeToString(WatchSyncSnapshot.serializer(), snapshot).encodeToByteArray()
    }

    fun decode(bytes: ByteArray): WatchSyncSnapshot {
        val decoded = json.decodeFromString(WatchSyncSnapshot.serializer(), bytes.decodeToString())
        require(decoded.version == WatchSyncContract.SCHEMA_VERSION) {
            "Unsupported watch sync schema version: ${decoded.version}"
        }
        return decoded
    }
}
