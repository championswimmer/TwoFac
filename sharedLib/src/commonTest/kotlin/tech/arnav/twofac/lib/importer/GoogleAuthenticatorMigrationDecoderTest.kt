package tech.arnav.twofac.lib.importer

import kotlinx.coroutines.test.runTest
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.importer.google.GoogleAuthenticatorMigrationDecoder
import tech.arnav.twofac.lib.otp.HOTP
import tech.arnav.twofac.lib.otp.TOTP
import tech.arnav.twofac.lib.storage.MemoryStorage
import tech.arnav.twofac.lib.uri.OtpAuthURI
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GoogleAuthenticatorMigrationDecoderTest {
    // Public interoperability example from dim13/otpauth, not produced by our serializer.
    private val publicFixture = "otpauth-migration://offline?data=CjEKCkhlbGxvId6tvu8SGEV4YW1wbGU6YWxpY2VAZ29vZ2xlLmNvbRoHRXhhbXBsZTAC"

    @Test
    fun decodesIndependentFixtureWithProto3Defaults() {
        val result = GoogleAuthenticatorMigrationDecoder.decode(publicFixture)
        assertEquals(1, result.batchSize)
        val otp = assertIs<TOTP>(OtpAuthURI.parse(result.otpAuthUris.single()))
        assertEquals("JBSWY3DPEHPK3PXP", otp.secret)
        assertEquals("alice@google.com", otp.accountName)
        assertEquals("Example", otp.issuer)
        assertEquals(CryptoTools.Algo.SHA1, otp.algorithm)
        assertEquals(6, otp.digits)
        assertEquals(30L, otp.timeInterval)
    }

    @Test
    fun percentEncodedBase64AndLiteralPlusArePreserved() {
        val raw = "CjsKFBHMQnKu/odWlB/zUy+dfiRIaHj0EhhFeGFtcGxlOmFsaWNlQGdvb2dsZS5jb20aB0V4YW1wbGUwAg=="
        val escaped = raw.replace("+", "%2b").replace("/", "%2f").replace("=", "%3d")
        val expected = GoogleAuthenticatorMigrationDecoder.decode("otpauth-migration://offline?data=$raw")
        assertEquals(expected, GoogleAuthenticatorMigrationDecoder.decode("otpauth-migration://offline?data=$escaped"))
        assertEquals(expected, GoogleAuthenticatorMigrationDecoder.decode("otpauth-migration://offline?data=${raw.replace('+', ' ')}"))
    }

    @Test
    fun preservesMultipleRecordsAlgorithmsDigitsAndHotpCounter() = runTest {
        val bytes = record(algorithm = 2, digits = 2, type = 1, counter = 42) +
            record(name = "工程:100% user", issuer = "工程", algorithm = 3)
        val result = GoogleAuthenticatorMigrationDecoder.decode(uri(bytes + number(2, 1) + number(3, 2) + number(4, 1) + number(5, 99)))
        assertEquals(2, result.otpAuthUris.size)
        assertEquals(2, result.batchSize)
        assertEquals(1, result.batchIndex)
        assertEquals(99, result.batchId)
        val hotp = assertIs<HOTP>(OtpAuthURI.parse(result.otpAuthUris.first()))
        assertEquals(CryptoTools.Algo.SHA256, hotp.algorithm)
        assertEquals(8, hotp.digits)
        assertEquals(42L, hotp.initialCounter)
        val totp = assertIs<TOTP>(OtpAuthURI.parse(result.otpAuthUris.last()))
        assertEquals("100% user", totp.accountName)
        assertEquals("工程", totp.issuer)
        assertEquals(CryptoTools.Algo.SHA512, totp.algorithm)
    }

    @Test
    fun acceptsFutureInformationalVersionAndProto3DefaultBatchFields() {
        val payload = record() + number(2, 2)
        val decoded = GoogleAuthenticatorMigrationDecoder.decode(uri(payload))
        assertEquals(2, decoded.version)
        assertEquals(1, decoded.batchSize)
        assertEquals(0, decoded.batchIndex)
    }

    @Test
    fun acceptsUrlSafeBase64Alphabet() {
        // Public fixture contains neither '-' nor '_'; synthesize URL-safe alphabet chars
        // in otherwise ignorable protobuf field bytes to exercise the fallback.
        val bytes = record() + bytes(99, byteArrayOf(0xFB.toByte(), 0xFF.toByte()))
        val base64Url = Base64.UrlSafe.encode(bytes).trimEnd('=')
        val decoded = GoogleAuthenticatorMigrationDecoder.decode("otpauth-migration://offline?data=$base64Url")
        assertEquals(1, decoded.otpAuthUris.size)
    }

    @Test
    fun derivesIssuerOrPreservesIssuerlessAccount() {
        val withPrefix = GoogleAuthenticatorMigrationDecoder.decode(uri(record(issuer = "")))
        assertEquals("Example", OtpAuthURI.parse(withPrefix.otpAuthUris.single()).issuer)
        val withoutPrefix = GoogleAuthenticatorMigrationDecoder.decode(uri(record(name = "alice", issuer = "")))
        assertEquals(null, OtpAuthURI.parse(withoutPrefix.otpAuthUris.single()).issuer)
    }

    @Test
    fun importsCanonicalUrisAndMatchesRfcOtp() = runTest {
        val batch = GoogleAuthenticatorMigrationDecoder.decode(uri(record()))
        val otp = OtpAuthURI.parse(batch.otpAuthUris.single())
        assertEquals("287082", otp.generateOTP(59)) // RFC 6238 SHA1 vector, six digits
        val lib = TwoFacLib.initialise(MemoryStorage(), "123456")
        lib.unlock("123456")
        val result = lib.addAccounts(batch.otpAuthUris)
        assertEquals(listOf(0), result.importedIndices)
        assertTrue(result.failedIndices.isEmpty())
        assertEquals("alice", lib.getAllAccounts().single().accountLabel)
    }

    @Test
    fun rejectsMalformedAndUnsupportedInputsWithoutLeakingData() {
        val valid = uri(record())
        val inputs = listOf(
            "https://example.com", valid.replace("offline", "online"),
            "otpauth-migration://offline?data=", "$valid&data=abc", "$valid#fragment",
            "otpauth-migration://offline?data=%ZZ", "otpauth-migration://offline?data=%",
            "otpauth-migration://offline?data=%%%", uri(byteArrayOf(10, 127)), uri(byteArrayOf()),
            uri(record(secret = byteArrayOf())), uri(record(name = "")),
            uri(record(algorithm = 4)), uri(record(algorithm = 99)),
            uri(record(digits = 3)), uri(record(type = 3)), uri(record(counter = -1)),
            uri(record() + number(2, -1)), uri(record() + number(3, 0) + number(4, 1)),
            uri(record() + number(3, 2) + number(4, 2)),
        )
        inputs.forEach { input ->
            val error = assertFailsWith<IllegalArgumentException> { GoogleAuthenticatorMigrationDecoder.decode(input) }
            assertEquals("Invalid or unsupported Google Authenticator export", error.message)
            assertEquals(null, error.cause)
            assertFalse(error.message.orEmpty().contains("1234567890"))
        }
    }

    @Test
    fun ignoresUnknownProtobufFields() {
        val base = record()
        assertEquals(GoogleAuthenticatorMigrationDecoder.decode(uri(base)),
            GoogleAuthenticatorMigrationDecoder.decode(uri(base + text(99, "future"))))
    }

    // Independent minimal wire writer avoids testing ProtoBuf against itself.
    private fun uri(payload: ByteArray) = "otpauth-migration://offline?data=${Base64.encode(payload)}"
    private fun record(
        name: String = "Example:alice", issuer: String = "Example", algorithm: Int = 0,
        digits: Int = 0, type: Int = 0, counter: Long = 0,
        secret: ByteArray = "12345678901234567890".encodeToByteArray(),
    ): ByteArray = bytes(1, bytes(1, secret) + text(2, name) + text(3, issuer) +
        number(4, algorithm.toLong()) + number(5, digits.toLong()) + number(6, type.toLong()) + number(7, counter))

    private fun text(field: Int, value: String) = bytes(field, value.encodeToByteArray())
    private fun bytes(field: Int, value: ByteArray) = varint((field * 8 + 2).toLong()) + varint(value.size.toLong()) + value
    private fun number(field: Int, value: Long) = varint((field * 8).toLong()) + varint(value)
    private fun varint(value: Long): ByteArray {
        var remaining = value
        val result = mutableListOf<Byte>()
        do {
            val low = (remaining and 127).toInt()
            remaining = remaining ushr 7
            result += (low or if (remaining == 0L) 0 else 128).toByte()
        } while (remaining != 0L)
        return result.toByteArray()
    }
}
