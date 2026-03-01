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

@JsFun(
    """
    () => {
      return (
        typeof window !== "undefined" &&
        window.isSecureContext === true &&
        typeof window.PublicKeyCredential !== "undefined" &&
        typeof navigator !== "undefined" &&
        navigator.credentials != null
      );
    }
    """
)
private external fun isWebAuthnSupported(): Boolean

@JsFun(
    """
    (onResult) => {
      const hasPublicKeyCredential =
        typeof window !== "undefined" &&
        typeof window.PublicKeyCredential !== "undefined";
      if (!hasPublicKeyCredential) {
        onResult(false, false, false, false);
        return;
      }

      const publicKeyCredential = window.PublicKeyCredential;
      const hasClientCapabilities =
        typeof publicKeyCredential.getClientCapabilities === "function";

      const resolveWithPrf = (supportsPrf) => {
        const hasUvpaCheck =
          typeof publicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable === "function";
        if (!hasUvpaCheck) {
          onResult(true, false, hasClientCapabilities, !!supportsPrf);
          return;
        }

        publicKeyCredential.isUserVerifyingPlatformAuthenticatorAvailable()
          .then((isAvailable) => {
            onResult(true, !!isAvailable, hasClientCapabilities, !!supportsPrf);
          })
          .catch(() => {
            onResult(true, false, hasClientCapabilities, !!supportsPrf);
          });
      };

      if (!hasClientCapabilities) {
        resolveWithPrf(false);
        return;
      }

      try {
        const maybeCapabilities = publicKeyCredential.getClientCapabilities();
        if (maybeCapabilities && typeof maybeCapabilities.then === "function") {
          maybeCapabilities
            .then((capabilities) => resolveWithPrf(!!(capabilities && capabilities.prf)))
            .catch(() => resolveWithPrf(false));
        } else {
          resolveWithPrf(!!(maybeCapabilities && maybeCapabilities.prf));
        }
      } catch (_) {
        resolveWithPrf(false);
      }
    }
    """
)
private external fun queryWebAuthnCapabilities(onResult: (Boolean, Boolean, Boolean, Boolean) -> Unit)

@JsFun(
    """
    (onResult) => {
      const handleError = (error) => {
        const name = (error && error.name) || "UnknownError";
        if (name === "AbortError" || name === "NotAllowedError") {
          onResult("CANCELLED", null, null, null, name);
          return;
        }
        if (name === "NotSupportedError" || name === "SecurityError" || name === "InvalidStateError") {
          onResult("UNAVAILABLE", null, null, null, name);
          return;
        }
        onResult("FAILED", null, null, null, name);
      };

      if (
        typeof navigator === "undefined" ||
        navigator.credentials == null ||
        typeof window === "undefined" ||
        typeof window.PublicKeyCredential === "undefined" ||
        !window.isSecureContext
      ) {
        onResult("UNAVAILABLE", null, null, null, "WebAuthnUnavailable");
        return;
      }

      const challenge = new Uint8Array(32);
      const userId = new Uint8Array(16);
      crypto.getRandomValues(challenge);
      crypto.getRandomValues(userId);

      const options = {
        publicKey: {
          challenge,
          rp: { name: "TwoFac" },
          user: {
            id: userId,
            name: "twofac-user",
            displayName: "TwoFac User",
          },
          pubKeyCredParams: [
            { type: "public-key", alg: -7 },
            { type: "public-key", alg: -257 },
          ],
          authenticatorSelection: {
            userVerification: "required",
          },
          timeout: 60000,
          extensions: {
            prf: {
              eval: {
                first: new Uint8Array([116, 119, 111, 102, 97, 99]),
              },
            },
          },
        },
      };

      navigator.credentials.create(options)
        .then((credential) => {
          const credentialRawId = credential && credential.rawId ? new Uint8Array(credential.rawId) : null;
          let credentialId = null;
          if (credentialRawId != null) {
            let binary = "";
            for (let index = 0; index < credentialRawId.length; index++) {
              binary += String.fromCharCode(credentialRawId[index]);
            }
            credentialId = btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
          }
          const extensionResults =
            credential && typeof credential.getClientExtensionResults === "function"
              ? JSON.stringify(credential.getClientExtensionResults())
              : null;
          onResult("SUCCESS", credentialId, extensionResults, null, null);
        })
        .catch(handleError);
    }
    """
)
private external fun createWebAuthnCredential(onResult: (String, String?, String?, String?, String?) -> Unit)

