package tech.arnav.twofac.lib.otp

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
    override val secret: String
) : OTP {
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
        val counterBytes = ByteArray(8)
        for (i in 7 downTo 0) {
            counterBytes[7 - i] = ((counter shr (i * 8)) and 0xFF).toByte()
        }

        // Decode the Base32-encoded secret
        val secretBytes = Encoding.decodeBase32(secret)

        // Compute the HMAC using the CryptoTools
        val hmac = cryptoTools.hmacSha(
            algorithm,
            ByteString(secretBytes),
            ByteString(counterBytes)
        )

        // Dynamic truncation
        val offset = (hmac.get(hmac.size - 1) and 0x0F).toInt()
        val binary = ((hmac.get(offset).toInt() and 0x7F) shl 24) or
                ((hmac.get(offset + 1).toInt() and 0xFF) shl 16) or
                ((hmac.get(offset + 2).toInt() and 0xFF) shl 8) or
                (hmac.get(offset + 3).toInt() and 0xFF)

        // Generate the OTP
        val modulo = 10.0.pow(digits.toDouble()).toInt()
        val otp = binary % modulo

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
}
