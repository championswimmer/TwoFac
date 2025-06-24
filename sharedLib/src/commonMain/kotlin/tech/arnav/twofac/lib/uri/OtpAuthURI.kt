package tech.arnav.twofac.lib.uri

import tech.arnav.twofac.lib.crypto.CryptoTools
import tech.arnav.twofac.lib.otp.HOTP
import tech.arnav.twofac.lib.otp.OTP
import tech.arnav.twofac.lib.otp.TOTP
import kotlin.experimental.ExperimentalNativeApi
import kotlin.js.ExperimentalJsStatic
import kotlin.js.JsStatic
import kotlin.jvm.JvmStatic
import kotlin.native.CName

/**
 * OtpAuthURI class for creating and parsing otpauth URIs.
 * Based on the specification: https://datatracker.ietf.org/doc/draft-linuxgemini-otpauth-uri/02/
 *
 * The otpauth URI format is:
 * otpauth://TYPE/LABEL?PARAMETERS
 *
 * Where:
 * - TYPE is either "totp" or "hotp"
 * - LABEL is a string that identifies the account (e.g., "Example:alice@example.com")
 * - PARAMETERS are key-value pairs (e.g., "secret=JBSWY3DPEHPK3PXP&issuer=Example")
 */
@OptIn(ExperimentalJsStatic::class)
object OtpAuthURI {

    const val DEFAULT_DIGITS = 6
    const val DEFAULT_PERIOD = 30 // Default period for TOTP in seconds

    /**
     * OTP type (TOTP or HOTP)
     */
    enum class Type {
        TOTP, HOTP;

        override fun toString(): String {
            return name.lowercase()
        }
    }

    /**
     * Create an otpauth URI from an OTP object.
     *
     * @param otp The OTP object (either TOTP or HOTP)
     * @param label The label for the account (e.g., "Example:alice@example.com")
     * @param issuer The issuer of the OTP (e.g., "Example")
     * @return The otpauth URI as a string
     */
    @JvmStatic
    @JsStatic
    @OptIn(ExperimentalNativeApi::class)
    @CName("create_otp_auth_uri")
    fun create(otp: OTP, label: String, issuer: String? = null): String {
        val builder = Builder()
            .type(if (otp is TOTP) Type.TOTP else Type.HOTP)
            .label(label)
            .secret(otp.secret)
            .digits(otp.digits)
            .algorithm(otp.algorithm)

        if (issuer != null) {
            builder.issuer(issuer)
        }

        if (otp is HOTP) {
            // For HOTP, we need a counter
            builder.counter(0) // Default to 0 if not specified
        }

        if (otp is TOTP) {
            // For TOTP, we can set period (time interval)
            builder.period(otp.timeInterval) // Default to 30 if not specified
        }

        return builder.build()
    }

    /**
     * Parse an otpauth URI and create the corresponding OTP object.
     *
     * @param uri The otpauth URI to parse
     * @return The OTP object (either TOTP or HOTP)
     * @throws IllegalArgumentException if the URI is invalid
     */
    @JvmStatic
    @JsStatic
    @OptIn(ExperimentalNativeApi::class)
    @CName("parse_otp_auth_uri")
    fun parse(uri: String): OTP {
        // Check if the URI starts with "otpauth://"
        require(uri.startsWith("otpauth://")) { "Invalid otpauth URI: $uri" }

        // Extract the type (totp or hotp)
        val typeEndIndex = uri.indexOf("/", 10)
        require(typeEndIndex != -1) { "Invalid otpauth URI: $uri" }
        val typeStr = uri.substring(10, typeEndIndex)
        val type = when (typeStr.lowercase()) {
            "totp" -> Type.TOTP
            "hotp" -> Type.HOTP
            else -> throw IllegalArgumentException("Invalid OTP type: $typeStr")
        }

        // Extract the label
        val labelEndIndex = uri.indexOf("?", typeEndIndex)
        require(labelEndIndex != -1) { "Invalid otpauth URI: $uri" }
        val label = uri.substring(typeEndIndex + 1, labelEndIndex)

        // Parse the parameters
        val paramsStr = uri.substring(labelEndIndex + 1)
        val params = paramsStr.split("&").associate {
            val parts = it.split("=", limit = 2)
            require(parts.size == 2) { "Invalid parameter: $it" }
            parts[0] to parts[1]
        }

        // Extract required parameters
        val secret = params["secret"] ?: throw IllegalArgumentException("Missing required parameter: secret")

        // Extract optional parameters with defaults
        val digits = params["digits"]?.toIntOrNull() ?: DEFAULT_DIGITS
        val algorithm = when (params["algorithm"]?.uppercase()) {
            "SHA1" -> CryptoTools.Algo.SHA1
            "SHA256" -> CryptoTools.Algo.SHA256
            "SHA512" -> CryptoTools.Algo.SHA512
            null -> CryptoTools.Algo.SHA1 // Default
            else -> throw IllegalArgumentException("Invalid algorithm: ${params["algorithm"]}")
        }

        // Create the appropriate OTP object
        return when (type) {
            Type.TOTP -> {
                val period = params["period"]?.toIntOrNull() ?: DEFAULT_PERIOD
                TOTP(
                    digits = digits,
                    algorithm = algorithm,
                    secret = secret,
                    timeInterval = period
                )
            }

            Type.HOTP -> {
                val counter = params["counter"]?.toLongOrNull() ?: 0
                HOTP(
                    digits = digits,
                    algorithm = algorithm,
                    secret = secret
                )
                // Note: The counter is not used in the HOTP constructor, but it's a required parameter in the URI
            }
        }
    }

