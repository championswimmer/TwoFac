package tech.arnav.twofac.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import tech.arnav.twofac.lib.watchsync.WatchSyncContract

object PhoneCapabilityRegistrar {
    fun register(context: Context) {
        Wearable.getCapabilityClient(context.applicationContext)
            .addLocalCapability(WatchSyncContract.PHONE_CAPABILITY)
            .addOnSuccessListener {
                Log.d(TAG, "Phone capability '${WatchSyncContract.PHONE_CAPABILITY}' registered.")
            }
            .addOnFailureListener { e ->
                // DUPLICATE_CAPABILITY (4006) is expected when wear.xml already declares it — not an error.
                if (e is com.google.android.gms.common.api.ApiException && e.statusCode == 4006) {
                    Log.d(TAG, "Phone capability '${WatchSyncContract.PHONE_CAPABILITY}' already registered (from wear.xml).")
                } else {
                    Log.w(TAG, "Failed to register phone capability '${WatchSyncContract.PHONE_CAPABILITY}'.", e)
                }
            }
    }
}

private const val TAG = "PhoneCapabilityRegistrar"
