package tech.arnav.twofac.watch.storage

import android.content.Context
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class WatchSyncSnapshotRepository private constructor(
    context: Context,
) {
    private val store: KStore<WatchSyncCacheState> = storeOf(
        file = Path(context.filesDir.absolutePath, WATCH_SYNC_CACHE_FILE),
        default = WatchSyncCacheState(),
    )

    private val _state = MutableStateFlow(WatchSyncCacheState())
    val state: StateFlow<WatchSyncCacheState> = _state

    suspend fun initialize() {
        _state.value = store.get() ?: WatchSyncCacheState()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun persistSnapshot(snapshot: WatchSyncSnapshot) {
        val updated = WatchSyncCacheState(
            snapshot = snapshot,
            lastSyncedAtEpochSec = Clock.System.now().epochSeconds,
            lastError = null,
        )
        store.set(updated)
        _state.value = updated
    }

    suspend fun persistError(error: WatchSyncError) {
        val current = _state.value
        val updated = current.copy(lastError = error.name)
        store.set(updated)
        _state.value = updated
    }

    companion object {
        @Volatile
        private var instance: WatchSyncSnapshotRepository? = null

        fun get(context: Context): WatchSyncSnapshotRepository {
            return instance ?: synchronized(this) {
                instance ?: WatchSyncSnapshotRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}

@Serializable
data class WatchSyncCacheState(
    val snapshot: WatchSyncSnapshot? = null,
    val lastSyncedAtEpochSec: Long? = null,
    val lastError: String? = null,
)

enum class WatchSyncError {
    MISSING_PAYLOAD,
    MALFORMED_PAYLOAD,
    UNSUPPORTED_SCHEMA,
}

private const val WATCH_SYNC_CACHE_FILE = "watch_sync_snapshot.json"
