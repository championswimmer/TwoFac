package tech.arnav.twofac.wear

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.arnav.twofac.lib.watchsync.WatchSyncContract
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshotCodec

class WearSyncDataLayerClient(context: Context) {
    private val appContext = context.applicationContext

    val dataClient: DataClient = Wearable.getDataClient(appContext)
    val messageClient: MessageClient = Wearable.getMessageClient(appContext)
    val capabilityClient: CapabilityClient = Wearable.getCapabilityClient(appContext)

    fun getReachableWatchNodes(): Task<Set<Node>> {
        return capabilityClient
            .getCapability(WatchSyncContract.WATCH_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .continueWith { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: IllegalStateException("Failed to query watch capability")
                }
                task.result?.nodes ?: emptySet()
            }
    }

    suspend fun hasReachableWatchCompanion(): Boolean = withContext(Dispatchers.IO) {
        Tasks.await(getReachableWatchNodes()).isNotEmpty()
    }

    suspend fun publishSnapshot(snapshot: WatchSyncSnapshot, manual: Boolean): Boolean = withContext(Dispatchers.IO) {
        val dataRequest = PutDataMapRequest.create(WatchSyncContract.SNAPSHOT_DATA_PATH).run {
            dataMap.putByteArray(WatchSyncContract.SNAPSHOT_PAYLOAD_KEY, WatchSyncSnapshotCodec.encode(snapshot))
            dataMap.putLong(WatchSyncContract.SNAPSHOT_GENERATED_AT_KEY, snapshot.generatedAtEpochSec)
            dataMap.putLong(WatchSyncContract.SNAPSHOT_PUBLISHED_AT_KEY, System.currentTimeMillis())
            asPutDataRequest()
        }
        if (manual) {
            dataRequest.setUrgent()
        }
        Tasks.await(dataClient.putDataItem(dataRequest))
        true
    }
}
