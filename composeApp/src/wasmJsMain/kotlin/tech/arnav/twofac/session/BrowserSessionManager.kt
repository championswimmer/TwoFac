@file:OptIn(ExperimentalWasmJsInterop::class)
package tech.arnav.twofac.session

import tech.arnav.twofac.session.interop.BrowserWebAuthnClient
import tech.arnav.twofac.session.interop.LocalStorageClient
import tech.arnav.twofac.session.interop.WebAuthnClient
import tech.arnav.twofac.session.interop.WebAuthnOperationStatus
import tech.arnav.twofac.session.interop.WebStorageClient

enum class SecureUnlockOutcome {
    SUCCESS,
    CANCELLED,
    UNAVAILABLE,
    FAILED,
}

data class SecureUnlockAttempt(
    val outcome: SecureUnlockOutcome,
    val passkey: String? = null,
    val detail: String? = null,
)

/**
 * Browser implementation of [WebAuthnSessionManager] using WebAuthn as the unlock gate.
 *
 * This phase keeps decrypted passkey data in-memory only and stores only non-secret
 * preference/metadata in web storage.
 */
internal class BrowserSessionManager(
    private val storageClient: WebStorageClient = LocalStorageClient(),
    private val webAuthnClient: WebAuthnClient = BrowserWebAuthnClient(),
) : WebAuthnSessionManager {

    companion object {
        private const val REMEMBER_PASSKEY_KEY = "twofac_remember_passkey"
        private const val SECURE_UNLOCK_ENABLED_KEY = "twofac_secure_unlock_enabled"
        private const val ENROLLED_CREDENTIAL_ID_KEY = "twofac_webauthn_credential_id"
    }

    private var sessionPasskey: String? = null
    private var enrolledCredentialId: String? = safeStorageGet(ENROLLED_CREDENTIAL_ID_KEY)

    var lastEnrollOutcome: SecureUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
        private set

    var lastUnlockOutcome: SecureUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
        private set

    override fun isAvailable(): Boolean = isSecureUnlockAvailable()

    override fun isRememberPasskeyEnabled(): Boolean = isSecureUnlockEnabled()

    override fun setRememberPasskey(enabled: Boolean) = setSecureUnlockEnabled(enabled)

    override suspend fun getSavedPasskey(): String? {
        val unlockAttempt = unlockWithOutcome()
        return unlockAttempt.passkey
    }

    override fun savePasskey(passkey: String) {
        if (isRememberPasskeyEnabled()) {
            sessionPasskey = passkey
        }
    }

    override fun clearPasskey() {
        sessionPasskey = null
        enrolledCredentialId = null
        safeStorageRemove(ENROLLED_CREDENTIAL_ID_KEY)
    }

    override fun isSecureUnlockAvailable(): Boolean {
        return storageClient.isAvailable() && webAuthnClient.isSupported()
    }

    override fun isSecureUnlockEnabled(): Boolean {
        if (!isSecureUnlockAvailable()) return false
        return safeStorageGet(SECURE_UNLOCK_ENABLED_KEY) == "true"
    }

    override fun setSecureUnlockEnabled(enabled: Boolean) {
        if (!enabled || !isSecureUnlockAvailable()) {
            safeStorageRemove(REMEMBER_PASSKEY_KEY)
            safeStorageRemove(SECURE_UNLOCK_ENABLED_KEY)
            clearPasskey()
            return
        }

        safeStorageSet(REMEMBER_PASSKEY_KEY, "true")
        safeStorageSet(SECURE_UNLOCK_ENABLED_KEY, "true")
    }

    override suspend fun enrollPasskey(passkey: String): Boolean {
        return enrollPasskeyWithOutcome(passkey) == SecureUnlockOutcome.SUCCESS
    }

    suspend fun enrollPasskeyWithOutcome(passkey: String): SecureUnlockOutcome {
        if (passkey.isBlank()) {
            lastEnrollOutcome = SecureUnlockOutcome.FAILED
            return lastEnrollOutcome
        }
        if (!isSecureUnlockAvailable()) {
            lastEnrollOutcome = SecureUnlockOutcome.UNAVAILABLE
            return lastEnrollOutcome
        }

        val capabilities = webAuthnClient.queryCapabilities()
        if (!capabilities.publicKeyCredentialAvailable || !capabilities.userVerifyingAuthenticatorAvailable) {
            lastEnrollOutcome = SecureUnlockOutcome.UNAVAILABLE
            return lastEnrollOutcome
        }

        val enrollResult = webAuthnClient.createCredential()
        val outcome = enrollResult.status.toSecureUnlockOutcome()
        lastEnrollOutcome = outcome

        if (outcome == SecureUnlockOutcome.SUCCESS) {
            sessionPasskey = passkey
            enrolledCredentialId = enrollResult.credentialId
            if (!enrollResult.credentialId.isNullOrBlank()) {
                safeStorageSet(ENROLLED_CREDENTIAL_ID_KEY, enrollResult.credentialId)
            }
            setSecureUnlockEnabled(true)
        }

        return outcome
    }

    suspend fun unlockWithOutcome(): SecureUnlockAttempt {
        if (!isRememberPasskeyEnabled()) {
            lastUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
            return SecureUnlockAttempt(
                SecureUnlockOutcome.UNAVAILABLE,
                detail = "Secure unlock disabled"
            )
        }

        val passkey = sessionPasskey
        if (passkey == null) {
            lastUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
            return SecureUnlockAttempt(
                SecureUnlockOutcome.UNAVAILABLE,
                detail = "No in-memory passkey"
            )
        }

        val credentialId =
            enrolledCredentialId ?: safeStorageGet(ENROLLED_CREDENTIAL_ID_KEY)?.also {
                enrolledCredentialId = it
            }
        if (credentialId.isNullOrBlank()) {
            lastUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
            return SecureUnlockAttempt(
                SecureUnlockOutcome.UNAVAILABLE,
                detail = "No enrolled credential"
            )
        }

        val authResult = webAuthnClient.authenticate(credentialId)
        val outcome = authResult.status.toSecureUnlockOutcome()
        lastUnlockOutcome = outcome

        return if (outcome == SecureUnlockOutcome.SUCCESS) {
            SecureUnlockAttempt(
                SecureUnlockOutcome.SUCCESS,
                passkey = passkey,
                detail = authResult.message
            )
        } else {
            SecureUnlockAttempt(outcome = outcome, detail = authResult.message)
        }
    }

    private fun WebAuthnOperationStatus.toSecureUnlockOutcome(): SecureUnlockOutcome = when (this) {
        WebAuthnOperationStatus.SUCCESS -> SecureUnlockOutcome.SUCCESS
        WebAuthnOperationStatus.CANCELLED -> SecureUnlockOutcome.CANCELLED
        WebAuthnOperationStatus.UNAVAILABLE -> SecureUnlockOutcome.UNAVAILABLE
        WebAuthnOperationStatus.FAILED -> SecureUnlockOutcome.FAILED
    }

    private fun safeStorageGet(key: String): String? {
        if (!storageClient.isAvailable()) return null
        return storageClient.getItem(key)
    }

    private fun safeStorageSet(key: String, value: String) {
        if (!storageClient.isAvailable()) return
        storageClient.setItem(key, value)
    }

    private fun safeStorageRemove(key: String) {
        if (!storageClient.isAvailable()) return
        storageClient.removeItem(key)
    }
}
