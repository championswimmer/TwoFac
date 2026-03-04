@file:OptIn(ExperimentalWasmJsInterop::class)
package tech.arnav.twofac.session

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf
import tech.arnav.twofac.session.interop.BrowserWebAuthnClient
import tech.arnav.twofac.session.interop.BrowserWebCryptoClient
import tech.arnav.twofac.session.interop.LocalStorageClient
import tech.arnav.twofac.session.interop.WebAuthnClient
import tech.arnav.twofac.session.interop.WebAuthnOperationStatus
import tech.arnav.twofac.session.interop.WebCryptoClient
import tech.arnav.twofac.session.interop.WebCryptoEncryptResult
import tech.arnav.twofac.session.interop.WebStorageClient

private const val REMEMBER_PASSKEY_KEY = "twofac_remember_passkey"
private const val SECURE_UNLOCK_ENABLED_KEY = "twofac_secure_unlock_enabled"
private const val ENROLLED_CREDENTIAL_ID_KEY = "twofac_webauthn_credential_id"
private const val ENCRYPTED_PASSKEY_BLOB_KEY = "twofac_webauthn_encrypted_passkey_blob"
private const val ENCRYPTED_PAYLOAD_VERSION = 1

internal interface EncryptedPasskeyStore {
    suspend fun read(): String?
    suspend fun write(value: String): Boolean
    fun clear()
}

