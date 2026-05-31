@file:OptIn(ExperimentalUuidApi::class)

package tech.arnav.twofac.lib.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.arnav.twofac.lib.theme.AccountColorTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class StoredAccountSerializationTest {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun oldStoredAccountJsonWithoutColorDecodesWithNullColor() {
        val accountId = Uuid.random()
        val encoded = """
            {
              "accountID": "$accountId",
              "accountLabel": "GitHub:alice@example.com",
              "salt": "00112233445566778899aabbccddeeff",
              "encryptedURI": "deadbeef"
            }
        """.trimIndent()

        val decoded = json.decodeFromString<StoredAccount>(encoded)

        assertEquals(accountId, decoded.accountID)
        assertEquals("GitHub:alice@example.com", decoded.accountLabel)
        assertNull(decoded.color)
    }

    @Test
    fun storedAccountJsonWithColorDecodesSelectedTag() {
        val encoded = """
            {
              "accountID": "00000000-0000-0000-0000-000000000001",
              "accountLabel": "GitHub:alice@example.com",
              "salt": "00112233445566778899aabbccddeeff",
              "encryptedURI": "deadbeef",
              "color": "teal"
            }
        """.trimIndent()

        val decoded = json.decodeFromString<StoredAccount>(encoded)

        assertEquals(AccountColorTag.TEAL, decoded.color)
    }

    @Test
    fun storedAccountColorEncodesAsStableSerializedName() {
        val account = StoredAccount(
            accountID = Uuid.parse("00000000-0000-0000-0000-000000000002"),
            accountLabel = "Google:bob@example.com",
            salt = "00112233445566778899aabbccddeeff",
            encryptedURI = "cafebabe",
            color = AccountColorTag.PURPLE,
        )

        val encoded = json.encodeToString(account)

        assertTrue(encoded.contains("\"color\":\"purple\""))
    }
}
