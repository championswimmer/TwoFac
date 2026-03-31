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
            timeInterval = 30,
            accountName = "alice@example.com",
            issuer = "Example"
        )

        val uri = OtpAuthURI.create(totp)

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
            secret = testSecret,
            accountName = "bob@example.com",
            issuer = "Example"
        )

        val uri = OtpAuthURI.create(hotp)

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
    fun testParseURLEncodedLabel() = runTest {
        val uri = "otpauth://totp/GitHub%3Auser%40example.com?secret=$testSecret&issuer=GitHub"
        val otp = OtpAuthURI.parse(uri)

        assertTrue(otp is TOTP, "Parsed OTP should be a TOTP instance")
        assertEquals("GitHub", otp.issuer, "Issuer should be URL-decoded")
        assertEquals("user@example.com", otp.accountName, "Account name should be URL-decoded")
    }

    @Test
    fun testRoundTripWithSpecialChars() = runTest {
        val originalTotp = TOTP(
            digits = 6,
            algorithm = CryptoTools.Algo.SHA1,
            secret = testSecret,
            timeInterval = 30,
            accountName = "user@example.com",
            issuer = "GitHub"
        )

        val uri = OtpAuthURI.create(originalTotp)
        val parsedOtp = OtpAuthURI.parse(uri)

        assertTrue(parsedOtp is TOTP)
        assertEquals("GitHub", parsedOtp.issuer, "Issuer should survive round-trip without encoding")
        assertEquals("user@example.com", parsedOtp.accountName, "Account name should survive round-trip without encoding")
    }

    @Test
    fun testRoundTrip() = runTest {
        // Create a TOTP object with default timeInterval (30)
        val originalTotp = TOTP(
            digits = 8,
            algorithm = CryptoTools.Algo.SHA256,
            secret = testSecret,
            accountName = "user@example.com",
            issuer = "Test"
            // Using default timeInterval (30)
        )

        // Convert to URI
        val uri = OtpAuthURI.create(originalTotp)

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

    // ── Phase 0 regression tests ──────────────────────────────────────────────

    @Test
    fun testHOTPCounterPreservedInRoundTrip() = runTest {
        // RFC 4226 §7.2: counter in an otpauth://hotp/ URI sets the initial client-side
        // counter state. Parsing an HOTP URI must preserve that counter.
        val uri = "otpauth://hotp/Example%3Abob%40example.com?secret=$testSecret&issuer=Example&counter=42"
        val otp = OtpAuthURI.parse(uri)

        assertTrue(otp is HOTP, "Parsed OTP should be HOTP")
        assertEquals(42L, otp.initialCounter, "initialCounter must survive parse")
    }

    @Test
    fun testHOTPCounterRoundTripViaCreate() = runTest {
        // Creating a URI from an HOTP with initialCounter=7 must embed counter=7.
        val hotp = HOTP(
            secret = testSecret,
            accountName = "carol@example.com",
            issuer = "Example",
            initialCounter = 7L,
        )
        val uri = OtpAuthURI.create(hotp)
        assertTrue(uri.contains("counter=7"), "URI must contain the provisioned counter")

        val parsed = OtpAuthURI.parse(uri) as HOTP
        assertEquals(7L, parsed.initialCounter, "initialCounter must survive create→parse round-trip")
    }

    @Test
    fun testTOTPDefaultPeriodUsesConstant() {
        // The builder must use DEFAULT_PERIOD, not a magic 30L literal.
        // When period == DEFAULT_PERIOD the parameter is omitted from the URI.
        val uri = OtpAuthURI.Builder()
            .type(OtpAuthURI.Type.TOTP)
            .label("Example:user@example.com")
            .secret(testSecret)
            .period(OtpAuthURI.DEFAULT_PERIOD)
            .build()
        assertTrue(!uri.contains("period="), "Default period must be omitted from URI")

        // Explicitly setting a non-default period must appear in the URI.
        val uri60 = OtpAuthURI.Builder()
            .type(OtpAuthURI.Type.TOTP)
            .label("Example:user@example.com")
            .secret(testSecret)
            .period(60L)
            .build()
        assertTrue(uri60.contains("period=60"), "Non-default period must appear in URI")
    }
}
