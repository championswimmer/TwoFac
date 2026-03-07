package tech.arnav.twofac.lib.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupTransportRegistryTest {

    private fun fakeTransport(id: String): BackupTransport = object : BackupTransport {
        override val id = id
        override suspend fun isAvailable() = true
        override suspend fun listBackups() = BackupResult.Success(emptyList<BackupDescriptor>())
        override suspend fun upload(content: ByteArray, descriptor: BackupDescriptor) =
            BackupResult.Success(descriptor)
        override suspend fun download(backupId: String) =
            BackupResult.Failure("not implemented")
        override suspend fun delete(backupId: String) = BackupResult.Success(Unit)
    }

    private fun info(id: String, displayName: String = id) = BackupProviderInfo(
        id = id,
        displayName = displayName,
    )

    @Test
    fun emptyRegistryHasNoProviders() {
        val registry = BackupTransportRegistry()
        assertTrue(registry.isEmpty())
        assertEquals(0, registry.size)
        assertEquals(emptyList(), registry.providers())
    }

    @Test
    fun registerSingleProvider() {
        val registry = BackupTransportRegistry()
        val transport = fakeTransport("local")
        registry.register(transport, info("local", "Local Backup"))

        assertEquals(1, registry.size)
        assertEquals(listOf(info("local", "Local Backup")), registry.providers())
        assertNotNull(registry.transport("local"))
        assertEquals("local", registry.transport("local")!!.id)
    }

    @Test
    fun registerMultipleProviders() {
        val registry = BackupTransportRegistry()
        registry.register(fakeTransport("local"), info("local", "Local Backup"))
        registry.register(fakeTransport("gdrive-appdata"), info("gdrive-appdata", "Google Drive"))

        assertEquals(2, registry.size)
        val ids = registry.providers().map { it.id }
        assertEquals(listOf("local", "gdrive-appdata"), ids)
        assertNotNull(registry.transport("local"))
        assertNotNull(registry.transport("gdrive-appdata"))
    }

    @Test
    fun lookupMissingProviderReturnsNull() {
        val registry = BackupTransportRegistry()
        registry.register(fakeTransport("local"), info("local"))

        assertNull(registry.transport("nonexistent"))
        assertNull(registry.providerInfo("nonexistent"))
    }

    @Test
    fun duplicateRegistrationThrows() {
        val registry = BackupTransportRegistry()
        registry.register(fakeTransport("local"), info("local"))

        assertFailsWith<IllegalArgumentException> {
            registry.register(fakeTransport("local"), info("local"))
        }
    }

    @Test
    fun providerInfoLookup() {
        val registry = BackupTransportRegistry()
        val providerInfo = BackupProviderInfo(
            id = "icloud",
            displayName = "iCloud",
            supportsManualBackup = true,
            supportsManualRestore = true,
            supportsAutomaticRestore = true,
            authRequired = false,
        )
        registry.register(fakeTransport("icloud"), providerInfo)

        val found = registry.providerInfo("icloud")
        assertNotNull(found)
        assertEquals("iCloud", found.displayName)
        assertTrue(found.supportsAutomaticRestore)
    }
}
