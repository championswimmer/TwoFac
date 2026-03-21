package tech.arnav.twofac.watch.datalayer

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import tech.arnav.twofac.lib.watchsync.WatchSyncContract

object WatchCapabilityRegistrar {
    fun register(context: Context) {
        val client = Wearable.getCapabilityClient(context.applicationContext)
        client.addLocalCapability(WatchSyncContract.WATCH_CAPABILITY)
            .addOnSuccessListener {
                Log.d(TAG, "Watch capability '${WatchSyncContract.WATCH_CAPABILITY}' registered.")
            }
            .addOnFailureListener { e ->
                // DUPLICATE_CAPABILITY (4006) is expected when wear.xml already declares it — not an error.
                if (e is com.google.android.gms.common.api.ApiException && e.statusCode == 4006) {
                    Log.d(TAG, "Watch capability '${WatchSyncContract.WATCH_CAPABILITY}' already registered (from wear.xml).")
                } else {
                    Log.w(TAG, "Failed to register watch capability '${WatchSyncContract.WATCH_CAPABILITY}'.", e)
                }
            }
    }
}

private const val TAG = "WatchCapabilityRegistrar"
