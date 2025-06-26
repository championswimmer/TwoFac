package tech.arnav.twofac.lib.otp

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HOTPTest {

    @Test
    fun testGenerateOTP() = runTest {
        // Test vector from RFC 4226
        // Secret: "12345678901234567890" (ASCII)
        // Base32 encoded: "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        val hotp = HOTP(
            digits = 6,
            secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            accountName = "test@example.com",
            issuer = "Test"
        )

        // Test vectors from RFC 4226 (Appendix D)
        val expectedOTPs = listOf(
            "755224", // Counter = 0
            "287082", // Counter = 1
            "359152", // Counter = 2
            "969429", // Counter = 3
            "338314", // Counter = 4
            "254676", // Counter = 5
            "287922", // Counter = 6
            "162583", // Counter = 7
            "399871", // Counter = 8
            "520489"  // Counter = 9
        )

        // Test the first 10 OTPs
        for (i in 0..9) {
            val otp = hotp.generateOTP(i.toLong())
            println("Counter = $i, OTP = $otp, Expected = ${expectedOTPs[i]}")
            assertEquals(expectedOTPs[i], otp, "OTP for counter $i should match expected value")
        }
    }

    @Test
    fun testValidateOTP() = runTest {
        val hotp = HOTP(
            digits = 6,
            secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            accountName = "test@example.com",
            issuer = "Test"
        )

        // Test validation with a known OTP
        val isValid = hotp.validateOTP("755224", 0)
        assertEquals(true, isValid, "OTP should be valid for counter 0")

        // Test validation with an invalid OTP
        val isInvalid = hotp.validateOTP("123456", 0)
        assertEquals(false, isInvalid, "OTP should be invalid")
    }
}
