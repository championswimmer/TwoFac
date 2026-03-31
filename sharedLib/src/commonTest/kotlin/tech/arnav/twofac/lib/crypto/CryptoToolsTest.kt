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
