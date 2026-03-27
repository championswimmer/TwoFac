package tech.arnav.twofac.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.jetbrains.compose.resources.getString
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.biometric_enrollment_title
import twofac.composeapp.generated.resources.biometric_enrollment_subtitle
import twofac.composeapp.generated.resources.biometric_unlock_title
import twofac.composeapp.generated.resources.biometric_unlock_subtitle
import twofac.composeapp.generated.resources.biometric_use_passkey_instead
import twofac.composeapp.generated.resources.action_cancel

class AndroidBiometricSessionManager(
    private val context: Context,
    private val activityProvider: () -> FragmentActivity,
) : BiometricSessionManager {

    companion object {
        private const val TAG = "AndroidBiometricSession"
        private const val KEYSTORE_ALIAS = "twofac_biometric_key"
        private const val PREFS_NAME = "twofac_session_prefs"
        private const val KEY_REMEMBER_ENABLED = "remember_passkey_enabled"
        private const val KEY_ENCRYPTED_PASSKEY = "encrypted_passkey"
        private const val KEY_PASSKEY_IV = "passkey_iv"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        // After a successful biometric prompt, the key remains usable for this duration.
        private const val AUTH_VALIDITY_SECONDS = 15 * 60
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun isAvailable(): Boolean = true

    override fun isRememberPasskeyEnabled(): Boolean {
        // On Android, "remember passkey" is synonymous with biometric unlock
        return isBiometricEnabled()
    }

    override fun setRememberPasskey(enabled: Boolean) {
        // Delegate to biometric toggle — the two are synonymous on secure platforms
        setBiometricEnabled(enabled)
    }

    override fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ENABLED, false) && isBiometricAvailable()
    }

    override fun isSecureUnlockReady(): Boolean {
        val hasEncryptedPasskey = !prefs.getString(KEY_ENCRYPTED_PASSKEY, null).isNullOrBlank()
        val hasIv = !prefs.getString(KEY_PASSKEY_IV, null).isNullOrBlank()
        return isBiometricEnabled() && hasEncryptedPasskey && hasIv
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        setRememberEnabled(enabled && isBiometricAvailable())
    }

    private fun setRememberEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMEMBER_ENABLED, enabled).apply()
        if (!enabled) {
            clearPasskey()
            deleteKey()
        }
    }

    override suspend fun getSavedPasskey(): String? {
        if (!isRememberPasskeyEnabled()) return null

        val encryptedData = prefs.getString(KEY_ENCRYPTED_PASSKEY, null) ?: return null
        val iv = prefs.getString(KEY_PASSKEY_IV, null) ?: return null
        return authenticateAndDecrypt(encryptedData, iv)
    }

    override fun savePasskey(passkey: String) {
        // No-op: on Android, passkeys are only stored via enrollPasskey() with biometric
        // protection. Plain-text persistence is not supported.
    }

    override fun clearPasskey() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_PASSKEY)
            .remove(KEY_PASSKEY_IV)
            .apply()
    }

    override fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun enrollPasskey(passkey: String): Boolean {
        if (!isBiometricAvailable()) return false

        return try {
            // Start fresh: delete old key and data
            deleteKey()
            clearPasskey()

            // Create a new key
            val key = getOrCreateKey()

            // Prompt biometric to authenticate the time-based key
            val authenticated = promptBiometric(
                title = getString(Res.string.biometric_enrollment_title),
                subtitle = getString(Res.string.biometric_enrollment_subtitle),
                negativeButtonText = getString(Res.string.action_cancel),
            )
            if (!authenticated) return false

            // Key is now authenticated for AUTH_VALIDITY_SECONDS — encrypt and save
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encrypted = cipher.doFinal(passkey.toByteArray())
            val iv = cipher.iv

            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSKEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_PASSKEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()

            true
        } catch (e: Exception) {
            Log.w(TAG, "Biometric enrollment failed", e)
            clearPasskey()
            false
        }
    }

    // ── Private helpers ──

    private suspend fun promptBiometric(
        title: String,
        subtitle: String,
        negativeButtonText: String,
    ): Boolean = suspendCoroutine { continuation ->
        try {
            val activity = activityProvider()
            val executor = ContextCompat.getMainExecutor(context)

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(true)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    continuation.resume(false)
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to show biometric prompt", e)
            continuation.resume(false)
        }
    }

    private suspend fun authenticateAndDecrypt(
        encryptedBase64: String,
        ivBase64: String,
    ): String? {
        val unlockTitle = getString(Res.string.biometric_unlock_title)
        val unlockSubtitle = getString(Res.string.biometric_unlock_subtitle)
        val usePasskeyText = getString(Res.string.biometric_use_passkey_instead)

        return suspendCoroutine { continuation ->
            try {
                val key = getOrCreateKey()
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        try {
                            val resultCipher = result.cryptoObject?.cipher ?: cipher
                            val encrypted = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                            val decrypted = resultCipher.doFinal(encrypted)
                            continuation.resume(String(decrypted))
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to decrypt passkey after biometric auth", e)
                            clearPasskey()
                            continuation.resume(null)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        continuation.resume(null)
                    }
                }

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(unlockTitle)
                    .setSubtitle(unlockSubtitle)
                    .setNegativeButtonText(usePasskeyText)
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build()

                val biometricPrompt = BiometricPrompt(
                    activityProvider(),
                    ContextCompat.getMainExecutor(context),
                    callback,
                )
                biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
            } catch (e: Exception) {
                Log.w(TAG, "Biometric prompt/decrypt setup failed", e)
                clearPasskey()
                continuation.resume(null)
            }
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        keyStore.getKey(KEYSTORE_ALIAS, null)?.let {
            return it as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(
                    AUTH_VALIDITY_SECONDS,
                    KeyProperties.AUTH_BIOMETRIC_STRONG,
                )
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun deleteKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(KEYSTORE_ALIAS)
        } catch (e: Exception) {
            Log.d(TAG, "Failed to delete biometric key", e)
        }
    }
}
