package tech.arnav.twofac.lib.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptoToolsTest {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testEncryptDecrypt() = runTest {
        val tools = DefaultCryptoTools(CryptographyProvider.Default)

        val signingKey = tools.createSigningKey("my-secret-password")
        val data = "my-secret-data".encodeToByteArray()

        println("Signing Key: ${signingKey.key.toHexString()}")

        // Encrypt the data
        val encryptedData = tools.encrypt(signingKey.key, data)

        // Decrypt the data
        val decryptedData = tools.decrypt(encryptedData, signingKey.key)

        // Assert that the decrypted data matches the original data
        assertEquals(data.size, decryptedData.size)
        assertEquals(data.toHexString(), decryptedData.toHexString())
    }
}