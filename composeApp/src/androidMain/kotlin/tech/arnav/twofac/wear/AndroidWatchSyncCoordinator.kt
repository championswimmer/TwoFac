package tech.arnav.twofac.wear

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.companion.CompanionSyncSourceAccount
import tech.arnav.twofac.companion.buildCompanionSyncSnapshot
import tech.arnav.twofac.lib.TwoFacLib
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AndroidWatchSyncCoordinator(
    private val appContext: Context,
    private val twoFacLib: TwoFacLib,
    private val dataLayerClient: WearSyncDataLayerClient,
) : CompanionSyncCoordinator {

    override val companionDisplayName: String = "Wear OS Watch"

    override suspend fun isCompanionActive(): Boolean {
        return runCatching { dataLayerClient.hasReachableWatchCompanion() }.getOrDefault(false)
    }

    override suspend fun forceDiscoverCompanion(): Boolean {
        return runCatching { dataLayerClient.forceDiscoverWatchCompanion() }.getOrDefault(false)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun syncNow(manual: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!twoFacLib.isUnlocked()) {
            Log.w(TAG, "Sync aborted: library is locked. Unlock TwoFac before syncing to watch.")
            return@withContext false
        }
        if (!dataLayerClient.hasReachableWatchCompanion()) {
            Log.w(TAG, "Sync aborted: no reachable watch companion.")
            return@withContext false
        }
        val accounts = twoFacLib.getAllAccounts()
        if (accounts.isEmpty()) {
            Log.w(TAG, "Sync aborted: no accounts available to sync.")
            return@withContext false
        }
        val uris = twoFacLib.exportAccountURIs()
        val sourceAccounts = accounts.zip(uris).map { (account, uri) ->
            CompanionSyncSourceAccount(
                accountId = account.accountID,
                accountLabel = account.accountLabel,
                otpAuthUri = uri,
            )
        }
        val snapshot = buildCompanionSyncSnapshot(
            sourceAccounts = sourceAccounts,
            generatedAtEpochSec = Clock.System.now().epochSeconds,
        )
        runCatching { dataLayerClient.publishSnapshot(snapshot, manual) }
            .onFailure { Log.w(TAG, "Sync failed while publishing snapshot.", it) }
            .getOrDefault(false)
    }

    override suspend fun onAccountsUnlocked() {
        updatePeriodicSyncSchedule()
        syncNow(manual = false)
    }

    override suspend fun onAccountsChanged() {
        updatePeriodicSyncSchedule()
        syncNow(manual = false)
    }

    private fun updatePeriodicSyncSchedule() {
        val workManager = WorkManager.getInstance(appContext)
        val hasAccounts = runCatching { twoFacLib.getAllAccounts().isNotEmpty() }.getOrDefault(false)
        if (!hasAccounts) {
            workManager.cancelUniqueWork(WATCH_SYNC_PERIODIC_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<WatchSyncWorker>(
            WATCH_SYNC_PERIODIC_INTERVAL_MINUTES,
            TimeUnit.MINUTES,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WATCH_SYNC_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

const val WATCH_SYNC_PERIODIC_WORK_NAME = "twofac-watch-sync-periodic"
const val WATCH_SYNC_PERIODIC_INTERVAL_MINUTES = 30L
private const val TAG = "AndroidWatchSync"
