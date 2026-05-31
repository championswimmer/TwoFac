package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.theme.AccountColorTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupPayloadCodecTest {

    private val sampleEncryptedAccounts = listOf(
        EncryptedAccountEntry(
            accountLabel = "GitHub:user@example.com",
            salt = "00112233445566778899aabbccddeeff",
            encryptedURI = "deadbeef",
            color = AccountColorTag.TEAL,
        ),
        EncryptedAccountEntry(
            accountLabel = "Google:test@gmail.com",
            salt = "ffeeddccbbaa99887766554433221100",
            encryptedURI = "cafebabe",
        ),
    )

    private val sampleUris = listOf(
        "otpauth://totp/GitHub:user@example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=GitHub",
        "otpauth://totp/Google:test@gmail.com?secret=JBSWY3DPEHPK3PXP&issuer=Google",
    )

    private val samplePlaintextAccounts = sampleUris.mapIndexed { index, uri ->
        PlaintextAccountEntry(
            uri = uri,
            color = if (index == 0) AccountColorTag.TEAL else null,
        )
    }

    @Test
    fun testRoundTripPlaintextPayload() {
        val payload = BackupPayload(createdAt = 1700000000L, plaintextAccounts = samplePlaintextAccounts)
        val bytes = BackupPayloadCodec.encode(payload)
        val decoded = BackupPayloadCodec.decode(bytes)

        assertEquals(payload.schemaVersion, decoded.schemaVersion)
        assertEquals(payload.createdAt, decoded.createdAt)
        assertEquals(payload.encrypted, decoded.encrypted)
        assertEquals(emptyList(), decoded.accounts)
        assertEquals(samplePlaintextAccounts, decoded.plaintextAccounts)
        assertEquals(emptyList(), decoded.encryptedAccounts)
    }

    @Test
    fun testRoundTripEncryptedPayload() {
        val payload = BackupPayload(
            createdAt = 1700000000L,
            encrypted = true,
            encryptedAccounts = sampleEncryptedAccounts,
        )
        val bytes = BackupPayloadCodec.encode(payload)
        val decoded = BackupPayloadCodec.decode(bytes)

        assertEquals(payload.schemaVersion, decoded.schemaVersion)
        assertEquals(payload.createdAt, decoded.createdAt)
        assertTrue(decoded.encrypted)
        assertEquals(emptyList(), decoded.accounts)
        assertEquals(emptyList(), decoded.plaintextAccounts)
        assertEquals(sampleEncryptedAccounts, decoded.encryptedAccounts)
    }

    @Test
    fun testEncodedBytesAreValidJson() {
        val payload = BackupPayload(createdAt = 1700000000L, plaintextAccounts = samplePlaintextAccounts)
        val bytes = BackupPayloadCodec.encode(payload)
        val jsonString = bytes.decodeToString()

        assertTrue(jsonString.contains("schemaVersion"))
        assertTrue(jsonString.contains("createdAt"))
        assertTrue(jsonString.contains("plaintextAccounts"))
        assertTrue(jsonString.contains("encrypted"))
        assertTrue(jsonString.contains("GitHub"))
        assertTrue(jsonString.contains("teal"))
    }

    @Test
    fun testRoundTripWithEmptyAccounts() {
        val payload = BackupPayload(createdAt = 1700000000L, plaintextAccounts = emptyList())
        val bytes = BackupPayloadCodec.encode(payload)
        val decoded = BackupPayloadCodec.decode(bytes)

        assertEquals(0, decoded.plaintextAccounts.size)
    }

    @Test
    fun testDecodeRejectsUnsupportedSchemaVersion() {
        val json = """{"schemaVersion":99,"createdAt":1700000000,"encrypted":false,"plaintextAccounts":[]}"""
        assertFailsWith<IllegalArgumentException> {
            BackupPayloadCodec.decode(json.encodeToByteArray())
        }
    }

    @Test
    fun testDecodeIgnoresUnknownKeys() {
        val json = """{"schemaVersion":1,"createdAt":1700000000,"accounts":[],"unknownField":"value"}"""
        val decoded = BackupPayloadCodec.decode(json.encodeToByteArray())
        assertEquals(1, decoded.schemaVersion)
        assertEquals(emptyList(), decoded.encryptedAccounts)
    }

    @Test
    fun testDecodeVersion1PayloadAsPlaintext() {
        val json = """
            {
              "schemaVersion": 1,
              "createdAt": 1700000000,
              "accounts": ["${sampleUris.first()}"]
            }
        """.trimIndent()
        val decoded = BackupPayloadCodec.decode(json.encodeToByteArray())

        assertEquals(1, decoded.schemaVersion)
        assertEquals(false, decoded.encrypted)
        assertEquals(listOf(sampleUris.first()), decoded.accounts)
        assertEquals(emptyList(), decoded.encryptedAccounts)
    }

    @Test
    fun testDecodeVersion2PlaintextPayloadAsUriList() {
        val json = """
            {
              "schemaVersion": 2,
              "createdAt": 1700000000,
              "encrypted": false,
              "accounts": ["${sampleUris.first()}"]
            }
        """.trimIndent()
        val decoded = BackupPayloadCodec.decode(json.encodeToByteArray())

        assertEquals(2, decoded.schemaVersion)
        assertEquals(listOf(sampleUris.first()), decoded.accounts)
        assertEquals(emptyList(), decoded.plaintextAccounts)
    }

    @Test
    fun testDecodeVersion2EncryptedPayloadWithoutColor() {
        val json = """
            {
              "schemaVersion": 2,
              "createdAt": 1700000000,
              "encrypted": true,
              "encryptedAccounts": [
                {
                  "accountLabel": "GitHub:user@example.com",
                  "salt": "00112233445566778899aabbccddeeff",
                  "encryptedURI": "deadbeef"
                }
              ]
            }
        """.trimIndent()
        val decoded = BackupPayloadCodec.decode(json.encodeToByteArray())

        assertEquals(2, decoded.schemaVersion)
        assertEquals(null, decoded.encryptedAccounts.single().color)
    }

    @Test
    fun testDecodeRejectsVersion1EncryptedFlag() {
        val json = """
            {
              "schemaVersion": 1,
              "createdAt": 1700000000,
              "encrypted": true,
              "accounts": ["${sampleUris.first()}"]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            BackupPayloadCodec.decode(json.encodeToByteArray())
        }
    }

    @Test
    fun testDecodeRejectsMixedPayload() {
        val json = """
            {
              "schemaVersion": 2,
              "createdAt": 1700000000,
              "encrypted": true,
              "accounts": ["${sampleUris.first()}"],
              "encryptedAccounts": [
                {
                  "accountLabel": "GitHub:user@example.com",
                  "salt": "00112233445566778899aabbccddeeff",
                  "encryptedURI": "deadbeef"
                }
              ]
            }
        """.trimIndent()

        assertFailsWith<IllegalArgumentException> {
            BackupPayloadCodec.decode(json.encodeToByteArray())
        }
    }
}
