package tech.arnav.twofac.session

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.session.interop.WebAuthnCapabilities
import tech.arnav.twofac.session.interop.WebAuthnClient
import tech.arnav.twofac.session.interop.WebAuthnOperationResult
import tech.arnav.twofac.session.interop.WebAuthnOperationStatus
import tech.arnav.twofac.session.interop.WebCryptoClient
import tech.arnav.twofac.session.interop.WebCryptoEncryptResult
import tech.arnav.twofac.session.interop.WebStorageClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrowserSessionManagerTest {
    @Test
    fun defaultManagerSmokeTestFunctionsRunInBrowser() = runTest {
        val manager = BrowserSessionManager()

        manager.setRememberPasskey(false)
        manager.clearPasskey()

        // Smoke check in real browser runtime: should execute without throwing.
        manager.isAvailable()
        manager.isSecureUnlockAvailable()
        assertNull(manager.getSavedPasskey())
    }

    @Test
    fun managerUnavailableWhenWebAuthnNotSupported() {
        val storage = FakeStorageClient()
        val client = FakeWebAuthnClient(
            supported = false,
            capabilities = supportedCapabilities(),
            createResult = WebAuthnOperationResult(
                status = WebAuthnOperationStatus.SUCCESS,
                credentialId = "cred-1"
            ),
            authenticateResult = successfulAuthResult(),
        )
        val manager = BrowserSessionManager(
            storageClient = storage,
            webAuthnClient = client,
            webCryptoClient = FakeWebCryptoClient(),
            encryptedPasskeyStore = FakeEncryptedPasskeyStore(),
        )

        assertFalse(manager.isAvailable())
        manager.setRememberPasskey(true)
        assertFalse(manager.isRememberPasskeyEnabled())
    }

    @Test
    fun enrollPersistsEncryptedBlobAndUnlockDecryptsPasskey() = runTest {
        val storage = FakeStorageClient()
        val encryptedStore = FakeEncryptedPasskeyStore()
        val cryptoClient = FakeWebCryptoClient(decryptResult = "pass-123")
        val client = FakeWebAuthnClient(
            supported = true,
            capabilities = supportedCapabilities(),
            createResult = WebAuthnOperationResult(
                status = WebAuthnOperationStatus.SUCCESS,
                credentialId = "cred-1"
            ),
            authenticateResult = successfulAuthResult(),
        )
        val manager = BrowserSessionManager(
            storageClient = storage,
            webAuthnClient = client,
            webCryptoClient = cryptoClient,
            encryptedPasskeyStore = encryptedStore,
        )

        manager.setRememberPasskey(true)
        assertFalse(manager.isPasskeyEnrolled())
        assertTrue(manager.enrollPasskey("pass-123"))
        assertEquals(SecureUnlockOutcome.SUCCESS, manager.lastEnrollOutcome)
        assertTrue(manager.isPasskeyEnrolled())
        val storedPayload = encryptedStore.value
        assertNotNull(storedPayload)
        assertFalse(storedPayload.contains("pass-123"))

        val unlockedPasskey = manager.getSavedPasskey()
        assertEquals("pass-123", unlockedPasskey)
        assertEquals("cred-1", client.lastAuthenticateCredentialId)
        assertFalse(storage.snapshot.values.contains("pass-123"))
        assertTrue((cryptoClient.lastEncryptContext ?: "").contains("cred-1"))
        assertTrue((cryptoClient.lastDecryptContext ?: "").contains("cred-1"))
    }

    @Test
    fun enrollSkipsAuthenticateWhenCreateReturnsPrf() = runTest {
        val encryptedStore = FakeEncryptedPasskeyStore()
        val cryptoClient = FakeWebCryptoClient()
        val client = FakeWebAuthnClient(
            supported = true,
            capabilities = supportedCapabilities(),
            // Create result already has PRF output
            createResult = WebAuthnOperationResult(
                status = WebAuthnOperationStatus.SUCCESS,
                credentialId = "cred-1",
                prfFirstOutputBase64Url = "cHJmLWtleS1ieXRlcw",
            ),
            authenticateResult = successfulAuthResult(),
        )
        val manager = BrowserSessionManager(
            storageClient = FakeStorageClient(),
            webAuthnClient = client,
            webCryptoClient = cryptoClient,
            encryptedPasskeyStore = encryptedStore,
        )

        manager.setRememberPasskey(true)
        assertTrue(manager.enrollPasskey("pass-123"))
        assertEquals(SecureUnlockOutcome.SUCCESS, manager.lastEnrollOutcome)
        // authenticate should NOT have been called since PRF came from createCredential
        assertNull(client.lastAuthenticateCredentialId)
    }

    @Test
    fun enrollCallsAuthenticateWhenCreateLacksPrf() = runTest {
        val encryptedStore = FakeEncryptedPasskeyStore()
        val client = FakeWebAuthnClient(
            supported = true,
            capabilities = supportedCapabilities(),
            // Create result has no PRF output
            createResult = WebAuthnOperationResult(
                status = WebAuthnOperationStatus.SUCCESS,
                credentialId = "cred-1",
                prfFirstOutputBase64Url = null,
            ),
            authenticateResult = successfulAuthResult(),
        )
        val manager = BrowserSessionManager(
            storageClient = FakeStorageClient(),
            webAuthnClient = client,
            webCryptoClient = FakeWebCryptoClient(),
            encryptedPasskeyStore = encryptedStore,
        )

        manager.setRememberPasskey(true)
        assertTrue(manager.enrollPasskey("pass-123"))
        // authenticate should have been called as a fallback to get PRF
        assertEquals("cred-1", client.lastAuthenticateCredentialId)
    }

    @Test
    fun isPasskeyEnrolledReturnsFalseWhenNotEnrolled() {
        val manager = BrowserSessionManager(
            storageClient = FakeStorageClient(),
            webAuthnClient = FakeWebAuthnClient(
                supported = true,
                capabilities = supportedCapabilities(),
                createResult = WebAuthnOperationResult(
                    status = WebAuthnOperationStatus.SUCCESS,
                    credentialId = "cred-1",
                ),
                authenticateResult = successfulAuthResult(),
            ),
            webCryptoClient = FakeWebCryptoClient(),
            encryptedPasskeyStore = FakeEncryptedPasskeyStore(),
        )

        manager.setRememberPasskey(true)
        assertFalse(manager.isPasskeyEnrolled())
    }

    @Test
    fun isPasskeyEnrolledReturnsFalseAfterClear() = runTest {
        val encryptedStore = FakeEncryptedPasskeyStore()
        val manager = BrowserSessionManager(
            storageClient = FakeStorageClient(),
            webAuthnClient = FakeWebAuthnClient(
                supported = true,
                capabilities = supportedCapabilities(),
                createResult = WebAuthnOperationResult(
                    status = WebAuthnOperationStatus.SUCCESS,
                    credentialId = "cred-1",
                ),
                authenticateResult = successfulAuthResult(),
            ),
            webCryptoClient = FakeWebCryptoClient(),
            encryptedPasskeyStore = encryptedStore,
        )

        manager.setRememberPasskey(true)
        assertTrue(manager.enrollPasskey("pass-123"))
        assertTrue(manager.isPasskeyEnrolled())

        manager.clearPasskey()
        assertFalse(manager.isPasskeyEnrolled())
    }

    @Test
    fun unlockCancelledMapsToExplicitOutcome() = runTest {
        val storage = FakeStorageClient()
        val encryptedStore = FakeEncryptedPasskeyStore()
        val client = FakeWebAuthnClient(
            supported = true,
            capabilities = supportedCapabilities(),
            createResult = WebAuthnOperationResult(
                status = WebAuthnOperationStatus.SUCCESS,
                credentialId = "cred-1"
            ),
            authenticateResult = successfulAuthResult(),
        )
        val manager = BrowserSessionManager(
            storageClient = storage,
            webAuthnClient = client,
            webCryptoClient = FakeWebCryptoClient(),
            encryptedPasskeyStore = encryptedStore,
        )

        manager.setRememberPasskey(true)
        assertTrue(manager.enrollPasskey("pass-123"))

        client.authenticateResult =
            WebAuthnOperationResult(status = WebAuthnOperationStatus.CANCELLED)
        val attempt = manager.unlockWithOutcome()

        assertEquals(SecureUnlockOutcome.CANCELLED, attempt.outcome)
        assertEquals(SecureUnlockOutcome.CANCELLED, manager.lastUnlockOutcome)
        assertNull(attempt.passkey)
    }

    @Test
    fun clearPasskeyRemovesEncryptedBlob() = runTest {
        val encryptedStore = FakeEncryptedPasskeyStore()
        val manager = BrowserSessionManager(
            storageClient = FakeStorageClient(),
            webAuthnClient = FakeWebAuthnClient(
                supported = true,
                capabilities = supportedCapabilities(),
                createResult = WebAuthnOperationResult(
                    status = WebAuthnOperationStatus.SUCCESS,
                    credentialId = "cred-1",
                ),
                authenticateResult = successfulAuthResult(),
            ),
            webCryptoClient = FakeWebCryptoClient(),
            encryptedPasskeyStore = encryptedStore,
        )

        manager.setRememberPasskey(true)
        assertTrue(manager.enrollPasskey("pass-123"))
        assertNotNull(encryptedStore.value)

        manager.clearPasskey()
        assertNull(encryptedStore.value)
        assertNull(manager.getSavedPasskey())
    }

    @Test
    fun enrollFailsWhenPrfOutputMissing() = runTest {
        val encryptedStore = FakeEncryptedPasskeyStore()
        val manager = BrowserSessionManager(
            storageClient = FakeStorageClient(),
            webAuthnClient = FakeWebAuthnClient(
                supported = true,
                capabilities = supportedCapabilities(),
                createResult = WebAuthnOperationResult(
                    status = WebAuthnOperationStatus.SUCCESS,
                    credentialId = "cred-1",
                ),
                authenticateResult = WebAuthnOperationResult(status = WebAuthnOperationStatus.SUCCESS),
            ),
            webCryptoClient = FakeWebCryptoClient(),
            encryptedPasskeyStore = encryptedStore,
        )

        manager.setRememberPasskey(true)
        assertFalse(manager.enrollPasskey("pass-123"))
        assertEquals(SecureUnlockOutcome.UNAVAILABLE, manager.lastEnrollOutcome)
        assertNull(encryptedStore.value)
    }

    private fun supportedCapabilities(): WebAuthnCapabilities {
        return WebAuthnCapabilities(
            publicKeyCredentialAvailable = true,
            userVerifyingAuthenticatorAvailable = true,
            clientCapabilitiesAvailable = true,
            prfSupported = true,
        )
    }

    private fun successfulAuthResult(): WebAuthnOperationResult {
        return WebAuthnOperationResult(
            status = WebAuthnOperationStatus.SUCCESS,
            prfFirstOutputBase64Url = "cHJmLWtleS1ieXRlcw",
        )
    }
}

