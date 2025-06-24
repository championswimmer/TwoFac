package tech.arnav.twofac.lib.otp

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.crypto.Encoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TOTPTest {

    @Test
    fun testGenerateOTP() = runTest {
        // Use the same secret as in HOTPTest
        val totp = TOTP(
            digits = 6,
            secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            baseTime = 0,
            timeInterval = 30
        )

        // Test with specific timestamps
        // For time = 0, counter = 0, expected OTP = "755224" (from HOTPTest)
        val otp1 = totp.generateOTP(0)
        assertEquals("755224", otp1, "OTP for time 0 should match expected value")

        // For time = 30, counter = 1, expected OTP = "287082" (from HOTPTest)
        val otp2 = totp.generateOTP(30)
        assertEquals("287082", otp2, "OTP for time 30 should match expected value")

        // For time = 59, counter = 1 (still in the same time window), expected OTP = "287082"
        val otp3 = totp.generateOTP(59)
        assertEquals("287082", otp3, "OTP for time 59 should match expected value")

        // For time = 60, counter = 2, expected OTP = "359152" (from HOTPTest)
        val otp4 = totp.generateOTP(60)
        assertEquals("359152", otp4, "OTP for time 60 should match expected value")
    }

    @Test
    fun testValidateOTP() = runTest {
        val totp = TOTP(
            digits = 6,
            secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            baseTime = 0,
            timeInterval = 30
        )

        // Test validation with a known OTP at exact time
        val isValid1 = totp.validateOTP("755224", 0)
        assertTrue(isValid1, "OTP should be valid for time 0")

        // Test validation with a known OTP at a time in the same window
        val isValid2 = totp.validateOTP("755224", 29)
        assertTrue(isValid2, "OTP should be valid for time 29 (same window as time 0)")

        // Test validation with an invalid OTP
        val isInvalid = totp.validateOTP("123456", 0)
        assertFalse(isInvalid, "OTP should be invalid")
    }

    @Test
    fun testTimeWindowValidation() = runTest {
        val totp = TOTP(
            digits = 6,
            secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            baseTime = 0,
            timeInterval = 30
        )

        // Test validation with previous time window
        // For time = 60 (counter = 2), the OTP is "359152"
        // This should be valid at time = 90 (counter = 3) because we check previous window
        val isValidPrevious = totp.validateOTP("359152", 90)
        assertTrue(isValidPrevious, "OTP from previous window should be valid")

        // Test validation with current time window
        // For time = 90 (counter = 3), the OTP is "969429"
        val isValidCurrent = totp.validateOTP("969429", 90)
        assertTrue(isValidCurrent, "OTP from current window should be valid")

        // Test validation with next time window
        // For time = 120 (counter = 4), the OTP is "338314"
        // This should be valid at time = 90 (counter = 3) because we check next window
        val isValidNext = totp.validateOTP("338314", 90)
        assertTrue(isValidNext, "OTP from next window should be valid")

        // Test validation with window outside the -1/+1 range
        // For time = 0 (counter = 0), the OTP is "755224"
        // This should NOT be valid at time = 90 (counter = 3)
        val isInvalidOutsideWindow = totp.validateOTP("755224", 90)
        assertFalse(isInvalidOutsideWindow, "OTP from outside the time window range should be invalid")
    }

    @Test
    fun testCustomBaseTimeAndInterval() = runTest {
        // Test with custom base time and interval
        val customTotp = TOTP(
            digits = 6,
            secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            baseTime = 100,  // Start at time 100
            timeInterval = 60  // 60-second intervals
        )

        // For time = 100, counter = 0, expected OTP = "755224"
        val otp1 = customTotp.generateOTP(100)
        assertEquals("755224", otp1, "OTP for time 100 with base time 100 should match expected value")

        // For time = 160, counter = 1, expected OTP = "287082"
        val otp2 = customTotp.generateOTP(160)
        assertEquals("287082", otp2, "OTP for time 160 with base time 100 and interval 60 should match expected value")

        // Validation should work with the custom parameters
        val isValid = customTotp.validateOTP("755224", 100)
        assertTrue(isValid, "OTP should be valid with custom base time and interval")
    }

    /**
     * Data class to hold RFC 6238 test vector data
     */
    private data class RFC6238TestVector(
        val timestamp: Long,
        val utcTime: String, // For documentation purposes
        val asciiKey: String, // ASCII key from RFC 6238
        val algorithm: CryptoTools.Algo,
        val expectedOtp: String
    )

    /**
     * Test vectors from RFC 6238
     * Each test vector includes:
     * - Unix Timestamp (sec)
     * - UTC Time (for documentation)
     * - HMAC Key (ASCII)
     * - Algorithm
     * - Expected TOTP Code
     */
    @Test
    fun testRFC6238Vectors() = runTest {
        // Define the test vectors from RFC 6238
        @Suppress("detekt:MaxLineLength")
        val testVectors = listOf(
            RFC6238TestVector(59, "1970-01-01 00:00:59", "12345678901234567890", CryptoTools.Algo.SHA1, "94287082"),
            RFC6238TestVector(
                59,
                "1970-01-01 00:00:59",
                "12345678901234567890123456789012",
                CryptoTools.Algo.SHA256,
                "46119246"
            ),
            RFC6238TestVector(
                59,
                "1970-01-01 00:00:59",
                "1234567890123456789012345678901234567890123456789012345678901234",
                CryptoTools.Algo.SHA512,
                "90693936"
            ),
            RFC6238TestVector(
                1111111109,
                "2005-03-18 01:58:29",
                "12345678901234567890",
                CryptoTools.Algo.SHA1,
                "07081804"
            ),
            RFC6238TestVector(
                1111111109,
                "2005-03-18 01:58:29",
                "12345678901234567890123456789012",
                CryptoTools.Algo.SHA256,
                "68084774"
            ),
            RFC6238TestVector(
                1111111109,
                "2005-03-18 01:58:29",
                "1234567890123456789012345678901234567890123456789012345678901234",
                CryptoTools.Algo.SHA512,
                "25091201"
            ),
            RFC6238TestVector(
                1111111111,
                "2005-03-18 01:58:31",
                "12345678901234567890",
                CryptoTools.Algo.SHA1,
                "14050471"
            ),
            RFC6238TestVector(
                1111111111,
                "2005-03-18 01:58:31",
                "12345678901234567890123456789012",
                CryptoTools.Algo.SHA256,
                "67062674"
            ),
            RFC6238TestVector(
                1111111111,
                "2005-03-18 01:58:31",
                "1234567890123456789012345678901234567890123456789012345678901234",
                CryptoTools.Algo.SHA512,
                "99943326"
            )
        )

        // Test each vector
        for (vector in testVectors) {
            // Convert ASCII key to Base32-encoded key
            val base32Key = Encoding.encodeBase32(
                Encoding.decodeAscii(vector.asciiKey)
            )

            // Create a TOTP instance with the appropriate parameters
            val totp = TOTP(
                digits = 8, // RFC 6238 uses 8 digits
                algorithm = vector.algorithm,
                secret = base32Key, // Use Base32-encoded key
                baseTime = 0,
                timeInterval = 30
            )

            // Generate OTP and verify it matches the expected code
            val otp = totp.generateOTP(vector.timestamp)
            assertEquals(
                vector.expectedOtp,
                otp,
                "OTP for time ${vector.utcTime} (${vector.timestamp}) with algorithm ${vector.algorithm} should match expected value"
            )

            // Print the date for documentation purposes
            println("Validated TOTP: ${otp} Timestamp: ${vector.timestamp}, UTC time: ${vector.utcTime}")
        }
    }
}
