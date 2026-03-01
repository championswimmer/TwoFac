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

@JsFun(
    """
    (plaintext, prfFirstOutputBase64Url, context, onResult) => {
      if (
        typeof crypto === "undefined" ||
        crypto == null ||
        typeof crypto.subtle === "undefined" ||
        crypto.subtle == null
      ) {
        onResult("UNAVAILABLE", null, null, null, "WebCryptoUnavailable");
        return;
      }

      const textEncoder = new TextEncoder();

      const base64UrlToBytes = (value) => {
        const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
        const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
        const binary = atob(padded);
        const bytes = new Uint8Array(binary.length);
        for (let index = 0; index < binary.length; index++) {
          bytes[index] = binary.charCodeAt(index);
        }
        return bytes;
      };

      const bytesToBase64Url = (bytes) => {
        let binary = "";
        for (let index = 0; index < bytes.length; index++) {
          binary += String.fromCharCode(bytes[index]);
        }
        return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
      };

      const deriveAesGcmKey = (saltBytes) => {
        const inputKeyMaterial = base64UrlToBytes(prfFirstOutputBase64Url);
        return crypto.subtle
          .importKey("raw", inputKeyMaterial, "HKDF", false, ["deriveKey"])
          .then((baseKey) => {
            return crypto.subtle.deriveKey(
              {
                name: "HKDF",
                hash: "SHA-256",
                salt: saltBytes,
                info: textEncoder.encode(context),
              },
              baseKey,
              { name: "AES-GCM", length: 256 },
              false,
              ["encrypt", "decrypt"],
            );
          });
      };

      const saltBytes = crypto.getRandomValues(new Uint8Array(16));
      const nonceBytes = crypto.getRandomValues(new Uint8Array(12));

      deriveAesGcmKey(saltBytes)
        .then((derivedKey) => {
          return crypto.subtle.encrypt(
            {
              name: "AES-GCM",
              iv: nonceBytes,
              additionalData: textEncoder.encode(context),
              tagLength: 128,
            },
            derivedKey,
            textEncoder.encode(plaintext),
          );
        })
        .then((cipherBuffer) => {
          const ciphertextBytes = new Uint8Array(cipherBuffer);
          onResult(
            "SUCCESS",
            bytesToBase64Url(saltBytes),
            bytesToBase64Url(nonceBytes),
            bytesToBase64Url(ciphertextBytes),
            null,
          );
        })
        .catch((error) => {
          const name = (error && error.name) || "CryptoError";
          onResult("FAILED", null, null, null, name);
        });
    }
    """
)
private external fun encryptPasskeyWithWebCrypto(
    plaintext: String,
    prfFirstOutputBase64Url: String,
    context: String,
    onResult: (String, String?, String?, String?, String?) -> Unit,
)

@JsFun(
    """
    (saltBase64Url, nonceBase64Url, ciphertextBase64Url, prfFirstOutputBase64Url, context, onResult) => {
      if (
        typeof crypto === "undefined" ||
        crypto == null ||
        typeof crypto.subtle === "undefined" ||
        crypto.subtle == null
      ) {
        onResult("UNAVAILABLE", null, "WebCryptoUnavailable");
        return;
      }

      const textEncoder = new TextEncoder();
      const textDecoder = new TextDecoder();

      const base64UrlToBytes = (value) => {
        const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
        const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4);
        const binary = atob(padded);
        const bytes = new Uint8Array(binary.length);
        for (let index = 0; index < binary.length; index++) {
          bytes[index] = binary.charCodeAt(index);
        }
        return bytes;
      };

      const deriveAesGcmKey = (saltBytes) => {
        const inputKeyMaterial = base64UrlToBytes(prfFirstOutputBase64Url);
        return crypto.subtle
          .importKey("raw", inputKeyMaterial, "HKDF", false, ["deriveKey"])
          .then((baseKey) => {
            return crypto.subtle.deriveKey(
              {
                name: "HKDF",
                hash: "SHA-256",
                salt: saltBytes,
                info: textEncoder.encode(context),
              },
              baseKey,
              { name: "AES-GCM", length: 256 },
              false,
              ["encrypt", "decrypt"],
            );
          });
      };

      let saltBytes;
      let nonceBytes;
      let ciphertextBytes;
      try {
        saltBytes = base64UrlToBytes(saltBase64Url);
        nonceBytes = base64UrlToBytes(nonceBase64Url);
        ciphertextBytes = base64UrlToBytes(ciphertextBase64Url);
      } catch (_) {
        onResult("FAILED", null, "DecodeError");
        return;
      }

      deriveAesGcmKey(saltBytes)
        .then((derivedKey) => {
          return crypto.subtle.decrypt(
            {
              name: "AES-GCM",
              iv: nonceBytes,
              additionalData: textEncoder.encode(context),
              tagLength: 128,
            },
            derivedKey,
            ciphertextBytes,
          );
        })
        .then((plaintextBuffer) => {
          const plaintext = textDecoder.decode(new Uint8Array(plaintextBuffer));
          onResult("SUCCESS", plaintext, null);
        })
        .catch((error) => {
          const name = (error && error.name) || "CryptoError";
          onResult("FAILED", null, name);
        });
    }
    """
)
private external fun decryptPasskeyWithWebCrypto(
    saltBase64Url: String,
    nonceBase64Url: String,
    ciphertextBase64Url: String,
    prfFirstOutputBase64Url: String,
    context: String,
    onResult: (String, String?, String?) -> Unit,
)
