package tech.arnav.twofac.lib.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupPayloadCodecTest {

    private val sampleUris = listOf(
        "otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub",
        "otpauth://totp/Google:test@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=Google",
    )

    @Test
    fun testRoundTrip() {
        val payload = BackupPayload(createdAt = 1700000000L, accounts = sampleUris)
        val bytes = BackupPayloadCodec.encode(payload)
        val decoded = BackupPayloadCodec.decode(bytes)

        assertEquals(payload.schemaVersion, decoded.schemaVersion)
        assertEquals(payload.createdAt, decoded.createdAt)
        assertEquals(payload.accounts, decoded.accounts)
    }

    @Test
    fun testEncodedBytesAreValidJson() {
        val payload = BackupPayload(createdAt = 1700000000L, accounts = sampleUris)
        val bytes = BackupPayloadCodec.encode(payload)
        val jsonString = bytes.decodeToString()

        assertTrue(jsonString.contains("schemaVersion"))
        assertTrue(jsonString.contains("createdAt"))
        assertTrue(jsonString.contains("accounts"))
        assertTrue(jsonString.contains("GitHub"))
    }

    @Test
    fun testRoundTripWithEmptyAccounts() {
        val payload = BackupPayload(createdAt = 1700000000L, accounts = emptyList())
        val bytes = BackupPayloadCodec.encode(payload)
        val decoded = BackupPayloadCodec.decode(bytes)

        assertEquals(0, decoded.accounts.size)
    }

    @Test
    fun testDecodeRejectsUnsupportedSchemaVersion() {
        val json = """{"schemaVersion":99,"createdAt":1700000000,"accounts":[]}"""
        assertFailsWith<IllegalArgumentException> {
            BackupPayloadCodec.decode(json.encodeToByteArray())
        }
    }

    @Test
    fun testDecodeIgnoresUnknownKeys() {
        val json = """{"schemaVersion":1,"createdAt":1700000000,"accounts":[],"unknownField":"value"}"""
        val decoded = BackupPayloadCodec.decode(json.encodeToByteArray())
        assertEquals(1, decoded.schemaVersion)
    }
}
