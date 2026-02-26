package tech.arnav.twofac.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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

class AndroidBiometricSessionManager(
    private val context: Context,
    private val activityProvider: () -> FragmentActivity,
) : SessionManager {

    companion object {
        private const val KEYSTORE_ALIAS = "twofac_biometric_key"
        private const val PREFS_NAME = "twofac_session_prefs"
        private const val KEY_REMEMBER_ENABLED = "remember_passkey_enabled"
        private const val KEY_ENCRYPTED_PASSKEY = "encrypted_passkey"
        private const val KEY_PASSKEY_IV = "passkey_iv"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AUTH_VALIDITY_SECONDS = 15 * 60
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun isAvailable(): Boolean = true

    override fun isRememberPasskeyEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ENABLED, false) && isBiometricAvailable()
    }

    override fun setRememberPasskey(enabled: Boolean) {
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
        if (!isRememberPasskeyEnabled()) return

        try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val encrypted = cipher.doFinal(passkey.toByteArray())
            val iv = cipher.iv

            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSKEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_PASSKEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (_: Exception) {
            clearPasskey()
        }
    }

    override fun clearPasskey() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_PASSKEY)
            .remove(KEY_PASSKEY_IV)
            .apply()
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private suspend fun authenticateAndDecrypt(
        encryptedBase64: String,
        ivBase64: String,
    ): String? = suspendCoroutine { continuation ->
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
                    } catch (_: Exception) {
                        clearPasskey()
                        continuation.resume(null)
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    continuation.resume(null)
                }
            }

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock TwoFac")
                .setSubtitle("Authenticate to access your 2FA codes")
                .setNegativeButtonText("Use passkey instead")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()

            val biometricPrompt = BiometricPrompt(
                activityProvider(),
                ContextCompat.getMainExecutor(context),
                callback,
            )
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        } catch (_: Exception) {
            clearPasskey()
            continuation.resume(null)
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
        } catch (_: Exception) {
        }
    }
}
