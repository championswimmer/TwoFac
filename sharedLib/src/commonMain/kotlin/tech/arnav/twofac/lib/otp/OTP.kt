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

    /**
     * The number of digits in the generated OTP. Default is 6.
     */
    val digits: Int

    /**
     * The algorithm used with HMAC for generating the OTP.
     * Default is SHA1, but can be changed to SHA256 or SHA512 if needed.
     */
    val algorithm: CryptoTools.Algo

    /**
     * The secret key used for generating the OTP. Base32-encoded string.
     */
    val secret: String

    /**
     * The account name associated with the OTP.
     * This is typically the username or email for which the OTP is generated.
     */
    val accountName: String

    /**
     * The issuer of the OTP, which is usually the service or application that provides the OTP.
     * This can be null if not specified.
     */
    val issuer: String?


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
