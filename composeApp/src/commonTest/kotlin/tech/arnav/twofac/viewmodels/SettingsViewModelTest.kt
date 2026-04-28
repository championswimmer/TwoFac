package tech.arnav.twofac.viewmodels

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.settings.AppPreferences
import tech.arnav.twofac.settings.AppPreferencesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {
    @Test
    fun `enabling secure unlock shows enrollment dialog`() = runTest {
        val viewModel = SettingsViewModel(
            sessionManager = SettingsFakeSecureSessionManager(),
            appPreferencesRepository = FakeAppPreferencesRepository(),
        )

        viewModel.onRememberPasskeyToggleChanged(true)

        assertTrue(viewModel.uiState.value.showEnrollmentDialog)
        assertFalse(viewModel.uiState.value.isSecureUnlockEnabled)
    }

    @Test
    fun `disabling secure unlock clears remembered state`() = runTest {
        val sessionManager = SettingsFakeSecureSessionManager(
            rememberPasskeyEnabled = true,
            secureUnlockEnabledState = true,
        )
        val viewModel = SettingsViewModel(
            sessionManager = sessionManager,
            appPreferencesRepository = FakeAppPreferencesRepository(),
        )

        viewModel.onRememberPasskeyToggleChanged(false)

        assertFalse(sessionManager.rememberPasskeyEnabled)
        assertFalse(sessionManager.secureUnlockEnabledState)
        assertFalse(viewModel.uiState.value.isSecureUnlockEnabled)
        assertFalse(viewModel.uiState.value.showEnrollmentDialog)
    }

    @Test
    fun `locked export queues pending backup action`() = runTest {
        val twoFacLib = TwoFacLib.initialise(storage = MemoryStorage())
        val viewModel = SettingsViewModel(
            twoFacLib = twoFacLib,
            appPreferencesRepository = FakeAppPreferencesRepository(),
        )

        viewModel.requestBackupExport("local")
        viewModel.onBackupExportModeSelected(encrypted = true)

        assertEquals(
            SettingsBackupAction.Export(providerId = "local", encrypted = true),
            viewModel.uiState.value.pendingAction,
        )
    }
}

private class FakeAppPreferencesRepository(
    initialPreferences: AppPreferences = AppPreferences(),
) : AppPreferencesRepository {
    private val state = MutableStateFlow(initialPreferences)

    override val preferencesFlow: Flow<AppPreferences> = state

    override suspend fun load(): AppPreferences = state.value

    override suspend fun setShowUpcomingCode(enabled: Boolean) {
        state.value = state.value.copy(showUpcomingCode = enabled)
    }
}

private open class SettingsBaseFakeSessionManager(
    var rememberPasskeyEnabled: Boolean = false,
) : SessionManager {
    override fun isAvailable(): Boolean = true
    override fun isRememberPasskeyEnabled(): Boolean = rememberPasskeyEnabled
    override fun setRememberPasskey(enabled: Boolean) {
        rememberPasskeyEnabled = enabled
    }

    override suspend fun getSavedPasskey(): String? = null
    override fun savePasskey(passkey: String) = Unit
    override fun clearPasskey() = Unit
}

private class SettingsFakeSecureSessionManager(
    rememberPasskeyEnabled: Boolean = false,
    var secureUnlockEnabledState: Boolean = false,
    private val secureUnlockAvailable: Boolean = true,
    private val secureUnlockReady: Boolean = false,
) : SettingsBaseFakeSessionManager(rememberPasskeyEnabled), SecureSessionManager {
    override fun isSecureUnlockAvailable(): Boolean = secureUnlockAvailable
    override fun isSecureUnlockEnabled(): Boolean = secureUnlockEnabledState
    override fun isSecureUnlockReady(): Boolean = secureUnlockReady
    override fun setSecureUnlockEnabled(enabled: Boolean) {
        secureUnlockEnabledState = enabled
    }

    override suspend fun enrollPasskey(passkey: String): Boolean = true
}
