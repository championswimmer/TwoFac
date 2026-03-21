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

    /**
     * Returns detailed companion status: reachable, installed-but-not-reachable, or not found.
     * Falls back to connectedNodes when capability lookup returns empty, since the capability
     * system can be slow to propagate after watch app installation.
     */
    suspend fun getWatchCompanionStatus(): WatchCompanionStatus = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext WatchCompanionStatus.NOT_FOUND
        val reachableNodes = runCatching { Tasks.await(getReachableWatchNodes()) }
            .getOrDefault(emptySet())
        if (reachableNodes.isNotEmpty()) return@withContext WatchCompanionStatus.REACHABLE
        val allCapableNodes = runCatching {
            Tasks.await(
                capabilityClient.getCapability(
                    WatchSyncContract.WATCH_CAPABILITY,
                    CapabilityClient.FILTER_ALL,
                )
            ).nodes
        }.getOrDefault(emptySet())
        if (allCapableNodes.isNotEmpty()) {
            Log.d(
                TAG,
                "Watch companion installed but not currently reachable. Nodes: ${
                    allCapableNodes.joinToString { it.displayName }
                }",
            )
            return@withContext WatchCompanionStatus.INSTALLED_NOT_REACHABLE
        }
        // Capability lookup found nothing — fall back to raw connected nodes.
        // The watch app may be installed but the capability hasn't propagated yet.
        val connectedNodes = runCatching {
            Tasks.await(nodeClient.connectedNodes)
        }.getOrDefault(emptyList())
        if (connectedNodes.isNotEmpty()) {
            Log.d(
                TAG,
                "No capability advertised, but connected watch node(s) found: ${
                    connectedNodes.joinToString { "${it.displayName}(${it.id})" }
                }. Treating as installed (capability may not have propagated yet).",
            )
            return@withContext WatchCompanionStatus.INSTALLED_NOT_REACHABLE
        }
        WatchCompanionStatus.NOT_FOUND
    }

    /**
     * Returns true if a watch companion exists (reachable or installed but not currently connected).
     * Falls back to FILTER_ALL when FILTER_REACHABLE returns empty.
     */
    suspend fun hasReachableWatchCompanion(): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext false
        getWatchCompanionStatus() != WatchCompanionStatus.NOT_FOUND
    }

    suspend fun forceDiscoverWatchCompanion(): Boolean = withContext(Dispatchers.IO) {
        if (!isAvailable) return@withContext false
        // Try capability-based discovery first.
        val capableNodes = runCatching {
            Tasks.await(
                capabilityClient.getCapability(
                    WatchSyncContract.WATCH_CAPABILITY,
                    CapabilityClient.FILTER_ALL,
                )
            ).nodes
        }.getOrDefault(emptySet())

        val targetNodes: Collection<Node> = capableNodes.ifEmpty {
            // Capability not advertised — fall back to connected nodes.
            // The watch app's WearableListenerService will still receive messages
            // even without an explicit capability, as long as the app is installed.
            val connected = runCatching {
                Tasks.await(nodeClient.connectedNodes)
            }.getOrDefault(emptyList())
            if (connected.isEmpty()) {
                logWatchAvailabilityIssue("discover")
                return@withContext false
            }
            Log.d(
                TAG,
                "No capability found, falling back to ${connected.size} connected node(s) for discovery.",
            )
            connected
        }

        targetNodes.forEach { node ->
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
        true
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

enum class WatchCompanionStatus {
    NOT_FOUND,
    INSTALLED_NOT_REACHABLE,
    REACHABLE,
}

private const val TAG = "WearSyncDataLayer"
