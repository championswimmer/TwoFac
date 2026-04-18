package tech.arnav.twofac.settings

actual fun createPlatformAppPreferencesRepository(): AppPreferencesRepository {
    return DesktopAppPreferencesRepository(
        settingsManager = DesktopSettingsManager(),
    )
}

private class DesktopAppPreferencesRepository(
    private val settingsManager: DesktopSettingsManager,
) : AppPreferencesRepository {
    override val preferencesFlow = settingsManager.appPreferencesFlow

    override suspend fun load(): AppPreferences {
        return settingsManager.loadAppPreferences()
    }

    override suspend fun setShowUpcomingCode(enabled: Boolean) {
        settingsManager.setShowUpcomingCode(enabled)
    }
}
