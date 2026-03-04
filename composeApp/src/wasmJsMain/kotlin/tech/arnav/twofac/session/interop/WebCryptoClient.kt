@file:OptIn(ExperimentalWasmJsInterop::class)

package tech.arnav.twofac.session.interop

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal data class WebCryptoEncryptResult(
    val saltBase64Url: String,
    val nonceBase64Url: String,
    val ciphertextBase64Url: String,
)

internal interface WebCryptoClient {
    suspend fun encrypt(
        plaintext: String,
        prfFirstOutputBase64Url: String,
        context: String,
    ): WebCryptoEncryptResult?

    suspend fun decrypt(
        encryptedResult: WebCryptoEncryptResult,
        prfFirstOutputBase64Url: String,
        context: String,
    ): String?
}

internal class BrowserWebCryptoClient : WebCryptoClient {
    override suspend fun encrypt(
        plaintext: String,
        prfFirstOutputBase64Url: String,
        context: String,
    ): WebCryptoEncryptResult? = suspendCoroutine { continuation ->
        runCatching {
            encryptPasskeyWithWebCrypto(
                plaintext,
                prfFirstOutputBase64Url,
                context
            ) { status, salt, nonce, ciphertext, _ ->
                if (status == "SUCCESS" && salt != null && nonce != null && ciphertext != null) {
                    continuation.resume(
                        WebCryptoEncryptResult(
                            saltBase64Url = salt,
                            nonceBase64Url = nonce,
                            ciphertextBase64Url = ciphertext,
                        )
                    )
                } else {
                    continuation.resume(null)
                }
            }
        }.onFailure {
            continuation.resume(null)
        }
    }

    override suspend fun decrypt(
        encryptedResult: WebCryptoEncryptResult,
        prfFirstOutputBase64Url: String,
        context: String,
    ): String? = suspendCoroutine { continuation ->
        runCatching {
            decryptPasskeyWithWebCrypto(
                encryptedResult.saltBase64Url,
                encryptedResult.nonceBase64Url,
                encryptedResult.ciphertextBase64Url,
                prfFirstOutputBase64Url,
                context,
            ) { status, plaintext, _ ->
                if (status == "SUCCESS") {
                    continuation.resume(plaintext)
                } else {
                    continuation.resume(null)
                }
            }
        }.onFailure {
            continuation.resume(null)
        }
    }
}

@JsModule("crypto.js")
private external fun encryptPasskeyWithWebCrypto(
    plaintext: String,
    prfFirstOutputBase64Url: String,
    context: String,
    onResult: (String, String?, String?, String?, String?) -> Unit,
)

@JsModule("crypto.js")
private external fun decryptPasskeyWithWebCrypto(
    saltBase64Url: String,
    nonceBase64Url: String,
    ciphertextBase64Url: String,
    prfFirstOutputBase64Url: String,
    context: String,
    onResult: (String, String?, String?) -> Unit,
)
