package tech.arnav.twofac.lib.otp

import dev.whyoleg.cryptography.BinarySize.Companion.bytes
import dev.whyoleg.cryptography.CryptographyProvider
import kotlinx.io.bytestring.ByteString
import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.crypto.DefaultCryptoTools
import tech.arnav.twofac.lib.crypto.Encoding
import kotlin.experimental.and
import kotlin.math.pow

class HOTP(
    override val digits: Int = 6,
    override val algorithm: CryptoTools.Algo = CryptoTools.Algo.SHA1,
    override val secret: String,
) : OTP {
    companion object {
        private const val MSB_MASK = 0x7F // most significant bit mask 01111111
        private const val BYTE_MASK = 0xFF // byte mask 11111111
    }
    private val cryptoTools = DefaultCryptoTools(CryptographyProvider.Default)

    /**
     * Generate a new OTP based on the current counter.
     * The counter is typically incremented for each OTP generation.
     *
     * @param counter The current counter value, which is used to generate the OTP.
     * @return The generated OTP is a string with [digits] length.
     */
    override suspend fun generateOTP(counter: Long): String {
        // Convert the counter to a byte array (8 bytes, big-endian)
        val counterBytes = ByteArray(8) { i ->
            ((counter shr ((7 - i) * 8)) and 0xFF).toByte()
        }

        // Decode the Base32-encoded secret
        val secretBytes = Encoding.decodeBase32(secret)

        // Compute the HMAC using the CryptoTools
        val hmac = cryptoTools.hmacSha(
            algorithm,
            ByteString(secretBytes),
            ByteString(counterBytes)
        )

        // Dynamic truncation (get 4 bytes from the HMAC result)
        val fourBytes = dynamicTruncate(hmac)

        // Generate the OTP by modulo with 10^digits
        val otp = fourBytes % 10.0.pow(digits.toDouble()).toInt()

        // Pad with leading zeros if necessary
        return otp.toString().padStart(digits, '0')
    }

    /**
     * Validate an OTP against the expected value for a given counter.
     *
     * @param otp The OTP to validate.
     * @param counter The counter value used to generate the expected OTP.
     * @return True if the OTP is valid for the given counter, false otherwise.
     */
    override suspend fun validateOTP(otp: String, counter: Long): Boolean {
        val expectedOTP = generateOTP(counter)
        return otp == expectedOTP
    }

    /**
     * Performs dynamic truncation as per RFC 4226.
     * Extracts a 4-byte dynamic binary code from the HMAC result.
     * Works with different hash algorithms (SHA1, SHA256, SHA512) which produce
     * different-sized outputs.
     *
     * @param hmac The HMAC result to truncate (can be of any hash algorithm)
     * @return The truncated 31-bit integer
     */
    internal fun dynamicTruncate(hmac: ByteString): Int {
        // The last byte's 4 lowest bits determine the offset
        // This works for all hash algorithms as we only use the last byte
        val offset = (hmac.get(hmac.size - 1) and 0x0F).toInt()

        // Extract 4 bytes starting at the offset position
        // For all hash algorithms; we only need 4 bytes for the OTP calculation
        return ((hmac.get(offset).toInt() and MSB_MASK) shl 3.bytes.inBits) or
                ((hmac.get(offset + 1).toInt() and BYTE_MASK) shl 2.bytes.inBits) or
                ((hmac.get(offset + 2).toInt() and BYTE_MASK) shl 1.bytes.inBits) or
                (hmac.get(offset + 3).toInt() and BYTE_MASK)
    }
}
