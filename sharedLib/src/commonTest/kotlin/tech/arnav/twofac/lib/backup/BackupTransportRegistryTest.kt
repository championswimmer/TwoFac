package tech.arnav.twofac.lib.backup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class BackupTransportRegistryTest {
    private class FakeTransport(
        override val id: String,
    ) : BackupTransport {
        override suspend fun isAvailable(): Boolean = true

        override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> {
            return BackupResult.Success(emptyList())
        }

        override suspend fun upload(
            content: ByteArray,
            descriptor: BackupDescriptor,
        ): BackupResult<BackupDescriptor> {
            return BackupResult.Success(descriptor)
        }

        override suspend fun download(backupId: String): BackupResult<BackupBlob> {
            return BackupResult.Failure("not implemented")
        }

        override suspend fun delete(backupId: String): BackupResult<Unit> {
            return BackupResult.Success(Unit)
        }
    }

    @Test
    fun returnsEmptyWhenNoProvidersRegistered() {
        val registry = StaticBackupTransportRegistry(emptyList())

        assertEquals(emptyList(), registry.all())
        assertNull(registry.get("missing"))
    }

    @Test
    fun looksUpProvidersById() {
        val localTransport = FakeTransport("local")
        val cloudTransport = FakeTransport("cloud")
        val registry = backupTransportRegistryOf(
            BackupProvider(LocalBackupProviderInfo, localTransport),
            BackupProvider(
                BackupProviderInfo(
                    id = "cloud",
                    displayName = "Cloud Backup",
                    supportsAutomaticRestore = true,
                    requiresAuthentication = true,
                ),
                cloudTransport,
            ),
        )

        assertEquals(listOf("local", "cloud"), registry.all().map { it.info.id })
        assertSame(localTransport, registry.get("local")?.transport)
        assertSame(cloudTransport, registry.get("cloud")?.transport)
        assertNull(registry.get("unknown"))
    }

    @Test
    fun rejectsDuplicateProviderIds() {
        val error = assertFailsWith<IllegalArgumentException> {
            backupTransportRegistryOf(
                BackupProvider(LocalBackupProviderInfo, FakeTransport("local")),
                BackupProvider(LocalBackupProviderInfo, FakeTransport("local")),
            )
        }

        assertEquals("Duplicate backup provider id 'local' is not allowed", error.message)
    }

    @Test
    fun iCloudProviderDoesNotRequireInteractiveAuthorization() {
        assertFalse(ICloudBackupProviderInfo.requiresAuthentication)
    }
}
