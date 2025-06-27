package tech.arnav.twofac.lib.crypto

import kotlinx.io.bytestring.ByteString

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
    enum class Algo {
        SHA1,
        SHA256,
        SHA512
    }

    public data class SigningKey(
        /**
         * The resulting key derived from the password and salt
         */

        val key: ByteString,

        /**
         * The salt used in the key derivation process
         */
        val salt: ByteString
    )

    /**
     * Generate an HMAC using the specified SHA algorithm
     *
     * @param algorithm The SHA algorithm to use (SHA1, SHA128, or SHA256)
     * @param key The key to use for the HMAC
     * @param data The data to generate the HMAC for
     * @return The generated HMAC as a ByteString
     */
    suspend fun hmacSha(algorithm: Algo, key: ByteString, data: ByteString): ByteString

    /**
     * Derive a key from a password using PBKDF2
     *
     * @param passKey The password to derive the key from
     * @param salt The salt to use for key derivation. If null, a random salt will be generated
     * @return The derived signing key as a ByteString
     */
    suspend fun createSigningKey(passKey: String, salt: ByteString? = null): SigningKey

    /**
     * Create a hash of the passKey using the specified SHA algorithm
     *
     * @param passKey The password to hash
     * @param algorithm The SHA algorithm to use (SHA1, SHA256, or SHA512)
     * @return The resulting hash as a ByteString
     */
    suspend fun createHash(passKey: String, algorithm: CryptoTools.Algo): ByteString

    /**
     * Encrypt data using a key
     *
     * @param key The key to use for encryption
     * @param secret The data to encrypt
     * @return The encrypted data as a ByteString
     */
    suspend fun encrypt(key: ByteString, secret: ByteString): ByteString

    /**
     * Decrypt data using a key
     *
     * @param encryptedData The encrypted data to decrypt
     * @param key The key to use for decryption
     * @return The decrypted data as a ByteString
     */
    suspend fun decrypt(encryptedData: ByteString, key: ByteString): ByteString
}