private class FakeStorageClient : WebStorageClient {
    private val values = mutableMapOf<String, String>()
    val snapshot: Map<String, String> get() = values.toMap()

    override fun isAvailable(): Boolean = true

    override fun getItem(key: String): String? = values[key]

    override fun setItem(key: String, value: String) {
        values[key] = value
    }

    override fun removeItem(key: String) {
        values.remove(key)
    }
}

private class FakeEncryptedPasskeyStore : EncryptedPasskeyStore {
    var value: String? = null

    override suspend fun read(): String? = value

    override suspend fun write(value: String): Boolean {
        this.value = value
        return true
    }

    override fun clear() {
        value = null
    }
}

private class FakeWebCryptoClient(
    var encryptResult: WebCryptoEncryptResult? = WebCryptoEncryptResult(
        saltBase64Url = "c2FsdA",
        nonceBase64Url = "bm9uY2U",
        ciphertextBase64Url = "Y2lwaGVydGV4dA",
    ),
    var decryptResult: String? = "pass-123",
) : WebCryptoClient {
    var lastEncryptContext: String? = null
    var lastDecryptContext: String? = null

    override suspend fun encrypt(
        plaintext: String,
        prfFirstOutputBase64Url: String,
        context: String,
    ): WebCryptoEncryptResult? {
        lastEncryptContext = context
        return encryptResult
    }

    override suspend fun decrypt(
        encryptedResult: WebCryptoEncryptResult,
        prfFirstOutputBase64Url: String,
        context: String,
    ): String? {
        lastDecryptContext = context
        return decryptResult
    }
}

private class FakeWebAuthnClient(
    private val supported: Boolean,
    private val capabilities: WebAuthnCapabilities,
    private val createResult: WebAuthnOperationResult,
    var authenticateResult: WebAuthnOperationResult,
) : WebAuthnClient {
    var lastAuthenticateCredentialId: String? = null

    override fun isSupported(): Boolean = supported

    override suspend fun queryCapabilities(): WebAuthnCapabilities = capabilities

    override suspend fun createCredential(): WebAuthnOperationResult = createResult

    override suspend fun authenticate(credentialId: String?): WebAuthnOperationResult {
        lastAuthenticateCredentialId = credentialId
        return authenticateResult
    }
}