    /**
     * Builder class for incrementally constructing an otpauth URI.
     */
    class Builder {
        private var type: Type? = null
        private var label: String? = null
        private var secret: String? = null
        private var issuer: String? = null
        private var algorithm: CryptoTools.Algo = CryptoTools.Algo.SHA1
        private var digits: Int = DEFAULT_DIGITS
        private var counter: Long = 0L // Default counter for HOTP
        private var period: Int = DEFAULT_PERIOD

        /**
         * Set the OTP type (TOTP or HOTP).
         */
        fun type(type: Type): Builder {
            this.type = type
            return this
        }

        /**
         * Set the label for the account.
         */
        fun label(label: String): Builder {
            this.label = label
            return this
        }

        /**
         * Set the secret key (Base32-encoded).
         */
        fun secret(secret: String): Builder {
            this.secret = secret
            return this
        }

        /**
         * Set the issuer of the OTP.
         */
        fun issuer(issuer: String): Builder {
            this.issuer = issuer
            return this
        }

        /**
         * Set the algorithm used for HMAC.
         */
        fun algorithm(algorithm: CryptoTools.Algo): Builder {
            this.algorithm = algorithm
            return this
        }

        /**
         * Set the number of digits in the OTP.
         */
        fun digits(digits: Int): Builder {
            this.digits = digits
            return this
        }

        /**
         * Set the counter for HOTP.
         */
        fun counter(counter: Long): Builder {
            this.counter = counter
            return this
        }

        /**
         * Set the period (time interval) for TOTP.
         */
        fun period(period: Int): Builder {
            this.period = period
            return this
        }

        /**
         * Build the otpauth URI.
         *
         * @return The otpauth URI as a string
         * @throws IllegalArgumentException if required parameters are missing
         */
        fun build(): String {
            // Validate required parameters
            val type = this.type ?: throw IllegalArgumentException("Type is required")
            val label = this.label ?: throw IllegalArgumentException("Label is required")
            val secret = this.secret ?: throw IllegalArgumentException("Secret is required")

            // Build the URI
            val uri = StringBuilder("otpauth://")
            uri.append(type.toString())
            uri.append("/")
            uri.append(encodeURIComponent(label))
            uri.append("?")

            // Add parameters
            val params = mutableListOf<String>()
            params.add("secret=$secret")

            if (issuer != null) {
                params.add("issuer=${encodeURIComponent(issuer!!)}")
            }

            if (algorithm != CryptoTools.Algo.SHA1) {
                params.add("algorithm=${algorithm.name}")
            }

            if (digits != DEFAULT_DIGITS) {
                params.add("digits=$digits")
            }

            if (type == Type.HOTP) {
                params.add("counter=$counter")
            }

            if (type == Type.TOTP && period != 30) {
                params.add("period=$period")
            }

            uri.append(params.joinToString("&"))

            return uri.toString()
        }

        /**
         * Encode a string for use in a URI.
         */
        private fun encodeURIComponent(s: String): String {
            return s.replace(" ", "%20")
                .replace(":", "%3A")
                .replace("/", "%2F")
                .replace("?", "%3F")
                .replace("&", "%26")
                .replace("=", "%3D")
                .replace("+", "%2B")
                .replace("#", "%23")
                .replace("@", "%40")
        }
    }
}
