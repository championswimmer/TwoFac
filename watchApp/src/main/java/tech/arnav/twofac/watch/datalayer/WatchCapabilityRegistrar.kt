package tech.arnav.twofac.watch.datalayer

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import tech.arnav.twofac.lib.watchsync.WatchSyncContract

object WatchCapabilityRegistrar {
    fun register(context: Context) {
        Wearable.getCapabilityClient(context.applicationContext)
            .addLocalCapability(WatchSyncContract.WATCH_CAPABILITY)
            .addOnFailureListener {
                Log.w(
                    TAG,
                    "Failed to register watch capability '${WatchSyncContract.WATCH_CAPABILITY}'.",
                    it
                )
            }
    }
}

private const val TAG = "WatchCapabilityRegistrar"
