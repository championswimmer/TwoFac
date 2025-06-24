package tech.arnav.twofac.lib.uri

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.otp.HOTP
import tech.arnav.twofac.lib.otp.TOTP
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OtpAuthURITest {

    // Test secret (same as used in HOTP and TOTP tests)
    private val testSecret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

    @Test
    fun testCreateTOTPUri() = runTest {
        val totp = TOTP(
            digits = 6,
            algorithm = CryptoTools.Algo.SHA1,
            secret = testSecret,
            timeInterval = 30
        )

        val uri = OtpAuthURI.create(totp, "Example:alice@example.com", "Example")

        // Verify the URI format
        assertTrue(
            uri.startsWith("otpauth://totp/Example%3Aalice%40example.com?"),
            "URI should start with correct prefix"
        )
        assertTrue(uri.contains("secret=$testSecret"), "URI should contain the secret")
        assertTrue(uri.contains("issuer=Example"), "URI should contain the issuer")
    }

    @Test
    fun testCreateHOTPUri() = runTest {
        val hotp = HOTP(
            digits = 6,
            algorithm = CryptoTools.Algo.SHA1,
            secret = testSecret
        )

        val uri = OtpAuthURI.create(hotp, "Example:bob@example.com", "Example")

        // Verify the URI format
        assertTrue(
            uri.startsWith("otpauth://hotp/Example%3Abob%40example.com?"),
            "URI should start with correct prefix"
        )
        assertTrue(uri.contains("secret=$testSecret"), "URI should contain the secret")
        assertTrue(uri.contains("issuer=Example"), "URI should contain the issuer")
        assertTrue(uri.contains("counter=0"), "URI should contain the counter")
    }

    @Test
    fun testParseTOTPUri() = runTest {
        val uri = "otpauth://totp/Example%3Aalice%40example.com?secret=$testSecret&issuer=Example&period=30"
        val otp = OtpAuthURI.parse(uri)

        // Verify the OTP object
        assertTrue(otp is TOTP, "Parsed OTP should be a TOTP instance")
        assertEquals(testSecret, otp.secret, "Secret should match")
        assertEquals(6, otp.digits, "Digits should match")
        assertEquals(CryptoTools.Algo.SHA1, otp.algorithm, "Algorithm should match")

        // Generate an OTP and verify it's a 6-digit string
        val generatedOtp = otp.generateOTP(0)
        assertEquals(6, generatedOtp.length, "Generated OTP should have 6 digits")
    }

    @Test
    fun testParseHOTPUri() = runTest {
        val uri = "otpauth://hotp/Example%3Abob%40example.com?secret=$testSecret&issuer=Example&counter=42"
        val otp = OtpAuthURI.parse(uri)

        // Verify the OTP object
        assertTrue(otp is HOTP, "Parsed OTP should be a HOTP instance")
        assertEquals(testSecret, otp.secret, "Secret should match")
        assertEquals(6, otp.digits, "Digits should match")
        assertEquals(CryptoTools.Algo.SHA1, otp.algorithm, "Algorithm should match")

        // Generate an OTP and verify it's a 6-digit string
        val generatedOtp = otp.generateOTP(42)
        assertEquals(6, generatedOtp.length, "Generated OTP should have 6 digits")
    }

    @Test
    fun testBuilderPattern() {
        val uri = OtpAuthURI.Builder()
            .type(OtpAuthURI.Type.TOTP)
            .label("Example:charlie@example.com")
            .secret(testSecret)
            .issuer("Example")
            .algorithm(CryptoTools.Algo.SHA256)
            .digits(8)
            .period(60)
            .build()

        // Verify the URI format
        assertTrue(
            uri.startsWith("otpauth://totp/Example%3Acharlie%40example.com?"),
            "URI should start with correct prefix"
        )
        assertTrue(uri.contains("secret=$testSecret"), "URI should contain the secret")
        assertTrue(uri.contains("issuer=Example"), "URI should contain the issuer")
        assertTrue(uri.contains("algorithm=SHA256"), "URI should contain the algorithm")
        assertTrue(uri.contains("digits=8"), "URI should contain the digits")
        assertTrue(uri.contains("period=60"), "URI should contain the period")
    }

    @Test
    fun testBuilderWithHOTP() {
        val uri = OtpAuthURI.Builder()
            .type(OtpAuthURI.Type.HOTP)
            .label("Example:dave@example.com")
            .secret(testSecret)
            .issuer("Example")
            .counter(100)
            .build()

        // Verify the URI format
        assertTrue(
            uri.startsWith("otpauth://hotp/Example%3Adave%40example.com?"),
            "URI should start with correct prefix"
        )
        assertTrue(uri.contains("secret=$testSecret"), "URI should contain the secret")
        assertTrue(uri.contains("issuer=Example"), "URI should contain the issuer")
        assertTrue(uri.contains("counter=100"), "URI should contain the counter")
    }

    @Test
    fun testRoundTrip() = runTest {
        // Create a TOTP object with default timeInterval (30)
        val originalTotp = TOTP(
            digits = 8,
            algorithm = CryptoTools.Algo.SHA256,
            secret = testSecret
            // Using default timeInterval (30)
        )

        // Convert to URI
        val uri = OtpAuthURI.create(originalTotp, "Test:user@example.com", "Test")

        // Parse back to OTP
        val parsedOtp = OtpAuthURI.parse(uri)

        // Verify the parsed OTP matches the original
        assertTrue(parsedOtp is TOTP, "Parsed OTP should be a TOTP instance")
        assertEquals(testSecret, parsedOtp.secret, "Secret should match")
        assertEquals(8, parsedOtp.digits, "Digits should match")
        assertEquals(CryptoTools.Algo.SHA256, parsedOtp.algorithm, "Algorithm should match")

        // Generate OTPs with both objects and verify they match
        val timestamp = 1234567890L
        val originalOtp = originalTotp.generateOTP(timestamp)
        val parsedOtpValue = parsedOtp.generateOTP(timestamp)
        assertEquals(originalOtp, parsedOtpValue, "Generated OTPs should match")
    }
}
