package tech.arnav.twofac.lib.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupPayloadCodecTest {

    private val sampleSnapshot = BackupAccountSnapshot(
        id = "account-1",
        label = "Test Account",
        otpAuthUri = "otpauth://totp/Test:demo?secret=JBSWY3DPEHPK3PXP&issuer=Test",
    )

    @Test
    fun `encode and decode round trip`() {
        val codec = BackupPayloadCodec()
        val encoded = codec.encode(listOf(sampleSnapshot), appVersion = "1.0.0")
        assertTrue(encoded is BackupResult.Success)

        val decoded = codec.decode(encoded.value.blob)
        assertTrue(decoded is BackupResult.Success)
        assertEquals(BackupPayloadCodec.CURRENT_SCHEMA_VERSION, decoded.value.schemaVersion)
        assertEquals(1, decoded.value.accounts.size)
        assertEquals(sampleSnapshot.otpAuthUri, decoded.value.accounts.first().otpAuthUri)
    }
}