@JsFun(
    """
    (credentialId, onResult) => {
      const handleError = (error) => {
        const name = (error && error.name) || "UnknownError";
        if (name === "AbortError" || name === "NotAllowedError") {
          onResult("CANCELLED", credentialId || null, null, null, name);
          return;
        }
        if (name === "NotSupportedError" || name === "SecurityError") {
          onResult("UNAVAILABLE", credentialId || null, null, null, name);
          return;
        }
        onResult("FAILED", credentialId || null, null, null, name);
      };

      if (
        typeof navigator === "undefined" ||
        navigator.credentials == null ||
        typeof window === "undefined" ||
        typeof window.PublicKeyCredential === "undefined" ||
        !window.isSecureContext
      ) {
        onResult("UNAVAILABLE", credentialId || null, null, null, "WebAuthnUnavailable");
        return;
      }

      const challenge = new Uint8Array(32);
      crypto.getRandomValues(challenge);

      const publicKey = {
        challenge,
        userVerification: "required",
        timeout: 60000,
        extensions: {
          prf: {
            eval: {
              first: new Uint8Array([116, 119, 111, 102, 97, 99]),
            },
          },
        },
      };

      if (credentialId) {
        try {
          const base64 = credentialId.replace(/-/g, "+").replace(/_/g, "/");
          const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
          const binary = atob(padded);
          const bytes = new Uint8Array(binary.length);
          for (let index = 0; index < binary.length; index++) {
            bytes[index] = binary.charCodeAt(index);
          }
          publicKey.allowCredentials = [{ type: "public-key", id: bytes }];
        } catch (_) {
          // Invalid credential ID format should not break the unlock attempt.
        }
      }

      navigator.credentials.get({ publicKey })
        .then((assertion) => {
          const extensionResultObject =
            assertion && typeof assertion.getClientExtensionResults === "function"
              ? assertion.getClientExtensionResults()
              : null;
          const extensionResults = extensionResultObject ? JSON.stringify(extensionResultObject) : null;
          let prfFirstOutputBase64Url = null;
          try {
            const prfFirstOutput =
              extensionResultObject &&
              extensionResultObject.prf &&
              extensionResultObject.prf.results
                ? extensionResultObject.prf.results.first
                : null;
            if (prfFirstOutput instanceof ArrayBuffer) {
              const prfBytes = new Uint8Array(prfFirstOutput);
              let binary = "";
              for (let index = 0; index < prfBytes.length; index++) {
                binary += String.fromCharCode(prfBytes[index]);
              }
              prfFirstOutputBase64Url = btoa(binary)
                .replace(/\+/g, "-")
                .replace(/\//g, "_")
                .replace(/=+$/g, "");
            } else if (typeof ArrayBuffer !== "undefined" && ArrayBuffer.isView(prfFirstOutput)) {
              const view = prfFirstOutput;
              const prfBytes = new Uint8Array(view.buffer, view.byteOffset, view.byteLength);
              let binary = "";
              for (let index = 0; index < prfBytes.length; index++) {
                binary += String.fromCharCode(prfBytes[index]);
              }
              prfFirstOutputBase64Url = btoa(binary)
                .replace(/\+/g, "-")
                .replace(/\//g, "_")
                .replace(/=+$/g, "");
            }
          } catch (_) {
            prfFirstOutputBase64Url = null;
          }
          onResult("SUCCESS", credentialId || null, extensionResults, prfFirstOutputBase64Url, null);
        })
        .catch(handleError);
    }
    """
)
private external fun authenticateWebAuthnCredential(
    credentialId: String?,
    onResult: (String, String?, String?, String?, String?) -> Unit,
)
