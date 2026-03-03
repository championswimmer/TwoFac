package tech.arnav.twofac.qr

// Some scanner backends occasionally return "otpauth://" with spaces between characters
// (for example "o t p a u t h : //"). Keep this regex precompiled and reused across scans.
private val spacedOtpAuthRegex = Regex(
    pattern = "o\\s*t\\s*p\\s*a\\s*u\\s*t\\s*h\\s*:\\s*/\\s*/",
    options = setOf(RegexOption.IGNORE_CASE),
)

object QRCodeUtils {
    fun decodedPayloadCandidatesToQRCodeReadResult(
        primaryPayload: String?,
        rawBytes: ByteArray? = null,
    ): QRCodeReadResult {
        buildOtpAuthPayloadCandidates(primaryPayload, rawBytes).forEach { payload ->
            val mappedResult = decodedPayloadToQRCodeReadResult(payload)
            if (mappedResult is QRCodeReadResult.Success) {
                return mappedResult
            }
        }
        return QRCodeReadResult.DecodeFailure(INVALID_OTP_AUTH_URI_REASON)
    }

    fun payloadCandidateLengths(
        primaryPayload: String?,
        rawBytes: ByteArray? = null,
    ): List<Int> = buildOtpAuthPayloadCandidates(primaryPayload, rawBytes).map(String::length)
}

private fun buildOtpAuthPayloadCandidates(
    primaryPayload: String?,
    rawBytes: ByteArray?,
): List<String> {
    val candidates = linkedSetOf<String>()

    fun addCandidate(payload: String?) {
        val normalized = payload?.replace('\u0000', ' ')?.trim().orEmpty()
        if (normalized.isEmpty()) return

        candidates += normalized
        extractOtpAuthSubstring(normalized)?.let(candidates::add)
        if (spacedOtpAuthRegex.containsMatchIn(normalized)) {
            candidates += normalized.filterNot { it.isWhitespace() || it == '\u0000' }
        }
    }

    addCandidate(primaryPayload)
    rawBytes?.let {
        addCandidate(it.decodeToString())
        addCandidate(it.withoutNullBytes().decodeToString())
    }

    return candidates.toList()
}

private fun extractOtpAuthSubstring(payload: String): String? {
    val prefixIndex = payload.indexOf("otpauth://", ignoreCase = true)
    if (prefixIndex < 0) return null
    return payload.substring(prefixIndex).trim()
}

private fun ByteArray.withoutNullBytes(): ByteArray {
    val filteredBytes = ByteArray(size)
    var writeIndex = 0
    for (byte in this) {
        if (byte != 0.toByte()) {
            filteredBytes[writeIndex++] = byte
        }
    }
    return filteredBytes.copyOf(writeIndex)
}
