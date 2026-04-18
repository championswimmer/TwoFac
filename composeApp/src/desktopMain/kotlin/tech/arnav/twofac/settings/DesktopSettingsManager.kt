package tech.arnav.twofac.settings

import ca.gosyer.appdirs.AppDirs
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Paths

@Serializable
internal data class DesktopSettings(
    val trayIconEnabled: Boolean = false,
    val showUpcomingCode: Boolean = true,
)

private object DesktopSettingsStore {
    private val appDirs = AppDirs {
        appName = "TwoFac"
        appAuthor = "tech.arnav"
        macOS.useSpaceBetweenAuthorAndApp = false
    }

    val store: KStore<DesktopSettings>? by lazy {
        try {
            val dir = appDirs.getUserDataDir()
            SystemFileSystem.createDirectories(Path(dir))
            val filePath = Path(dir, "desktop_settings.json")
            migrateLegacyBooleanSettingsIfNeeded(filePath)
            ensureDesktopSettingsFileExists(filePath)
            storeOf(
                file = filePath,
                default = DesktopSettings(),
            )
        } catch (e: Exception) {
            println("Failed to initialize desktop settings store: ${e.message}")
            null
        }
    }
}

private fun migrateLegacyBooleanSettingsIfNeeded(filePath: Path) {
    if (!SystemFileSystem.exists(filePath)) return

    val raw = try {
        Files.readString(Paths.get(filePath.toString())).trim()
    } catch (_: Exception) {
        return
    }

    if (raw != "true" && raw != "false") return

    val migratedJson = """{"trayIconEnabled":$raw,"showUpcomingCode":true}"""
    SystemFileSystem.sink(filePath).buffered().use { sink ->
        sink.write(migratedJson.encodeToByteArray())
        sink.flush()
    }
}

private fun ensureDesktopSettingsFileExists(filePath: Path) {
    if (SystemFileSystem.exists(filePath)) return
    val defaultJson = """{"trayIconEnabled":false,"showUpcomingCode":true}"""
    SystemFileSystem.sink(filePath).buffered().use { sink ->
        sink.write(defaultJson.encodeToByteArray())
        sink.flush()
    }
}

class DesktopSettingsManager {
    private val store = DesktopSettingsStore.store

    private fun currentSettings(): DesktopSettings {
        return try {
            runBlocking { store?.get() } ?: DesktopSettings()
        } catch (_: Exception) {
            DesktopSettings()
        }
    }

    private fun updateSettings(update: (DesktopSettings) -> DesktopSettings) {
        try {
            val current = currentSettings()
            runBlocking {
                store?.set(update(current))
            }
        } catch (e: Exception) {
            println("Failed to save desktop settings: ${e.message}")
        }
    }

    suspend fun isTrayIconEnabled(): Boolean {
        return currentSettings().trayIconEnabled
    }

    fun isTrayIconEnabledSync(): Boolean = currentSettings().trayIconEnabled

    val isTrayIconEnabledFlow: Flow<Boolean> = store?.updates?.map { (it ?: DesktopSettings()).trayIconEnabled }
        ?: flowOf(false)

    suspend fun setTrayIconEnabled(enabled: Boolean) {
        updateSettings { it.copy(trayIconEnabled = enabled) }
    }

    suspend fun loadAppPreferences(): AppPreferences {
        return AppPreferences(showUpcomingCode = currentSettings().showUpcomingCode)
    }

    val appPreferencesFlow: Flow<AppPreferences> = store?.updates?.map {
        AppPreferences(showUpcomingCode = (it ?: DesktopSettings()).showUpcomingCode)
    } ?: flowOf(AppPreferences())

    suspend fun setShowUpcomingCode(enabled: Boolean) {
        updateSettings { it.copy(showUpcomingCode = enabled) }
    }
}
