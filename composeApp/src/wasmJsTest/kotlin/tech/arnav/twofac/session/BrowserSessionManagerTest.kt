package tech.arnav.twofac.session

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.session.interop.WebAuthnCapabilities
import tech.arnav.twofac.session.interop.WebAuthnClient
import tech.arnav.twofac.session.interop.WebAuthnOperationResult
import tech.arnav.twofac.session.interop.WebAuthnOperationStatus
import tech.arnav.twofac.session.interop.WebStorageClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            authenticateResult = WebAuthnOperationResult(status = WebAuthnOperationStatus.SUCCESS),
        )
        val manager = BrowserSessionManager(storage, client)

        assertFalse(manager.isAvailable())
        manager.setRememberPasskey(true)
        assertFalse(manager.isRememberPasskeyEnabled())
    }

    @Test
    fun enrollAndUnlockWithWebAuthnSuccess() = runTest {
        val storage = FakeStorageClient()
        val client = FakeWebAuthnClient(
            supported = true,
            capabilities = supportedCapabilities(),
            createResult = WebAuthnOperationResult(
                status = WebAuthnOperationStatus.SUCCESS,
                credentialId = "cred-1"
            ),
            authenticateResult = WebAuthnOperationResult(status = WebAuthnOperationStatus.SUCCESS),
        )
        val manager = BrowserSessionManager(storage, client)

        manager.setRememberPasskey(true)
        assertTrue(manager.enrollPasskey("pass-123"))
        assertEquals(SecureUnlockOutcome.SUCCESS, manager.lastEnrollOutcome)

        val unlockedPasskey = manager.getSavedPasskey()
        assertEquals("pass-123", unlockedPasskey)
        assertEquals("cred-1", client.lastAuthenticateCredentialId)
        assertFalse(storage.snapshot.values.contains("pass-123"))
    }

    @Test
    fun unlockCancelledMapsToExplicitOutcome() = runTest {
        val storage = FakeStorageClient()
        val client = FakeWebAuthnClient(
            supported = true,
            capabilities = supportedCapabilities(),
            createResult = WebAuthnOperationResult(
                status = WebAuthnOperationStatus.SUCCESS,
                credentialId = "cred-1"
            ),
            authenticateResult = WebAuthnOperationResult(status = WebAuthnOperationStatus.SUCCESS),
        )
        val manager = BrowserSessionManager(storage, client)

        manager.setRememberPasskey(true)
        assertTrue(manager.enrollPasskey("pass-123"))

        client.authenticateResult =
            WebAuthnOperationResult(status = WebAuthnOperationStatus.CANCELLED)
        val attempt = manager.unlockWithOutcome()

        assertEquals(SecureUnlockOutcome.CANCELLED, attempt.outcome)
        assertEquals(SecureUnlockOutcome.CANCELLED, manager.lastUnlockOutcome)
        assertNull(attempt.passkey)
    }

    private fun supportedCapabilities(): WebAuthnCapabilities {
        return WebAuthnCapabilities(
            publicKeyCredentialAvailable = true,
            userVerifyingAuthenticatorAvailable = true,
            clientCapabilitiesAvailable = true,
            prfSupported = true,
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
