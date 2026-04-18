package tech.arnav.twofac.settings

import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import tech.arnav.twofac.internal.getDocumentDirectory

actual fun createPlatformAppPreferencesRepository(): AppPreferencesRepository {
    val dir = getDocumentDirectory()
    SystemFileSystem.createDirectories(Path(dir))
    val filePath = Path(dir, APP_PREFERENCES_STORAGE_FILE)
    ensureAppPreferencesFileExists(filePath)
    return StoreBackedAppPreferencesRepository(
        store = storeOf(
            file = filePath,
            default = AppPreferences(),
        )
    )
}
