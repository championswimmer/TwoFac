package tech.arnav.twofac.lib.otp

import tech.arnav.twofac.lib.crypto.CryptoTools

/**
 * Implementation of Time-based One-Time Password (TOTP) as defined in RFC 6238.
 * TOTP uses HOTP with a counter derived from the current time.
 */
class TOTP(
    override val digits: Int = 6,
    override val algorithm: CryptoTools.Algo = CryptoTools.Algo.SHA1,
    override val secret: String,
    private val baseTime: Long = 0,
    private val timeInterval: Int = 30
) : OTP {

    // Use HOTP internally for the actual OTP generation
    private val hotp = HOTP(digits, algorithm, secret)

    /**
     * Converts a timestamp to a counter value.
     *
     * @param currentTime The current Unix timestamp in seconds.
     * @return The counter value derived from the time.
     */
    private fun timeToCounter(currentTime: Long): Long {
        return (currentTime - baseTime) / timeInterval
    }


    /**
     * Generate a new OTP based on the current time.
     * This is a convenience method that converts time to a counter value.
     *
     * @param currentTime The current Unix timestamp in seconds.
     * @return The generated OTP is a string with [digits] length.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override suspend fun generateOTP(currentTime: Long): String {
        val counter = timeToCounter(currentTime)
        return hotp.generateOTP(counter)
    }

    /**
     * Validate an OTP against the expected value for the current time.
     * This method checks the OTP for the current time window as well as
     * the previous (-1) and next (+1) time windows.
     *
     * @param otp The OTP to validate.
     * @param currentTime The current Unix timestamp in seconds.
     * @return True if the OTP is valid for any of the time windows, false otherwise.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override suspend fun validateOTP(otp: String, currentTime: Long): Boolean {
        val counter = timeToCounter(currentTime)

        // Check previous, current, and next time windows
        return hotp.validateOTP(otp, counter - 1) ||
                hotp.validateOTP(otp, counter) ||
                hotp.validateOTP(otp, counter + 1)
    }
}
