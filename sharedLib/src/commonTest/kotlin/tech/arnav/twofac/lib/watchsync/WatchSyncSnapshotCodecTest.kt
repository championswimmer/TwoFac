package tech.arnav.twofac.lib.watchsync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WatchSyncSnapshotCodecTest {

    @Test
    fun testRoundTrip() {
        val snapshot = WatchSyncSnapshot(
            generatedAtEpochSec = 1700000000L,
            accounts = listOf(
                WatchSyncAccount(
                    accountId = "github:user@example.com",
                    issuer = "GitHub",
                    accountLabel = "user@example.com",
                    otpAuthUri = "otpauth://totp/GitHub:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub",
                ),
            ),
        )

        val bytes = WatchSyncSnapshotCodec.encode(snapshot)
        val decoded = WatchSyncSnapshotCodec.decode(bytes)

        assertEquals(snapshot, decoded)
    }

    @Test
    fun testDecodeRejectsUnsupportedSchemaVersion() {
        val json = """{"version":99,"generatedAtEpochSec":1700000000,"accounts":[]}"""
        assertFailsWith<IllegalArgumentException> {
            WatchSyncSnapshotCodec.decode(json.encodeToByteArray())
        }
    }
}
