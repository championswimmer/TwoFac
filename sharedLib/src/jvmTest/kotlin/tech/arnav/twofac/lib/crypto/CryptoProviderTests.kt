package tech.arnav.twofac.lib.crypto

import dev.whyoleg.cryptography.CryptographyProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class CryptoProviderTests {

    @Test
    fun testJvmCryptoProviderAvailable() {
        val cryptoProvider = CryptographyProvider.Default
        assertNotNull(cryptoProvider)
        assertEquals("JDK", cryptoProvider.name)
    }
}