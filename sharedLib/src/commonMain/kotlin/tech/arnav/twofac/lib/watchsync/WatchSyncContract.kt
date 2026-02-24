package tech.arnav.twofac.lib.watchsync

object WatchSyncContract {
    const val SCHEMA_VERSION = 1
    const val SNAPSHOT_DATA_PATH = "/twofac/sync/snapshot"
    const val REQUEST_SYNC_NOW_MESSAGE_PATH = "/twofac/sync/request_now"
    const val PHONE_CAPABILITY = "twofac_mobile"
    const val WATCH_CAPABILITY = "twofac_watch"
}
