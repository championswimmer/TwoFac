@file:OptIn(ExperimentalNativeApi::class)

package tech.arnav.twofac.lib.crypto

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.*
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.experimental.ExperimentalNativeApi

class DefaultCryptoTools(val cryptoProvider: CryptographyProvider) : CryptoTools {
    val hmac = cryptoProvider.get(HMAC)
    val pbkdf2 = cryptoProvider.get(PBKDF2)
    val aesGcm = cryptoProvider.get(AES.GCM)


    @OptIn(DelicateCryptographyApi::class)
    override suspend fun hmacSha(algorithm: CryptoTools.ShaAlgorithm, key: ByteArray, data: ByteArray): ByteArray {
        val keyDecoder = when (algorithm) {
            CryptoTools.ShaAlgorithm.SHA1 -> hmac.keyDecoder(SHA1)
            CryptoTools.ShaAlgorithm.SHA256 -> hmac.keyDecoder(SHA256)
            CryptoTools.ShaAlgorithm.SHA512 -> hmac.keyDecoder(SHA512)
        }
        val key = keyDecoder.decodeFromByteArray(HMAC.Key.Format.RAW, key)
        val signature = key.signatureGenerator().generateSignature(data)
        return signature
    }

    override suspend fun createSigningKey(passKey: String): CryptoTools.SigningKey {
        // generate a salt
        val salt = CryptographyRandom.nextBytes(16) // 128-bit salt
        // derive a key using PBKDF2
        val secretDerivation = pbkdf2.secretDerivation(SHA256, 200, 256.bits, salt)
        val signingKey = secretDerivation.deriveSecretToByteArray(passKey.encodeToByteArray())
        return object : CryptoTools.SigningKey {
            override val key: ByteArray = signingKey
            override val salt: ByteArray = salt
        }
    }

    override suspend fun encrypt(key: ByteArray, secret: ByteArray): ByteArray {
        val keyDecoder = aesGcm.keyDecoder()
        val signingKey = keyDecoder.decodeFromByteArray(AES.Key.Format.RAW, key)
        val cipher = signingKey.cipher()
        val cipherText = cipher.encrypt(secret)
        return cipherText
    }

    override suspend fun decrypt(encryptedData: ByteArray, key: ByteArray): ByteArray {
        val keyDecoder = aesGcm.keyDecoder()
        val signingKey = keyDecoder.decodeFromByteArray(AES.Key.Format.RAW, key)
        val cipher = signingKey.cipher()
        val plainText = cipher.decrypt(encryptedData)
        return plainText
    }
}