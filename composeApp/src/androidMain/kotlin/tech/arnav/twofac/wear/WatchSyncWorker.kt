package tech.arnav.twofac.wear

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.context.GlobalContext

class WatchSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val coordinator =
            runCatching { GlobalContext.get().get<WatchSyncCoordinator>() }.getOrNull()
            ?: return Result.success()
        return runCatching {
            coordinator.syncNow(manual = false)
            Result.success()
        }.getOrDefault(Result.retry())
    }
}
