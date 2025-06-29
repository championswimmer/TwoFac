package tech.arnav.twofac.lib.crypto

import kotlinx.io.bytestring.ByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EncodingTest {
    // Test cases for Base32 decoding
    val testCases = mapOf(
        "hello world!" to "NBSWY3DPEB3W64TMMQQQ====",
        "12345678901234567890" to "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
    )

    @Test
    fun testDecodeBase32() {

        for ((stringInput, base32Encoded) in testCases) {
            val decodedBytes = Encoding.decodeBase32(base32Encoded)
            val expectedBytes = stringInput.encodeToByteArray()
            assertEquals(ByteString(expectedBytes), ByteString(decodedBytes))
        }
    }

    @Test
    fun testEncodeBase32() {
        for ((stringInput, base32Encoded) in testCases) {
            val inputBytes = stringInput.encodeToByteArray()
            val encodedString = Encoding.encodeBase32(inputBytes)
            assertEquals(base32Encoded, encodedString)
        }
    }

    @Test
    fun testHexToByteString() {
        // Test valid hex strings
        assertEquals(
            ByteString(byteArrayOf(1, 35, 69, 103, -119, -85, -51, -17)),
            Encoding.hexToByteString("0123456789abcdef")
        )
        assertEquals(
            ByteString(byteArrayOf(1, 35, 69, 103, -119, -85, -51, -17)),
            Encoding.hexToByteString("0123456789ABCDEF")
        )

        // Test empty string
        assertEquals(ByteString(byteArrayOf()), Encoding.hexToByteString(""))

        // Test odd-length string (should throw an exception)
        assertFailsWith<IllegalArgumentException> {
            Encoding.hexToByteString("123")
        }

        // Test invalid hex characters
        assertFailsWith<NumberFormatException> {
            Encoding.hexToByteString("123G")
        }
    }

    @Test
    fun testByteStringToHex() {
        // Test valid ByteStrings
        assertEquals(
            "0123456789abcdef",
            Encoding.byteStringToHex(ByteString(byteArrayOf(1, 35, 69, 103, -119, -85, -51, -17)))
        )

        // Test empty ByteString
        assertEquals("", Encoding.byteStringToHex(ByteString(byteArrayOf())))

        // Test ByteString with values that need padding
        assertEquals(
            "0001020304050607",
            Encoding.byteStringToHex(ByteString(byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)))
        )
    }
}
