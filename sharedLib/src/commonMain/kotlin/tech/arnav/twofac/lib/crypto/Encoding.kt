package tech.arnav.twofac.lib.crypto

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.ByteStringBuilder
import kotlin.experimental.ExperimentalNativeApi
import kotlin.js.ExperimentalJsStatic
import kotlin.js.JsStatic
import kotlin.jvm.JvmStatic
import kotlin.native.CName


@OptIn(ExperimentalJsStatic::class, ExperimentalNativeApi::class)
object Encoding {
    // Base32 alphabet (RFC 4648)
    const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /**
     * Decode a Base32-encoded string into a byte array.
     *
     * @param base32 The Base32-encoded string to decode.
     * @return The decoded byte array.
     */
    @JvmStatic
    @JsStatic
    @CName("decode_base32")
    fun decodeBase32(base32: String): ByteArray {
        // Remove padding and convert to uppercase
        val cleanInput = base32.replace("=", "").uppercase()

        // Calculate the output length
        val outputLength = (cleanInput.length * 5) / 8
        val result = ByteArray(outputLength)

        var buffer = 0
        var bitsLeft = 0
        var outputIndex = 0

        for (c in cleanInput) {
            val value = ALPHABET.indexOf(c)
            if (value < 0) continue // Skip invalid characters

            // Add the 5 bits to the buffer
            buffer = (buffer shl 5) or value
            bitsLeft += 5

            // If we have at least 8 bits, write a byte
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                result[outputIndex++] = (buffer shr bitsLeft).toByte()
            }
        }

        return result
    }

    /**
     * Encode a byte array into a Base32-encoded string.
     *
     * @param bytes The byte array to encode.
     * @return The Base32-encoded string.
     */
    fun encodeBase32(bytes: ByteArray): String {
        val output = StringBuilder()
        var buffer = 0
        var bitsLeft = 0

        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bitsLeft += 8

            while (bitsLeft >= 5) {
                bitsLeft -= 5
                val index = (buffer shr bitsLeft) and 0x1F
                output.append(ALPHABET[index])
            }
        }

        // Handle remaining bits
        if (bitsLeft > 0) {
            buffer = buffer shl (5 - bitsLeft)
            output.append(ALPHABET[buffer and 0x1F])
        }

        // Add padding if necessary
        while (output.length % 8 != 0) {
            output.append('=')
        }

        return output.toString()
    }

    /**
     * Decode an ASCII string into a byte array.
     *
     * @param ascii The ASCII string to decode.
     * @return The decoded byte array.
     */
    @JvmStatic
    @JsStatic
    @CName("decode_ascii")
    fun decodeAscii(ascii: String): ByteArray {
        // Convert ASCII string to byte array
        return ByteArray(ascii.length) { i ->
            ascii[i].code.toByte()
        }
    }

    /**
     * Encode a string for use in a URI.
     */
    internal fun encodeURIComponent(s: String): String {
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

    /**
     * Decode a URI-encoded string.
     */
    internal fun decodeURIComponent(s: String): String {
        return s.replace("%20", " ")
            .replace("%3A", ":")
            .replace("%2F", "/")
            .replace("%3F", "?")
            .replace("%26", "&")
            .replace("%3D", "=")
            .replace("%2B", "+")
            .replace("%23", "#")
            .replace("%40", "@")
    }

    /**
     * Convert a hexadecimal string to a ByteString.
     *
     * @param this The hexadecimal string to convert.
     * @return The resulting ByteString.
     * @throws NumberFormatException If the string is not a valid hexadecimal representation.
     */
    @Throws(NumberFormatException::class)
    internal fun String.toByteString(): ByteString {
        require(length % 2 == 0) { "Hex string must have an even length" }
        return ByteStringBuilder().apply {
            chunked(2).forEach {
                append(it.toInt(16).toByte())
            }
        }.toByteString()
    }

    /**
     * Convert a ByteString to a hexadecimal string.
     *
     * @return The hexadecimal representation of the ByteString.
     */
    internal fun ByteString.toHexString(): String {
        return this.toByteArray().joinToString("") { byte ->
            byte.toUByte().toString(16).padStart(2, '0')
        }
    }

    /**
     * Wrapper function for testing: Convert a hexadecimal string to a ByteString.
     *
     * @param hexString The hexadecimal string to convert.
     * @return The resulting ByteString.
     * @throws NumberFormatException If the string is not a valid hexadecimal representation.
     */
    @JvmStatic
    @JsStatic
    @CName("hex_to_bytestring")
    @Throws(NumberFormatException::class)
    fun hexToByteString(hexString: String): ByteString {
        return hexString.toByteString()
    }

    /**
     * Wrapper function for testing: Convert a ByteString to a hexadecimal string.
     *
     * @param byteString The ByteString to convert.
     * @return The hexadecimal representation of the ByteString.
     */
    @JvmStatic
    @JsStatic
    @CName("bytestring_to_hex")
    fun byteStringToHex(byteString: ByteString): String {
        return byteString.toHexString()
    }

}
