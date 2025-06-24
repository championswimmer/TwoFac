package tech.arnav.twofac.lib.otp

import tech.arnav.twofac.lib.crypto.CryptoTools

/**
 * Common interface for OTP (One-Time Password) generation and validation.
 * Possible implementations include
 * - HOTP (HMAC-based One-Time Password) based on RFC 4226
 * - TOTP (Time-based One-Time Password) based on RFC 6238 (extension of HOTP)
 *
 */
interface OTP {

    val digits: Int // Number of digits in the OTP; default is 6
    val algorithm: CryptoTools.Algo // Algo used for HOTP; default is SHA1
    val secret: String // Base32-encoded secret key for generating the OTP

    /**
     * Generate a new OTP based on the current counter.
     * The counter is typically incremented for each OTP generation.
     *
     * @param counter The current counter value, which is used to generate the OTP.
     * @return The generated OTP is a string with [digits] length.
     */
    suspend fun generateOTP(counter: Long): String

    /**
     * Validate an OTP against the expected value for a given counter.
     *
     * @param otp The OTP to validate.
     * @param counter The counter value used to generate the expected OTP.
     * @return True if the OTP is valid for the given counter, false otherwise.
     */
    suspend fun validateOTP(otp: String, counter: Long): Boolean

}
