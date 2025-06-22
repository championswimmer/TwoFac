package tech.arnav.twofac.lib.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CryptoProviderTests {

    @Test
    fun testCryptoProviderAvailable() {
        val cryptoProvider = CryptographyProvider.Default
        assertNotNull(cryptoProvider)
        // Check if the provider is OpenSSL (usually "OpenSSL3 (3.x.x)" or similar)
        assertTrue(cryptoProvider.name.startsWith("OpenSSL"))
    }
}