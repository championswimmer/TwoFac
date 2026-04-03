package tech.arnav.twofac.cli.storage

import kotlinx.io.files.SystemFileSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
        val written = CliConfigStore.write(CliConfig(storageBackend = CliStorageBackend.COMMON))

        assertTrue(written)
        assertEquals(CliStorageBackend.COMMON, CliConfigStore.read().storageBackend)
    }
}
