package tech.arnav.twofac.wear

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.arnav.twofac.lib.TwoFacLib
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import java.util.concurrent.TimeUnit

class AndroidWatchSyncCoordinator(
    private val appContext: Context,
    private val twoFacLib: TwoFacLib,
    private val dataLayerClient: WearSyncDataLayerClient,
) : WatchSyncCoordinator {

    override suspend fun isCompanionActive(): Boolean {
        return runCatching { dataLayerClient.hasReachableWatchCompanion() }.getOrDefault(false)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun syncNow(manual: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!twoFacLib.isUnlocked() || !dataLayerClient.hasReachableWatchCompanion()) {
            return@withContext false
        }
        val accounts = twoFacLib.getAllAccounts()
        if (accounts.isEmpty()) {
            return@withContext false
        }
        val uris = twoFacLib.exportAccountURIs()
        val sourceAccounts = accounts.zip(uris).map { (account, uri) ->
            WatchSyncSourceAccount(
                accountId = account.accountID,
                accountLabel = account.accountLabel,
                otpAuthUri = uri,
            )
        }
        val snapshot = buildWatchSyncSnapshot(
            sourceAccounts = sourceAccounts,
            generatedAtEpochSec = Clock.System.now().epochSeconds,
        )
        dataLayerClient.publishSnapshot(snapshot, manual)
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
const val WATCH_SYNC_PERIODIC_INTERVAL_MINUTES = 15L
