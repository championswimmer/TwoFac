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

    private val store: KStore<Boolean> by lazy {
        val dir = appDirs.getUserDataDir()
        SystemFileSystem.createDirectories(Path(dir))
        storeOf(
            file = Path(dir, "desktop_settings.json"),
            default = false
        )
    }

    suspend fun isTrayIconEnabled(): Boolean {
        return store.get() ?: false
    }

    val isTrayIconEnabledFlow: Flow<Boolean> = store.updates.map { it ?: false }

    suspend fun setTrayIconEnabled(enabled: Boolean) {
        store.set(enabled)
    }
}