internal class KStoreEncryptedPasskeyStore(
    private val storageClient: WebStorageClient,
    private val storageKey: String,
) : EncryptedPasskeyStore {
    private val store: KStore<String> = storeOf(
        key = storageKey,
        default = "",
    )

    override suspend fun read(): String? {
        return runCatching { store.get() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    override suspend fun write(value: String): Boolean {
        return runCatching {
            store.set(value)
            true
        }.getOrElse { false }
    }

    override fun clear() {
        if (!storageClient.isAvailable()) return
        storageClient.removeItem(storageKey)
    }
}

private data class EncryptedPasskeyBlob(
    val version: Int,
    val credentialId: String,
    val saltBase64Url: String,
    val nonceBase64Url: String,
    val ciphertextBase64Url: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

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
 * This phase stores only encrypted passkey payloads + metadata in persistent web storage
 * and keeps decrypted passkeys in memory for the current session lifetime.
 */
internal class BrowserSessionManager(
    private val storageClient: WebStorageClient = LocalStorageClient(),
    private val webAuthnClient: WebAuthnClient = BrowserWebAuthnClient(),
    private val webCryptoClient: WebCryptoClient = BrowserWebCryptoClient(),
    private val encryptedPasskeyStore: EncryptedPasskeyStore = KStoreEncryptedPasskeyStore(
        storageClient = storageClient,
        storageKey = ENCRYPTED_PASSKEY_BLOB_KEY,
    ),
) : WebAuthnSessionManager {

    private var sessionPasskey: String? = null
    private var enrolledCredentialId: String? = safeStorageGet(ENROLLED_CREDENTIAL_ID_KEY)
    private var encryptedPasskeyBlob: EncryptedPasskeyBlob? = null

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
        encryptedPasskeyBlob = null
        safeStorageRemove(ENROLLED_CREDENTIAL_ID_KEY)
        encryptedPasskeyStore.clear()
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

        if (outcome != SecureUnlockOutcome.SUCCESS) {
            return outcome
        }

        val credentialId = enrollResult.credentialId
        if (credentialId.isNullOrBlank()) {
            lastEnrollOutcome = SecureUnlockOutcome.FAILED
            return lastEnrollOutcome
        }

        val authResult = webAuthnClient.authenticate(credentialId)
        val authOutcome = authResult.status.toSecureUnlockOutcome()
        if (authOutcome != SecureUnlockOutcome.SUCCESS) {
            lastEnrollOutcome = authOutcome
            return lastEnrollOutcome
        }

        val prfFirstOutput = authResult.prfFirstOutputBase64Url
        if (prfFirstOutput.isNullOrBlank()) {
            lastEnrollOutcome = SecureUnlockOutcome.UNAVAILABLE
            return lastEnrollOutcome
        }

        val encrypted = webCryptoClient.encrypt(
            plaintext = passkey,
            prfFirstOutputBase64Url = prfFirstOutput,
            context = cryptoContextForCredential(credentialId),
        )
        if (encrypted == null) {
            lastEnrollOutcome = SecureUnlockOutcome.FAILED
            return lastEnrollOutcome
        }

        val nowEpochMillis = nowEpochMillis()
        val createdAtEpochMillis = readEncryptedPasskeyBlob()
            ?.takeIf { it.credentialId == credentialId }
            ?.createdAtEpochMillis
            ?: nowEpochMillis
        val blob = EncryptedPasskeyBlob(
            version = ENCRYPTED_PAYLOAD_VERSION,
            credentialId = credentialId,
            saltBase64Url = encrypted.saltBase64Url,
            nonceBase64Url = encrypted.nonceBase64Url,
            ciphertextBase64Url = encrypted.ciphertextBase64Url,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
        )
        if (!persistEncryptedPasskeyBlob(blob)) {
            lastEnrollOutcome = SecureUnlockOutcome.FAILED
            return lastEnrollOutcome
        }

        sessionPasskey = passkey
        enrolledCredentialId = credentialId
        safeStorageSet(ENROLLED_CREDENTIAL_ID_KEY, credentialId)
        setSecureUnlockEnabled(true)
        lastEnrollOutcome = SecureUnlockOutcome.SUCCESS
        return lastEnrollOutcome
    }

    suspend fun unlockWithOutcome(): SecureUnlockAttempt {
        if (!isRememberPasskeyEnabled()) {
            lastUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
            return SecureUnlockAttempt(
                SecureUnlockOutcome.UNAVAILABLE,
                detail = "Secure unlock disabled"
            )
        }

        val encryptedBlob = readEncryptedPasskeyBlob()
        if (encryptedBlob == null) {
            lastUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
            return SecureUnlockAttempt(
                SecureUnlockOutcome.UNAVAILABLE,
                detail = "No encrypted passkey"
            )
        }

        val credentialId =
            enrolledCredentialId ?: safeStorageGet(ENROLLED_CREDENTIAL_ID_KEY)?.also {
                enrolledCredentialId = it
            } ?: encryptedBlob.credentialId.also {
                enrolledCredentialId = it
                safeStorageSet(ENROLLED_CREDENTIAL_ID_KEY, it)
            }
        if (credentialId.isNullOrBlank()) {
            lastUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
            return SecureUnlockAttempt(
                SecureUnlockOutcome.UNAVAILABLE,
                detail = "No enrolled credential"
            )
        }

        if (encryptedBlob.credentialId != credentialId) {
            lastUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
            return SecureUnlockAttempt(
                SecureUnlockOutcome.UNAVAILABLE,
                detail = "Credential mismatch"
            )
        }

        if (encryptedBlob.version != ENCRYPTED_PAYLOAD_VERSION) {
            lastUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
            return SecureUnlockAttempt(
                SecureUnlockOutcome.UNAVAILABLE,
                detail = "Unsupported encrypted payload version"
            )
        }

        val authResult = webAuthnClient.authenticate(credentialId)
        val outcome = authResult.status.toSecureUnlockOutcome()
        lastUnlockOutcome = outcome

        if (outcome != SecureUnlockOutcome.SUCCESS) {
            return SecureUnlockAttempt(outcome = outcome, detail = authResult.message)
        }

        val prfFirstOutput = authResult.prfFirstOutputBase64Url
        if (prfFirstOutput.isNullOrBlank()) {
            lastUnlockOutcome = SecureUnlockOutcome.UNAVAILABLE
            return SecureUnlockAttempt(
                SecureUnlockOutcome.UNAVAILABLE,
                detail = "PRF output unavailable"
            )
        }

        val decryptedPasskey = webCryptoClient.decrypt(
            encryptedResult = WebCryptoEncryptResult(
                saltBase64Url = encryptedBlob.saltBase64Url,
                nonceBase64Url = encryptedBlob.nonceBase64Url,
                ciphertextBase64Url = encryptedBlob.ciphertextBase64Url,
            ),
            prfFirstOutputBase64Url = prfFirstOutput,
            context = cryptoContextForCredential(credentialId),
        )
        if (decryptedPasskey.isNullOrBlank()) {
            lastUnlockOutcome = SecureUnlockOutcome.FAILED
            return SecureUnlockAttempt(
                SecureUnlockOutcome.FAILED,
                detail = "Failed to decrypt stored passkey",
            )
        }

        sessionPasskey = decryptedPasskey
        return SecureUnlockAttempt(
            SecureUnlockOutcome.SUCCESS,
            passkey = decryptedPasskey,
            detail = authResult.message,
        )
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

    private suspend fun persistEncryptedPasskeyBlob(blob: EncryptedPasskeyBlob): Boolean {
        val persisted = encryptedPasskeyStore.write(blob.encode())
        if (persisted) {
            encryptedPasskeyBlob = blob
        }
        return persisted
    }

    private suspend fun readEncryptedPasskeyBlob(): EncryptedPasskeyBlob? {
        encryptedPasskeyBlob?.let { return it }
        val encoded = encryptedPasskeyStore.read() ?: return null
        val decoded = decodeEncryptedPasskeyBlob(encoded) ?: run {
            encryptedPasskeyStore.clear()
            return null
        }
        encryptedPasskeyBlob = decoded
        return decoded
    }

    private fun cryptoContextForCredential(credentialId: String): String {
        return "twofac-passkey-v$ENCRYPTED_PAYLOAD_VERSION:$credentialId"
    }
}

private fun EncryptedPasskeyBlob.encode(): String {
    return listOf(
        version.toString(),
        credentialId,
        saltBase64Url,
        nonceBase64Url,
        ciphertextBase64Url,
        createdAtEpochMillis.toString(),
        updatedAtEpochMillis.toString(),
    ).joinToString("|")
}

private fun decodeEncryptedPasskeyBlob(encoded: String): EncryptedPasskeyBlob? {
    val parts = encoded.split('|')
    if (parts.size != 7) return null

    val version = parts[0].toIntOrNull() ?: return null
    val createdAtEpochMillis = parts[5].toLongOrNull() ?: return null
    val updatedAtEpochMillis = parts[6].toLongOrNull() ?: return null

    if (parts[1].isBlank() || parts[2].isBlank() || parts[3].isBlank() || parts[4].isBlank()) {
        return null
    }

    return EncryptedPasskeyBlob(
        version = version,
        credentialId = parts[1],
        saltBase64Url = parts[2],
        nonceBase64Url = parts[3],
        ciphertextBase64Url = parts[4],
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
}

@JsModule("./time.mjs")
private external object TimeInterop {
    fun nowEpochMillisJs(): Double
}

private fun nowEpochMillis(): Long = TimeInterop.nowEpochMillisJs().toLong()
