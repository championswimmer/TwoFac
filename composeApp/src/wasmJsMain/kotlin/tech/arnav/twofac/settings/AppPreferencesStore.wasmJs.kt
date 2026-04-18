package tech.arnav.twofac.settings

import io.github.xxfast.kstore.storage.storeOf

actual fun createPlatformAppPreferencesRepository(): AppPreferencesRepository {
    return StoreBackedAppPreferencesRepository(
        store = storeOf(
            key = APP_PREFERENCES_STORAGE_KEY,
            default = AppPreferences(),
        )
    )
}
