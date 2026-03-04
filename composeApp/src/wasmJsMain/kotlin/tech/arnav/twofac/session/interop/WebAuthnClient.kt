@file:OptIn(ExperimentalWasmJsInterop::class)

package tech.arnav.twofac.session.interop

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    override fun isSupported(): Boolean = isWebAuthnSupported()

    override suspend fun queryCapabilities(): WebAuthnCapabilities =
        suspendCoroutine { continuation ->
            runCatching {
                queryWebAuthnCapabilities { hasPublicKeyCredential, hasUvAuthenticator, hasClientCapabilities, supportsPrf ->
                    continuation.resume(
                        WebAuthnCapabilities(
                            publicKeyCredentialAvailable = hasPublicKeyCredential,
                            userVerifyingAuthenticatorAvailable = hasUvAuthenticator,
                            clientCapabilitiesAvailable = hasClientCapabilities,
                            prfSupported = supportsPrf,
                        )
                    )
                }
            }.onFailure {
                continuation.resume(
                    WebAuthnCapabilities(
                        publicKeyCredentialAvailable = false,
                        userVerifyingAuthenticatorAvailable = false,
                        clientCapabilitiesAvailable = false,
                        prfSupported = false,
                    )
                )
            }
        }

    override suspend fun createCredential(): WebAuthnOperationResult =
        suspendCoroutine { continuation ->
            runCatching {
                createWebAuthnCredential { status, credentialId, extensionResults, prfFirstOutputBase64Url, message ->
                    continuation.resume(
                        WebAuthnOperationResult(
                            status = status.toWebAuthnOperationStatus(),
                            credentialId = credentialId,
                            extensionResults = extensionResults,
                            prfFirstOutputBase64Url = prfFirstOutputBase64Url,
                            message = message,
                        )
                    )
                }
            }.onFailure {
                continuation.resume(
                    WebAuthnOperationResult(
                        status = WebAuthnOperationStatus.FAILED,
                        message = it.message,
                    )
                )
            }
        }

    override suspend fun authenticate(credentialId: String?): WebAuthnOperationResult =
        suspendCoroutine { continuation ->
            runCatching {
                authenticateWebAuthnCredential(credentialId) { status, returnedCredentialId, extensionResults, prfFirstOutputBase64Url, message ->
                    continuation.resume(
                        WebAuthnOperationResult(
                            status = status.toWebAuthnOperationStatus(),
                            credentialId = returnedCredentialId ?: credentialId,
                            extensionResults = extensionResults,
                            prfFirstOutputBase64Url = prfFirstOutputBase64Url,
                            message = message,
                        )
                    )
                }
            }.onFailure {
                continuation.resume(
                    WebAuthnOperationResult(
                        status = WebAuthnOperationStatus.FAILED,
                        message = it.message,
                    )
                )
            }
        }
}

private fun String.toWebAuthnOperationStatus(): WebAuthnOperationStatus = when (this) {
    "SUCCESS" -> WebAuthnOperationStatus.SUCCESS
    "CANCELLED" -> WebAuthnOperationStatus.CANCELLED
    "UNAVAILABLE" -> WebAuthnOperationStatus.UNAVAILABLE
    else -> WebAuthnOperationStatus.FAILED
}

@JsModule("webauthn.js")
private external fun isWebAuthnSupported(): Boolean

@JsModule("webauthn.js")
private external fun queryWebAuthnCapabilities(onResult: (Boolean, Boolean, Boolean, Boolean) -> Unit)

@JsModule("webauthn.js")
private external fun createWebAuthnCredential(onResult: (String, String?, String?, String?, String?) -> Unit)

@JsModule("webauthn.js")
private external fun authenticateWebAuthnCredential(
    credentialId: String?,
    onResult: (String, String?, String?, String?, String?) -> Unit,
)
