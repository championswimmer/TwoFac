package tech.arnav.twofac.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
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

    /** Whether the Wearable API is available on this device. */
    val isAvailable: Boolean by lazy {
        val result = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(appContext, /* minApkVersion = */ 0)
        if (result != ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Play Services not available (code=$result), Wearable API disabled.")
            return@lazy false
        }
        // Try to touch the Wearable API — if it throws, the device lacks Wear support
        runCatching { Wearable.getNodeClient(appContext) }
            .onFailure { Log.d(TAG, "Wearable API not available on this device.", it) }
            .isSuccess
    }

    val dataClient: DataClient by lazy { Wearable.getDataClient(appContext) }
    val messageClient: MessageClient by lazy { Wearable.getMessageClient(appContext) }
    val capabilityClient: CapabilityClient by lazy { Wearable.getCapabilityClient(appContext) }
    val nodeClient by lazy { Wearable.getNodeClient(appContext) }

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
        if (!isAvailable) return@withContext false
        val hasReachable = Tasks.await(getReachableWatchNodes()).isNotEmpty()
        if (!hasReachable) {
            logWatchAvailabilityIssue("sync")
        }
        hasReachable
    }

    suspend fun forceDiscoverWatchCompanion(): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext false
        val nodes = Tasks.await(
            capabilityClient.getCapability(
                WatchSyncContract.WATCH_CAPABILITY,
                CapabilityClient.FILTER_ALL
            )
        ).nodes
        if (nodes.isEmpty()) {
            logWatchAvailabilityIssue("discover")
            return@withContext false
        }
        nodes.forEach { node ->
            runCatching {
                Tasks.await(
                    messageClient.sendMessage(
                        node.id,
                        WatchSyncContract.REQUEST_SYNC_NOW_MESSAGE_PATH,
                        ByteArray(0),
                    )
                )
            }
        }
        val reachableAfterDiscover =
            runCatching { Tasks.await(getReachableWatchNodes()).isNotEmpty() }
                .getOrDefault(false)
        reachableAfterDiscover || nodes.isNotEmpty()
    }

    suspend fun publishSnapshot(snapshot: WatchSyncSnapshot, manual: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext false
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

    private fun logWatchAvailabilityIssue(operation: String) {
        val pairedNodes =
            runCatching { Tasks.await(nodeClient.connectedNodes) }.getOrDefault(emptyList())
        val capableNodes = runCatching {
            Tasks.await(
                capabilityClient.getCapability(
                    WatchSyncContract.WATCH_CAPABILITY,
                    CapabilityClient.FILTER_ALL
                )
            ).nodes
        }.getOrDefault(emptySet())
        if (pairedNodes.isEmpty()) {
            Log.w(TAG, "Unable to $operation: no paired/reachable watch nodes found from phone.")
            return
        }
        if (capableNodes.isEmpty()) {
            val nodeNames = pairedNodes.joinToString { "${it.displayName}(${it.id})" }
            Log.w(
                TAG,
                "Unable to $operation: paired watch node(s) found but TwoFac watch capability '${WatchSyncContract.WATCH_CAPABILITY}' missing. Nodes=$nodeNames"
            )
            return
        }
        Log.w(
            TAG,
            "Unable to $operation: TwoFac watch capability found, but no reachable watch node is currently available."
        )
    }
}

private const val TAG = "WearSyncDataLayer"
