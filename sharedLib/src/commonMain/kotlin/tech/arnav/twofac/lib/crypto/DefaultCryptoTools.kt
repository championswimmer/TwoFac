package tech.arnav.twofac.lib.crypto

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.HMAC
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA1
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.algorithms.SHA512
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.io.bytestring.ByteString

class DefaultCryptoTools(val cryptoProvider: CryptographyProvider) : CryptoTools {

    // TODO: make a CryptoConfig class to hold these constants and pass in
    companion object {
        const val SALT_LENGTH = 16 // 128 bits
        const val HASH_ITERATIONS = 200 // Number of iterations for PBKDF2

    }

    val hmac = cryptoProvider.get(HMAC)
    val pbkdf2 = cryptoProvider.get(PBKDF2)
    val aesGcm = cryptoProvider.get(AES.GCM)


    @OptIn(DelicateCryptographyApi::class)
    override suspend fun hmacSha(algorithm: CryptoTools.Algo, key: ByteString, data: ByteString): ByteString {
        val keyDecoder = when (algorithm) {
            CryptoTools.Algo.SHA1 -> hmac.keyDecoder(SHA1)
            CryptoTools.Algo.SHA256 -> hmac.keyDecoder(SHA256)
            CryptoTools.Algo.SHA512 -> hmac.keyDecoder(SHA512)
        }
        val hmacKey = keyDecoder.decodeFromByteString(HMAC.Key.Format.RAW, key)
        val signature = hmacKey.signatureGenerator().generateSignature(data.toByteArray())
        return ByteString(signature)
    }

    override suspend fun createSigningKey(passKey: String, salt: ByteString?): CryptoTools.SigningKey {
        // generate a salt
        val saltBytes = salt?.toByteArray() ?: CryptographyRandom.nextBytes(SALT_LENGTH) // 128-bit salt
        // derive a key using PBKDF2
        val secretDerivation = pbkdf2.secretDerivation(SHA256, HASH_ITERATIONS, 256.bits, saltBytes)
        val signingKey = secretDerivation.deriveSecretToByteArray(passKey.encodeToByteArray())
        return CryptoTools.SigningKey(key = ByteString(signingKey), salt = ByteString(saltBytes))
    }

    @OptIn(DelicateCryptographyApi::class)
    override suspend fun createHash(passKey: String, algorithm: CryptoTools.Algo): ByteString {
        val hashFunction = when (algorithm) {
            CryptoTools.Algo.SHA1 -> cryptoProvider.get(SHA1)
            CryptoTools.Algo.SHA256 -> cryptoProvider.get(SHA256)
            CryptoTools.Algo.SHA512 -> cryptoProvider.get(SHA512)
        }
        val hash = hashFunction.hasher().hash(passKey.encodeToByteArray())
        return ByteString(hash)
    }

    override suspend fun encrypt(key: ByteString, secret: ByteString): ByteString {
        val keyDecoder = aesGcm.keyDecoder()
        val signingKey = keyDecoder.decodeFromByteString(AES.Key.Format.RAW, key)
        val cipher = signingKey.cipher()
        val cipherText = cipher.encrypt(secret.toByteArray())
        return ByteString(cipherText)
    }

    override suspend fun decrypt(encryptedData: ByteString, key: ByteString): ByteString {
        val keyDecoder = aesGcm.keyDecoder()
        val signingKey = keyDecoder.decodeFromByteString(AES.Key.Format.RAW, key)
        val cipher = signingKey.cipher()
        val plainText = cipher.decrypt(encryptedData.toByteArray())
        return ByteString(plainText)
    }
}
