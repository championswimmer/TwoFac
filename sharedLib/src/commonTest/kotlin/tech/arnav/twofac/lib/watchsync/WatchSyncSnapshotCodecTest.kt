package tech.arnav.twofac.lib.watchsync

import tech.arnav.twofac.lib.theme.AccountColorTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WatchSyncSnapshotCodecTest {

    @Test
    fun testRoundTrip() {
        val snapshot = WatchSyncSnapshot(
            generatedAtEpochSec = 1700000000L,
            accounts = listOf(
                WatchSyncAccount(
                    accountId = "github:user@example.com",
                    issuer = "GitHub",
                    issuerIconKey = "github",
                    accountLabel = "user@example.com",
                    otpAuthUri = "otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub",
                    color = AccountColorTag.PURPLE,
                ),
            ),
        )

        val bytes = WatchSyncSnapshotCodec.encode(snapshot)
        val decoded = WatchSyncSnapshotCodec.decode(bytes)

        assertEquals(snapshot, decoded)
    }

    @Test
    fun testDecodeOldPayloadWithoutColorDefaultsToNull() {
        val json = """
            {
              "version": 1,
              "generatedAtEpochSec": 1700000000,
              "accounts": [
                {
                  "accountId": "github:user@example.com",
                  "issuer": "GitHub",
                  "issuerIconKey": "github",
                  "accountLabel": "user@example.com",
                  "otpAuthUri": "otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub"
                }
              ]
            }
        """.trimIndent()

        val decoded = WatchSyncSnapshotCodec.decode(json.encodeToByteArray())

        assertEquals(null, decoded.accounts.single().color)
    }

    @Test
    fun testDecodeRejectsUnsupportedSchemaVersion() {
        val json = """{"version":99,"generatedAtEpochSec":1700000000,"accounts":[]}"""
        assertFailsWith<IllegalArgumentException> {
            WatchSyncSnapshotCodec.decode(json.encodeToByteArray())
        }
    }

    @Test
    fun testWatchSyncAccountDefaultsIssuerIconKeyFromIssuer() {
        val account = WatchSyncAccount(
            accountId = "github:user@example.com",
            issuer = "GitHub",
            accountLabel = "user@example.com",
            otpAuthUri = "otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub",
        )

        assertEquals("github", account.issuerIconKey)
        assertEquals(null, account.color)
        assertTrue(account.issuerIconKey.isNotBlank())
    }
}
