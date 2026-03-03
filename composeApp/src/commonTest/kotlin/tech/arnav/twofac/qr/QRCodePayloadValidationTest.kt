package tech.arnav.twofac.qr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class QRCodePayloadValidationTest {
    private val validOtpAuthUri =
        "otpauth://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example"

    @Test
    fun normalizeQrDecodedPayloadTrimsWhitespace() {
        val payload = "  \n$validOtpAuthUri\t"

        val normalized = normalizeQrDecodedPayload(payload)

        assertEquals(validOtpAuthUri, normalized)
    }

    @Test
    fun validateOtpAuthUriPayloadReturnsCanonicalUri() {
        val payload =
            " OTPAUTH://totp/Example:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=Example "

        val validated = validateOtpAuthUriPayload(payload)

        assertEquals(validOtpAuthUri, validated)
    }

    @Test
    fun validateOtpAuthUriPayloadReturnsNullForInvalidPayload() {
        assertNull(validateOtpAuthUriPayload("https://example.com"))
        assertNull(validateOtpAuthUriPayload("otpauth://totp/Example:alice@example.com?issuer=Example"))
        assertNull(validateOtpAuthUriPayload("   "))
    }

    @Test
    fun decodedPayloadToQRCodeReadResultReturnsDecodeFailureForInvalidPayload() {
        val result = decodedPayloadToQRCodeReadResult("not-a-valid-qr-payload")

        val decodeFailure = assertIs<QRCodeReadResult.DecodeFailure>(result)
        assertEquals(INVALID_OTP_AUTH_URI_REASON, decodeFailure.reason)
    }

    @Test
    fun decodedPayloadToQRCodeReadResultReturnsSuccessForValidPayload() {
        val result = decodedPayloadToQRCodeReadResult(validOtpAuthUri)

        val success = assertIs<QRCodeReadResult.Success>(result)
        assertEquals(validOtpAuthUri, success.otpAuthUri)
    }
}
