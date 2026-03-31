package tech.arnav.twofac.watch.datalayer

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.arnav.twofac.lib.watchsync.WatchSyncContract

class WatchCompanionRegistrar(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val capabilityClient = Wearable.getCapabilityClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)

    suspend fun requestInitialization(): WatchCompanionInitResult = withContext(Dispatchers.IO) {
        val connectedNodes =
            runCatching { Tasks.await(nodeClient.connectedNodes) }.getOrDefault(emptyList())
        val allCompanionNodes = runCatching {
            Tasks.await(
                capabilityClient.getCapability(
                    WatchSyncContract.PHONE_CAPABILITY,
                    CapabilityClient.FILTER_ALL,
                )
            ).nodes
        }.getOrDefault(emptySet())
        if (allCompanionNodes.isEmpty()) {
            if (connectedNodes.isEmpty()) {
                Log.w(
                    TAG,
                    "Initialization failed: no paired/reachable phone nodes found from watch."
                )
            } else {
                val nodeIds = connectedNodes.joinToString { "${it.displayName}(${it.id})" }
                Log.w(
                    TAG,
                    "Initialization failed: paired phone node(s) found but companion app capability '${WatchSyncContract.PHONE_CAPABILITY}' missing. Nodes=$nodeIds"
                )
            }
            return@withContext WatchCompanionInitResult.CompanionAppMissing
        }

        val reachableNodes = allCompanionNodes.filter { it.isNearby }
        if (reachableNodes.isEmpty()) {
            Log.w(
                TAG,
                "Initialization failed: companion app capability found but no nearby/reachable phone node is available."
            )
            return@withContext WatchCompanionInitResult.CompanionNotReachable
        }

        val hasSentRequest = reachableNodes.any { node ->
            runCatching {
                Tasks.await(
                    messageClient.sendMessage(
                        node.id,
                        WatchSyncContract.WATCH_REQUEST_SYNC_PATH,
                        ByteArray(0),
                    )
                )
                true
            }.getOrElse {
                Log.w(
                    TAG,
                    "Initialization failed: unable to send sync request to node ${node.displayName}(${node.id}).",
                    it
                )
                false
            }
        }
        if (hasSentRequest) {
            WatchCompanionInitResult.RequestSent
        } else {
            Log.w(
                TAG,
                "Initialization failed: found companion node(s) but could not deliver sync request message."
            )
            WatchCompanionInitResult.CompanionNotReachable
        }
    }
}

enum class WatchCompanionInitResult {
    RequestSent,
    CompanionAppMissing,
    CompanionNotReachable,
}

private const val TAG = "WatchCompanionRegistrar"
