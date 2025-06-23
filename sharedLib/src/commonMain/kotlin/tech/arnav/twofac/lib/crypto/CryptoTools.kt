@file:OptIn(ExperimentalNativeApi::class)

package tech.arnav.twofac.lib.crypto

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName

/**
 * All cryptographic tools we need for 2-factor authentication.
 * 1. HMAC-SHA1 (or HMAC-SHA256) for TOTP and HOTP
 * 2. PBKDF2 for creating keys from passwords
 * 3. AES for encrypting and decrypting secrets
 */
interface CryptoTools {

    /**
     * SHA algorithm types supported for HMAC operations
     */
    enum class ShaAlgorithm {
        SHA1,
        SHA256,
        SHA512
    }

    interface SigningKey {
        /**
         * The resulting key derived from the password and salt
         */
        val key: ByteArray

        /**
         * The salt used in the key derivation process
         */
        val salt: ByteArray
    }

    /**
     * Generate an HMAC using the specified SHA algorithm
     *
     * @param algorithm The SHA algorithm to use (SHA1, SHA128, or SHA256)
     * @param key The key to use for the HMAC
     * @param data The data to generate the HMAC for
     * @return The generated HMAC as a ByteArray
     */
    @CName("hmac_sha")
    suspend fun hmacSha(algorithm: ShaAlgorithm, key: ByteArray, data: ByteArray): ByteArray

    /**
     * Derive a key from a password using PBKDF2
     *
     * @param passKey The password to derive the key from
     * @param salt The salt to use for key derivation
     * @return The derived signing key as a ByteArray
     */
    @CName("create_signing_key")
    suspend fun createSigningKey(passKey: String): SigningKey

    /**
     * Encrypt data using a key
     *
     * @param key The key to use for encryption
     * @param secret The data to encrypt
     * @return The encrypted data as a ByteArray
     */
    @CName("encrypt_data")
    suspend fun encrypt(key: ByteArray, secret: ByteArray): ByteArray

    /**
     * Decrypt data using a key
     *
     * @param encryptedData The encrypted data to decrypt
     * @param key The key to use for decryption
     * @return The decrypted data as a ByteArray
     */
    @CName("decrypt_data")
    suspend fun decrypt(encryptedData: ByteArray, key: ByteArray): ByteArray
}
