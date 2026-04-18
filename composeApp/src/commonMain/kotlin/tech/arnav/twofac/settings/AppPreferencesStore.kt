package tech.arnav.twofac.settings

import io.github.xxfast.kstore.KStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable

const val APP_PREFERENCES_STORAGE_KEY = "twofac_app_preferences"
const val APP_PREFERENCES_STORAGE_FILE = "app_preferences.json"

@Serializable
data class AppPreferences(
    val showUpcomingCode: Boolean = true,
)

interface AppPreferencesRepository {
    val preferencesFlow: Flow<AppPreferences>

    suspend fun load(): AppPreferences

    suspend fun setShowUpcomingCode(enabled: Boolean)
}

expect fun createPlatformAppPreferencesRepository(): AppPreferencesRepository

internal fun ensureAppPreferencesFileExists(filePath: Path) {
    if (SystemFileSystem.exists(filePath)) return
    SystemFileSystem.sink(filePath).buffered().use { sink ->
        sink.write("""{"showUpcomingCode":true}""".encodeToByteArray())
        sink.flush()
    }
}

class StoreBackedAppPreferencesRepository(
    private val store: KStore<AppPreferences>,
) : AppPreferencesRepository {
    override val preferencesFlow: Flow<AppPreferences> = store.updates.map { it ?: AppPreferences() }

    override suspend fun load(): AppPreferences {
        return store.get() ?: AppPreferences()
    }

    override suspend fun setShowUpcomingCode(enabled: Boolean) {
        val current = load()
        if (current.showUpcomingCode == enabled) return
        store.set(current.copy(showUpcomingCode = enabled))
    }
}
