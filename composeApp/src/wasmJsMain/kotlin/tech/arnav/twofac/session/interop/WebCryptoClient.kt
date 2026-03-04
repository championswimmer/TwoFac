@file:OptIn(ExperimentalWasmJsInterop::class)

package tech.arnav.twofac.session.interop

import kotlinx.coroutines.await
import kotlin.js.Promise

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
    ): WebCryptoEncryptResult? = runCatching {
        val result = CryptoInterop.encryptPasskeyWithWebCrypto(
            plaintext = plaintext,
            prfFirstOutputBase64Url = prfFirstOutputBase64Url,
            context = context,
        ).await<WebCryptoEncryptInteropResult>()
        val salt = result.salt
        val nonce = result.nonce
        val ciphertext = result.ciphertext
        if (result.status == "SUCCESS" && salt != null && nonce != null && ciphertext != null) {
            WebCryptoEncryptResult(
                saltBase64Url = salt,
                nonceBase64Url = nonce,
                ciphertextBase64Url = ciphertext,
            )
        } else {
            null
        }
    }.getOrNull()

    override suspend fun decrypt(
        encryptedResult: WebCryptoEncryptResult,
        prfFirstOutputBase64Url: String,
        context: String,
    ): String? = runCatching {
        val result = CryptoInterop.decryptPasskeyWithWebCrypto(
            saltBase64Url = encryptedResult.saltBase64Url,
            nonceBase64Url = encryptedResult.nonceBase64Url,
            ciphertextBase64Url = encryptedResult.ciphertextBase64Url,
            prfFirstOutputBase64Url = prfFirstOutputBase64Url,
            context = context,
        ).await<WebCryptoDecryptInteropResult>()
        if (result.status == "SUCCESS") result.plaintext else null
    }.getOrNull()
}

private external interface WebCryptoEncryptInteropResult {
    val status: String
    val salt: String?
    val nonce: String?
    val ciphertext: String?
}

private external interface WebCryptoDecryptInteropResult {
    val status: String
    val plaintext: String?
}

@JsModule("./crypto.mjs")
private external object CryptoInterop {
    fun encryptPasskeyWithWebCrypto(
        plaintext: String,
        prfFirstOutputBase64Url: String,
        context: String,
    ): Promise<JsAny?>

    fun decryptPasskeyWithWebCrypto(
        saltBase64Url: String,
        nonceBase64Url: String,
        ciphertextBase64Url: String,
        prfFirstOutputBase64Url: String,
        context: String,
    ): Promise<JsAny?>
}
