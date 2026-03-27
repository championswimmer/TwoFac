package tech.arnav.twofac.session

import ca.gosyer.appdirs.AppDirs
import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable

@Serializable
data class BiometricPreferences(
    val biometricEnabled: Boolean = false,
    val rememberPasskeyEnabled: Boolean = false,
)

class DesktopBiometricSessionManager(
    private val backend: DesktopSecureUnlockBackend,
) : BiometricSessionManager {

    private val appDirs = AppDirs {
        appName = "TwoFac"
        appAuthor = "tech.arnav"
        macOS.useSpaceBetweenAuthorAndApp = false
    }

    private val store: KStore<BiometricPreferences>? by lazy {
        try {
            val dir = appDirs.getUserDataDir()
            SystemFileSystem.createDirectories(Path(dir))
            storeOf(
                file = Path(dir, "biometric_preferences.json"),
                default = BiometricPreferences(),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getPreferences(): BiometricPreferences {
        return try {
            runBlocking { store?.get() } ?: BiometricPreferences()
        } catch (e: Exception) {
            BiometricPreferences()
        }
    }

    private fun updatePreferences(update: (BiometricPreferences) -> BiometricPreferences) {
        try {
            val current = getPreferences()
            runBlocking { store?.set(update(current)) }
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun isAvailable(): Boolean = true

    override fun isBiometricAvailable(): Boolean {
        return backend.isAvailable() && backend.supportsStrongUserPresence()
    }

    override fun isBiometricEnabled(): Boolean {
        val prefs = getPreferences()
        return prefs.biometricEnabled && isBiometricAvailable()
    }

    override fun isSecureUnlockReady(): Boolean {
        return isBiometricEnabled() && backend.hasStoredPasskey()
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        updatePreferences { it.copy(biometricEnabled = enabled) }
        if (!enabled) {
            clearPasskey()
        }
    }

    override fun isRememberPasskeyEnabled(): Boolean {
        val prefs = getPreferences()
        val biometricEnabled = prefs.biometricEnabled && isBiometricAvailable()
        return biometricEnabled || prefs.rememberPasskeyEnabled
    }

    override fun setRememberPasskey(enabled: Boolean) {
        updatePreferences { it.copy(rememberPasskeyEnabled = enabled) }
        if (!enabled) {
            clearPasskey()
        }
    }

    override suspend fun getSavedPasskey(): String? {
        if (!isRememberPasskeyEnabled()) return null
        return try {
            backend.getSavedPasskey()
        } catch (e: Exception) {
            null
        }
    }

    override fun savePasskey(passkey: String) {
        if (!isRememberPasskeyEnabled()) return
        try {
            backend.savePasskey(passkey)
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun clearPasskey() {
        try {
            backend.clearPasskey()
        } catch (e: Exception) {
            // ignore
        }
    }

    override suspend fun enrollPasskey(passkey: String): Boolean {
        if (!isBiometricAvailable()) return false
        return try {
            backend.enrollPasskey(passkey)
        } catch (e: Exception) {
            false
        }
    }
}
