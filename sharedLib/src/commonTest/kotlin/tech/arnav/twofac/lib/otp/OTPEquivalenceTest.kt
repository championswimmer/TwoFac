package tech.arnav.twofac.lib.otp

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OTPEquivalenceTest {

    @Test
    fun testEquivalentWhenIssuerAccountDigitsAndSecretMatch() {
        val first = TOTP(
            digits = 6,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "test@example.com",
            issuer = "GitHub",
            timeInterval = 30,
        )
        val second = TOTP(
            digits = 6,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "test@example.com",
            issuer = "GitHub",
            timeInterval = 60,
        )

        assertTrue(first.isEquivalent(second))
    }

    @Test
    fun testNotEquivalentWhenDigitsDiffer() {
        val first = TOTP(
            digits = 6,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "test@example.com",
            issuer = "GitHub",
            timeInterval = 30,
        )
        val second = TOTP(
            digits = 8,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "test@example.com",
            issuer = "GitHub",
            timeInterval = 30,
        )

        assertFalse(first.isEquivalent(second))
    }

    @Test
    fun testNotEquivalentWhenIssuerAccountOrSecretDiffers() {
        val base = HOTP(
            digits = 6,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "test@example.com",
            issuer = "GitHub",
        )

        val issuerDifferent = HOTP(
            digits = 6,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "test@example.com",
            issuer = "Google",
        )
        val accountDifferent = HOTP(
            digits = 6,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "other@example.com",
            issuer = "GitHub",
        )
        val secretDifferent = HOTP(
            digits = 6,
            secret = "GEZDGNBVGY3TQOJQ",
            accountName = "test@example.com",
            issuer = "GitHub",
        )

        assertFalse(base.isEquivalent(issuerDifferent))
        assertFalse(base.isEquivalent(accountDifferent))
        assertFalse(base.isEquivalent(secretDifferent))
    }

    @Test
    fun testNullIssuerAndEmptyIssuerAreEquivalent() {
        val nullIssuer = TOTP(
            digits = 6,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "test@example.com",
            issuer = null,
            timeInterval = 30,
        )
        val emptyIssuer = TOTP(
            digits = 6,
            secret = "JBSWY3DPEHPK3PXP",
            accountName = "test@example.com",
            issuer = "",
            timeInterval = 30,
        )

        assertTrue(nullIssuer.isEquivalent(emptyIssuer))
    }
}
