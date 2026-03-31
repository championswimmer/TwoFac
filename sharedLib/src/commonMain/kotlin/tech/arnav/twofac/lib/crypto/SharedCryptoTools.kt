package tech.arnav.twofac.lib.crypto

import dev.whyoleg.cryptography.CryptographyProvider

/**
 * Module-internal singleton [CryptoTools] instance backed by the platform default provider.
 *
 * Centralises construction so domain-model classes (e.g. [tech.arnav.twofac.lib.otp.HOTP])
 * don't each own a separate `DefaultCryptoTools(CryptographyProvider.Default)`.  Callers that
 * need a different provider (e.g. tests) can pass an explicit instance instead.
 */
internal val sharedCryptoTools: CryptoTools = DefaultCryptoTools(CryptographyProvider.Default)
