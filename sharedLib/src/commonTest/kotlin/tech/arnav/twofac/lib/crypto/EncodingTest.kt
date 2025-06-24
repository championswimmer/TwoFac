package tech.arnav.twofac.lib.crypto

import kotlinx.io.bytestring.ByteString
import kotlin.test.Test
import kotlin.test.assertEquals

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
}