package tech.arnav.twofac.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import tech.arnav.twofac.lib.watchsync.WatchSyncContract

object PhoneCapabilityRegistrar {
    fun register(context: Context) {
        Wearable.getCapabilityClient(context.applicationContext)
            .addLocalCapability(WatchSyncContract.PHONE_CAPABILITY)
            .addOnFailureListener {
                Log.w(
                    TAG,
                    "Failed to register phone capability '${WatchSyncContract.PHONE_CAPABILITY}'.",
                    it
                )
            }
    }
}

private const val TAG = "PhoneCapabilityRegistrar"
