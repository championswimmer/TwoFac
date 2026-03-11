package tech.arnav.twofac.lib.backup

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupTransportRegistryTest {

    private open class FakeTransport(
        override val id: String,
        override val displayName: String = id,
        private val available: Boolean = true,
    ) : BackupTransport {
        override suspend fun isAvailable(): Boolean = available

        override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> =
            BackupResult.Success(emptyList())

        override suspend fun upload(
            content: ByteArray,
            descriptor: BackupDescriptor,
        ): BackupResult<BackupDescriptor> = BackupResult.Success(descriptor)

        override suspend fun download(backupId: String): BackupResult<BackupBlob> =
            BackupResult.Failure("Not implemented")

        override suspend fun delete(backupId: String): BackupResult<Unit> = BackupResult.Success(Unit)
    }

    @Test
    fun testEmptyRegistry() = runTest {
        val registry = BackupTransportRegistry()

        assertTrue(registry.all().isEmpty())
        assertNull(registry.findById("local"))
        assertTrue(registry.providerInfo().isEmpty())
    }

    @Test
    fun testSingleProviderLookupAndInfo() = runTest {
        val local = FakeTransport(id = "local", displayName = "Local Files")
        val registry = BackupTransportRegistry(listOf(local))

        assertEquals(local, registry.findById("local"))

        val providers = registry.providerInfo()
        assertEquals(1, providers.size)
        assertEquals("local", providers.first().id)
        assertEquals("Local Files", providers.first().displayName)
        assertTrue(providers.first().isAvailable)
    }

    @Test
    fun testManyProviderRegistrationAndLookupById() = runTest {
        val local = FakeTransport(id = "local")
        val gdrive = FakeTransport(id = "gdrive-appdata", available = false)
        val registry = BackupTransportRegistry(listOf(local, gdrive))

        assertEquals(local, registry.findById("local"))
        assertEquals(gdrive, registry.findById("gdrive-appdata"))

        val providers = registry.providerInfo()
        assertEquals(2, providers.size)
        assertTrue(providers.any { it.id == "local" && it.isAvailable })
        assertTrue(providers.any { it.id == "gdrive-appdata" && !it.isAvailable })
    }

    @Test
    fun testProviderInfoMarksTransportUnavailableOnNonCancellationFailure() = runTest {
        val flaky = object : FakeTransport(id = "flaky") {
            override suspend fun isAvailable(): Boolean = error("boom")
        }

        val providers = BackupTransportRegistry(listOf(flaky)).providerInfo()
        assertEquals(1, providers.size)
        assertTrue(!providers.first().isAvailable)
    }

    @Test
    fun testProviderInfoRethrowsCancellationException() = runTest {
        val cancellable = object : FakeTransport(id = "cancelled") {
            override suspend fun isAvailable(): Boolean = throw CancellationException("cancel")
        }

        assertFailsWith<CancellationException> {
            BackupTransportRegistry(listOf(cancellable)).providerInfo()
        }
    }

    @Test
    fun testDuplicateIdsRejected() {
        assertFailsWith<IllegalArgumentException> {
            BackupTransportRegistry(
                listOf(
                    FakeTransport(id = "local"),
                    FakeTransport(id = "local"),
                )
            )
        }
    }
}
