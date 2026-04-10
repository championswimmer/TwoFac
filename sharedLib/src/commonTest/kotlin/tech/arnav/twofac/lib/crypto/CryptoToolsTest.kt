package tech.arnav.twofac.lib.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.encodeToByteString
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptoToolsTest {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testEncryptDecrypt() = runTest {
        val tools = DefaultCryptoTools(CryptographyProvider.Default)

        val (signingKey, salt) = tools.createSigningKey("my-secret-password")
        val data = "my-secret-data".encodeToByteString()

        println("Signing Key: ${signingKey}")

        // Encrypt the data
        val encryptedData = tools.encrypt(signingKey, data)

        // Decrypt the data
        val decryptedData = tools.decrypt(encryptedData, signingKey)

        // Assert that the decrypted data matches the original data
        assertEquals(data.size, decryptedData.size)
        assertEquals(data, decryptedData)
    }

    @Test
    fun testRoundTripCreateSigningKey() = runTest {
        val tools = DefaultCryptoTools(CryptographyProvider.Default)

        // Create a signing key with a password
        val (signingKey, salt) = tools.createSigningKey("my-secret-password")
        val (signingKey2, salt2) = tools.createSigningKey("my-secret-password", salt)

        // Assert that the keys are equal
        assertEquals(signingKey, signingKey2)
    }

    @Test
    fun testCreateSigningKeyWithExplicitIterations() = runTest {
        val tools = DefaultCryptoTools(CryptographyProvider.Default)
        val data = "my-secret-data".encodeToByteString()
        val key200 = tools.createSigningKey("password", iterations = CryptoTools.LEGACY_HASH_ITERATIONS)
        val encrypted200 = tools.encrypt(key200.key, data)
        val decrypted200 = tools.decrypt(encrypted200, key200.key)
        assertEquals(data, decrypted200)
        val key600k = tools.createSigningKey("password", iterations = CryptoTools.TARGET_HASH_ITERATIONS)
        val encrypted600k = tools.encrypt(key600k.key, data)
        val decrypted600k = tools.decrypt(encrypted600k, key600k.key)
        assertEquals(data, decrypted600k)
    }

    @Test
    fun testDifferentIterationsProduceDifferentKeys() = runTest {
        val tools = DefaultCryptoTools(CryptographyProvider.Default)
        val salt = ByteString(ByteArray(16) { it.toByte() })
        val key200 = tools.createSigningKey("password", salt, CryptoTools.LEGACY_HASH_ITERATIONS)
        val key600k = tools.createSigningKey("password", salt, CryptoTools.TARGET_HASH_ITERATIONS)
        assertTrue(key200.key != key600k.key, "Different iteration counts must produce different keys")
        assertEquals(key200.salt, key600k.salt, "Salt should be preserved")
    }

    @Test
    fun testMigrationRoundTrip() = runTest {
        val tools = DefaultCryptoTools(CryptographyProvider.Default)
        val data = "otpauth://totp/Test:user@example.com?secret=JBSWY3DPEHPK3PXP".encodeToByteString()
        val salt = ByteString(ByteArray(16) { it.toByte() })
        val oldKey = tools.createSigningKey("password", salt, CryptoTools.LEGACY_HASH_ITERATIONS)
        val encrypted = tools.encrypt(oldKey.key, data)
        val decrypted = tools.decrypt(encrypted, oldKey.key)
        assertEquals(data, decrypted)
        val newKey = tools.createSigningKey("password", salt, CryptoTools.TARGET_HASH_ITERATIONS)
        val reEncrypted = tools.encrypt(newKey.key, decrypted)
        val finalDecrypted = tools.decrypt(reEncrypted, newKey.key)
        assertEquals(data, finalDecrypted)
    }

    @Test
    fun testAlgoFromStringKnownValues() {
        assertEquals(CryptoTools.Algo.SHA1,   CryptoTools.Algo.fromString("SHA1"))
        assertEquals(CryptoTools.Algo.SHA256, CryptoTools.Algo.fromString("SHA256"))
        assertEquals(CryptoTools.Algo.SHA512, CryptoTools.Algo.fromString("SHA512"))
    }

    @Test
    fun testAlgoFromStringCaseInsensitive() {
        assertEquals(CryptoTools.Algo.SHA256, CryptoTools.Algo.fromString("sha256"))
        assertEquals(CryptoTools.Algo.SHA512, CryptoTools.Algo.fromString("Sha512"))
        assertEquals(CryptoTools.Algo.SHA1,   CryptoTools.Algo.fromString("sha1"))
    }

    @Test
    fun testAlgoFromStringUnknownFallsBackToSha1() {
        // Unknown strings should default to SHA1 (otpauth spec default).
        assertEquals(CryptoTools.Algo.SHA1, CryptoTools.Algo.fromString("MD5"))
        assertEquals(CryptoTools.Algo.SHA1, CryptoTools.Algo.fromString(""))
    }
}
