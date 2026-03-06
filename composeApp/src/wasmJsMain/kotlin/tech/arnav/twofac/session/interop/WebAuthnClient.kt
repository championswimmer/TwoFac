@file:OptIn(ExperimentalWasmJsInterop::class)

package tech.arnav.twofac.session.interop

import kotlinx.coroutines.await
import kotlin.js.Promise

internal data class WebAuthnCapabilities(
    val publicKeyCredentialAvailable: Boolean,
    val userVerifyingAuthenticatorAvailable: Boolean,
    val clientCapabilitiesAvailable: Boolean,
    val prfSupported: Boolean,
)

internal enum class WebAuthnOperationStatus {
    SUCCESS,
    CANCELLED,
    UNAVAILABLE,
    FAILED,
}

internal data class WebAuthnOperationResult(
    val status: WebAuthnOperationStatus,
    val credentialId: String? = null,
    val extensionResults: String? = null,
    val prfFirstOutputBase64Url: String? = null,
    val message: String? = null,
)

internal interface WebAuthnClient {
    fun isSupported(): Boolean
    suspend fun queryCapabilities(): WebAuthnCapabilities
    suspend fun createCredential(): WebAuthnOperationResult
    suspend fun authenticate(credentialId: String?): WebAuthnOperationResult
}

internal class BrowserWebAuthnClient : WebAuthnClient {
    override fun isSupported(): Boolean = WebAuthnInterop.isWebAuthnSupported()

    override suspend fun queryCapabilities(): WebAuthnCapabilities =
        runCatching {
            val result = WebAuthnInterop.queryWebAuthnCapabilities().await<WebAuthnCapabilitiesInteropResult>()
            WebAuthnCapabilities(
                publicKeyCredentialAvailable = result.publicKeyCredentialAvailable,
                userVerifyingAuthenticatorAvailable = result.userVerifyingAuthenticatorAvailable,
                clientCapabilitiesAvailable = result.clientCapabilitiesAvailable,
                prfSupported = result.prfSupported,
            )
        }.getOrElse {
            WebAuthnCapabilities(
                publicKeyCredentialAvailable = false,
                userVerifyingAuthenticatorAvailable = false,
                clientCapabilitiesAvailable = false,
                prfSupported = false,
            )
        }

    override suspend fun createCredential(): WebAuthnOperationResult =
        runCatching {
            WebAuthnInterop.createWebAuthnCredential().await<WebAuthnOperationInteropResult>().toOperationResult()
        }.getOrElse {
            WebAuthnOperationResult(
                status = WebAuthnOperationStatus.FAILED,
                message = it.message,
            )
        }

    override suspend fun authenticate(credentialId: String?): WebAuthnOperationResult =
        runCatching {
            WebAuthnInterop.authenticateWebAuthnCredential(credentialId).await<WebAuthnOperationInteropResult>().toOperationResult(
                fallbackCredentialId = credentialId,
            )
        }.getOrElse {
            WebAuthnOperationResult(
                status = WebAuthnOperationStatus.FAILED,
                message = it.message,
            )
        }
}

private fun String.toWebAuthnOperationStatus(): WebAuthnOperationStatus = when (this) {
    "SUCCESS" -> WebAuthnOperationStatus.SUCCESS
    "CANCELLED" -> WebAuthnOperationStatus.CANCELLED
    "UNAVAILABLE" -> WebAuthnOperationStatus.UNAVAILABLE
    else -> WebAuthnOperationStatus.FAILED
}

private external interface WebAuthnCapabilitiesInteropResult : JsAny {
    val publicKeyCredentialAvailable: Boolean
    val userVerifyingAuthenticatorAvailable: Boolean
    val clientCapabilitiesAvailable: Boolean
    val prfSupported: Boolean
}

private external interface WebAuthnOperationInteropResult : JsAny {
    val status: String
    val credentialId: String?
    val extensionResults: String?
    val prfFirstOutputBase64Url: String?
    val message: String?
}

private fun WebAuthnOperationInteropResult.toOperationResult(
    fallbackCredentialId: String? = null,
): WebAuthnOperationResult = WebAuthnOperationResult(
    status = status.toWebAuthnOperationStatus(),
    credentialId = credentialId ?: fallbackCredentialId,
    extensionResults = extensionResults,
    prfFirstOutputBase64Url = prfFirstOutputBase64Url,
    message = message,
)

@JsModule("./webauthn.mjs")
private external object WebAuthnInterop {
    fun isWebAuthnSupported(): Boolean
    fun queryWebAuthnCapabilities(): Promise<WebAuthnCapabilitiesInteropResult>
    fun createWebAuthnCredential(): Promise<WebAuthnOperationInteropResult>
    fun authenticateWebAuthnCredential(credentialId: String?): Promise<WebAuthnOperationInteropResult>
}
