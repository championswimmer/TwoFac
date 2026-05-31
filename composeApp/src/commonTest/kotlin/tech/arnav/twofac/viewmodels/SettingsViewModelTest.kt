package tech.arnav.twofac.viewmodels

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SecureUnlockRetentionPolicy
import tech.arnav.twofac.session.SecureUnlockRetentionScope
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.session.SessionRetentionCapableSecureSessionManager
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

    @Test
    fun `session retention state is exposed for supported managers`() = runTest {
        val viewModel = SettingsViewModel(
            sessionManager = SettingsFakeSecureSessionManager(
                secureUnlockEnabledState = true,
                supportsSessionRetentionState = true,
                retentionPolicyState = SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION,
                retentionScopeState = SecureUnlockRetentionScope.BROWSER_SESSION,
            ),
            appPreferencesRepository = FakeAppPreferencesRepository(),
        )

        assertTrue(viewModel.uiState.value.isSessionRetentionSupported)
        assertEquals(
            SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION,
            viewModel.uiState.value.sessionRetentionPolicy,
        )
        assertEquals(
            SecureUnlockRetentionScope.BROWSER_SESSION,
            viewModel.uiState.value.sessionRetentionScope,
        )
    }

    @Test
    fun `changing session retention updates manager and ui state`() = runTest {
        val sessionManager = SettingsFakeSecureSessionManager(
            secureUnlockEnabledState = true,
            supportsSessionRetentionState = true,
        )
        val viewModel = SettingsViewModel(
            sessionManager = sessionManager,
            appPreferencesRepository = FakeAppPreferencesRepository(),
        )

        viewModel.onSessionRetentionChanged(true)

        assertEquals(
            SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION,
            sessionManager.retentionPolicyState,
        )
        assertEquals(
            SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION,
            viewModel.uiState.value.sessionRetentionPolicy,
        )

        viewModel.onSessionRetentionChanged(false)

        assertEquals(
            SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
            sessionManager.retentionPolicyState,
        )
        assertEquals(
            SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
            viewModel.uiState.value.sessionRetentionPolicy,
        )
    }

    @Test
    fun `browser session retention requires explicit warning confirmation`() = runTest {
        val sessionManager = SettingsFakeSecureSessionManager(
            secureUnlockEnabledState = true,
            supportsSessionRetentionState = true,
            retentionScopeState = SecureUnlockRetentionScope.BROWSER_SESSION,
        )
        val viewModel = SettingsViewModel(
            sessionManager = sessionManager,
            appPreferencesRepository = FakeAppPreferencesRepository(),
        )

        viewModel.onSessionRetentionChanged(true)

        assertTrue(viewModel.uiState.value.showSessionRetentionRiskDialog)
        assertEquals(
            SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
            sessionManager.retentionPolicyState,
        )

        viewModel.confirmSessionRetentionRisk()

        assertFalse(viewModel.uiState.value.showSessionRetentionRiskDialog)
        assertEquals(
            SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION,
            sessionManager.retentionPolicyState,
        )
        assertEquals(
            SecureUnlockRetentionPolicy.RETAIN_FOR_CURRENT_SESSION,
            viewModel.uiState.value.sessionRetentionPolicy,
        )
    }

    @Test
    fun `dismissing browser retention warning keeps prompt every time policy`() = runTest {
        val sessionManager = SettingsFakeSecureSessionManager(
            secureUnlockEnabledState = true,
            supportsSessionRetentionState = true,
            retentionScopeState = SecureUnlockRetentionScope.BROWSER_SESSION,
        )
        val viewModel = SettingsViewModel(
            sessionManager = sessionManager,
            appPreferencesRepository = FakeAppPreferencesRepository(),
        )

        viewModel.onSessionRetentionChanged(true)
        viewModel.dismissSessionRetentionRiskDialog()

        assertFalse(viewModel.uiState.value.showSessionRetentionRiskDialog)
        assertEquals(
            SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
            sessionManager.retentionPolicyState,
        )
        assertEquals(
            SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
            viewModel.uiState.value.sessionRetentionPolicy,
        )
    }

    @Test
    fun `unsupported retention managers keep setting hidden and unchanged`() = runTest {
        val sessionManager = SettingsFakeSecureSessionManager(
            secureUnlockEnabledState = true,
            supportsSessionRetentionState = false,
        )
        val viewModel = SettingsViewModel(
            sessionManager = sessionManager,
            appPreferencesRepository = FakeAppPreferencesRepository(),
        )

        assertFalse(viewModel.uiState.value.isSessionRetentionSupported)
        assertEquals(
            SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
            viewModel.uiState.value.sessionRetentionPolicy,
        )

        viewModel.onSessionRetentionChanged(true)

        assertEquals(
            SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
            sessionManager.retentionPolicyState,
        )
        assertEquals(
            SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
            viewModel.uiState.value.sessionRetentionPolicy,
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
    private val supportsSessionRetentionState: Boolean = false,
    var retentionPolicyState: SecureUnlockRetentionPolicy = SecureUnlockRetentionPolicy.PROMPT_EVERY_TIME,
    private val retentionScopeState: SecureUnlockRetentionScope = SecureUnlockRetentionScope.APP_SESSION,
) : SettingsBaseFakeSessionManager(rememberPasskeyEnabled),
    SessionRetentionCapableSecureSessionManager {
    override fun isSecureUnlockAvailable(): Boolean = secureUnlockAvailable
    override fun isSecureUnlockEnabled(): Boolean = secureUnlockEnabledState
    override fun isSecureUnlockReady(): Boolean = secureUnlockReady
    override fun setSecureUnlockEnabled(enabled: Boolean) {
        secureUnlockEnabledState = enabled
    }

    override suspend fun enrollPasskey(passkey: String): Boolean = true

    override fun supportsSessionRetention(): Boolean = supportsSessionRetentionState

    override fun getSecureUnlockRetentionPolicy(): SecureUnlockRetentionPolicy = retentionPolicyState

    override fun setSecureUnlockRetentionPolicy(policy: SecureUnlockRetentionPolicy) {
        if (!supportsSessionRetentionState) return
        retentionPolicyState = policy
    }

    override fun getSecureUnlockRetentionScope(): SecureUnlockRetentionScope = retentionScopeState
}
