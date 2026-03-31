package tech.arnav.twofac.lib.watchsync

object WatchSyncContract {
    const val SCHEMA_VERSION = 1
    const val SNAPSHOT_DATA_PATH = "/twofac/sync/snapshot"
    /** Sent watch → phone: watch is requesting a fresh sync snapshot. */
    const val WATCH_REQUEST_SYNC_PATH = "/twofac/sync/watch/request_sync"
    /** Sent phone → watch: phone is pinging the watch to confirm presence / trigger ACK. */
    const val PHONE_PING_WATCH_PATH = "/twofac/sync/phone/ping"
    const val SYNC_ACK_MESSAGE_PATH = "/twofac/sync/ack"
    const val PHONE_CAPABILITY = "twofac_mobile"
    const val WATCH_CAPABILITY = "twofac_watch"
    const val SNAPSHOT_PAYLOAD_KEY = "payload"
    const val SNAPSHOT_GENERATED_AT_KEY = "generatedAtEpochSec"
    const val SNAPSHOT_PUBLISHED_AT_KEY = "publishedAtMs"
}
