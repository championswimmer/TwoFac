package tech.arnav.twofac.watch.datalayer

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import tech.arnav.twofac.lib.watchsync.WatchSyncContract
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshotCodec
import tech.arnav.twofac.watch.storage.WatchSyncSnapshotRepository
import tech.arnav.twofac.watch.storage.WatchSyncSyncError

class WatchSyncListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { WatchSyncSnapshotRepository.get(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch { repository.initialize() }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.use { buffer ->
            buffer.forEach { event ->
                if (event.type != DataEvent.TYPE_CHANGED) return@forEach
                val dataItem = event.dataItem
                if (dataItem.uri.path != WatchSyncContract.SNAPSHOT_DATA_PATH) return@forEach

                val payload = DataMapItem.fromDataItem(dataItem).dataMap
                    .getByteArray(WatchSyncContract.SNAPSHOT_PAYLOAD_KEY)
                if (payload == null) {
                    serviceScope.launch { repository.persistError(WatchSyncSyncError.MISSING_PAYLOAD) }
                    return@forEach
                }

                serviceScope.launch {
                    try {
                        val snapshot = WatchSyncSnapshotCodec.decode(payload)
                        repository.persistSnapshot(snapshot)
                    } catch (_: SerializationException) {
                        repository.persistError(WatchSyncSyncError.MALFORMED_PAYLOAD)
                    } catch (_: IllegalArgumentException) {
                        repository.persistError(WatchSyncSyncError.UNSUPPORTED_SCHEMA)
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WatchSyncContract.REQUEST_SYNC_NOW_MESSAGE_PATH) {
            serviceScope.launch {
                repository.initialize()
                runCatching {
                    Wearable.getMessageClient(this@WatchSyncListenerService)
                        .sendMessage(
                            messageEvent.sourceNodeId,
                            WatchSyncContract.SYNC_ACK_MESSAGE_PATH,
                            ByteArray(0),
                        )
                }
            }
            return
        }
        super.onMessageReceived(messageEvent)
    }
}
