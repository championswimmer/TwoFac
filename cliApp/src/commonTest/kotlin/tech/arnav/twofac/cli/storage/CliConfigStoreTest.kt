package tech.arnav.twofac.cli.storage

import kotlinx.io.files.SystemFileSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliConfigStoreTest {

    @AfterTest
    fun cleanup() {
        val configPath = AppDirUtils.getCliConfigFilePath()
        if (SystemFileSystem.exists(configPath)) {
            SystemFileSystem.delete(configPath)
        }
    }

    @Test
    fun testWriteAndReadBackendSelection() {
        val written = CliConfigStore.write(
            CliConfig(
                storageBackend = CliStorageBackend.COMMON,
                issuerIconsEnabled = false,
            )
        )

        assertTrue(written)
        val config = CliConfigStore.read()
        assertEquals(CliStorageBackend.COMMON, config.storageBackend)
        assertFalse(config.issuerIconsEnabled)
    }

    @Test
    fun testReadDefaultsIssuerIconsToEnabledWhenMissingFromConfig() {
        val written = CliConfigStore.write(CliConfig(storageBackend = CliStorageBackend.STANDALONE))

        assertTrue(written)
        assertTrue(CliConfigStore.read().issuerIconsEnabled)
    }
}
