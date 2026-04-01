package tech.arnav.twofac.wear

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.lib.watchsync.WatchSyncContract

/**
 * Receives watch-initiated sync requests and drives the phone-side workflow:
 * 1) bring the main phone app activity to foreground
 * 2) trigger a manual watch sync publish
 */
class PhoneSyncRequestListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        if (messageEvent.path != WatchSyncContract.WATCH_REQUEST_SYNC_PATH) return

        Log.d(TAG, "Watch requested sync from node=${messageEvent.sourceNodeId}")
        openMainApp()

        serviceScope.launch {
            val syncCoordinator = runCatching { GlobalContext.get().get<CompanionSyncCoordinator>() }
                .getOrNull()
            if (syncCoordinator == null) {
                Log.w(TAG, "Watch sync request ignored: CompanionSyncCoordinator unavailable.")
                return@launch
            }

            val synced = runCatching { syncCoordinator.syncNow(manual = true) }
                .onFailure { Log.w(TAG, "Watch sync request failed while running sync workflow.", it) }
                .getOrDefault(false)

            if (synced) {
                Log.d(TAG, "Watch sync request completed successfully.")
            } else {
                Log.w(TAG, "Watch sync request completed without publishing snapshot.")
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun openMainApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Log.w(TAG, "Unable to open main app activity: launch intent not found for package=$packageName")
            return
        }
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP,
        )
        runCatching { startActivity(launchIntent) }
            .onFailure { Log.w(TAG, "Failed to launch phone app for watch sync request.", it) }
    }
}

private const val TAG = "PhoneWatchSyncReq"
