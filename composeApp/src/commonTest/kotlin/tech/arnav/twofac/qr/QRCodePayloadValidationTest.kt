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
        assertEquals(listOf(validOtpAuthUri), success.otpAuthUris)
    }

    @Test
    fun decodedPayloadCandidatesToQRCodeReadResultExtractsEmbeddedOtpAuthUri() {
        val result = QRCodeUtils.decodedPayloadCandidatesToQRCodeReadResult(
            primaryPayload = "Scanned payload: $validOtpAuthUri",
        )

        val success = assertIs<QRCodeReadResult.Success>(result)
        assertEquals(listOf(validOtpAuthUri), success.otpAuthUris)
    }

    @Test
    fun decodedPayloadCandidatesToQRCodeReadResultRecoversFromNullInterleavedRawBytes() {
        val result = QRCodeUtils.decodedPayloadCandidatesToQRCodeReadResult(
            primaryPayload = null,
            rawBytes = validOtpAuthUri.toNullInterleavedBytes(),
        )

        val success = assertIs<QRCodeReadResult.Success>(result)
        assertEquals(listOf(validOtpAuthUri), success.otpAuthUris)
    }

    @Test
    fun migrationPayloadMapsToCanonicalUrisAndMetadata() {
        val payload = "otpauth-migration://offline?data=CjEKCkhlbGxvId6tvu8SGEV4YW1wbGU6YWxpY2VAZ29vZ2xlLmNvbRoHRXhhbXBsZTAC"
        val result = assertIs<QRCodeReadResult.Success>(decodedPayloadToQRCodeReadResult(payload))
        assertEquals(1, result.otpAuthUris.size)
        assertEquals(1, result.migrationBatch?.batchSize)
        assertIs<QRCodeReadResult.Success>(QRCodeUtils.decodedPayloadCandidatesToQRCodeReadResult("QR: $payload"))
        assertIs<QRCodeReadResult.Success>(QRCodeUtils.decodedPayloadCandidatesToQRCodeReadResult(null, payload.toNullInterleavedBytes()))
    }

    @Test
    fun malformedMigrationFailsWithoutExposingPayload() {
        val result = assertIs<QRCodeReadResult.DecodeFailure>(decodedPayloadToQRCodeReadResult(
            "otpauth-migration://offline?data=PRIVATE_SECRET",
        ))
        assertEquals(INVALID_OTP_AUTH_URI_REASON, result.reason)
    }

    private fun String.toNullInterleavedBytes(): ByteArray {
        val interleavedBytes = ByteArray(length * 2)
        forEachIndexed { index, char ->
            interleavedBytes[index * 2] = char.code.toByte()
            interleavedBytes[index * 2 + 1] = 0
        }
        return interleavedBytes
    }
}
