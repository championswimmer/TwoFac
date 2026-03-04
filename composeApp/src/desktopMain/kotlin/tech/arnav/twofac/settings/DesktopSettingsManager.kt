package tech.arnav.twofac.settings

import ca.gosyer.appdirs.AppDirs
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

class DesktopSettingsManager {
    private val appDirs = AppDirs {
        appName = "TwoFac"
        appAuthor = "tech.arnav"
        macOS.useSpaceBetweenAuthorAndApp = false
    }

    private val store: KStore<Boolean>? by lazy {
        try {
            val dir = appDirs.getUserDataDir()
            SystemFileSystem.createDirectories(Path(dir))
            storeOf(
                file = Path(dir, "desktop_settings.json"),
                default = false
            )
        } catch (e: Exception) {
            println("Failed to initialize desktop settings store: ${e.message}")
            null
        }
    }

    suspend fun isTrayIconEnabled(): Boolean {
        return try {
            store?.get() ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun isTrayIconEnabledSync(): Boolean = try {
        kotlinx.coroutines.runBlocking {
            store?.get() ?: false
        }
    } catch (e: Exception) {
        false
    }

    val isTrayIconEnabledFlow: Flow<Boolean> = store?.updates?.map { it ?: false } 
        ?: kotlinx.coroutines.flow.flowOf(false)

    suspend fun setTrayIconEnabled(enabled: Boolean) {
        try {
            store?.set(enabled)
        } catch (e: Exception) {
            println("Failed to save tray icon setting: ${e.message}")
        }
    }
}
