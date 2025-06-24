package tech.arnav.twofac.lib.crypto

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
}