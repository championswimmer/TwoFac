package tech.arnav.twofac.qr

import tech.arnav.twofac.lib.uri.OtpAuthURI

private const val OTP_AUTH_URI_SCHEME = "otpauth://"
const val INVALID_OTP_AUTH_URI_REASON = "Invalid or unsupported otpauth URI payload"

fun normalizeQrDecodedPayload(payload: String): String = payload.trim()

fun validateOtpAuthUriPayload(payload: String): String? {
    val normalizedPayload = normalizeQrDecodedPayload(payload)
    if (normalizedPayload.isEmpty()) return null

    val canonicalPayload = canonicalizeOtpAuthScheme(normalizedPayload)
    if (!canonicalPayload.startsWith(OTP_AUTH_URI_SCHEME)) return null

    return runCatching {
        OtpAuthURI.parse(canonicalPayload)
        canonicalPayload
    }.getOrNull()
}

fun decodedPayloadToQRCodeReadResult(payload: String): QRCodeReadResult =
    validateOtpAuthUriPayload(payload)
        ?.let(QRCodeReadResult::Success)
        ?: QRCodeReadResult.DecodeFailure(INVALID_OTP_AUTH_URI_REASON)

private fun canonicalizeOtpAuthScheme(payload: String): String {
    if (!payload.startsWith(OTP_AUTH_URI_SCHEME, ignoreCase = true)) {
        return payload
    }
    return OTP_AUTH_URI_SCHEME + payload.substring(OTP_AUTH_URI_SCHEME.length)
}
