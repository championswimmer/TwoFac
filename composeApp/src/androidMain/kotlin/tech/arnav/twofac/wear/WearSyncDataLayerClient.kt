package tech.arnav.twofac.wear

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import tech.arnav.twofac.lib.watchsync.WatchSyncContract

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
}
