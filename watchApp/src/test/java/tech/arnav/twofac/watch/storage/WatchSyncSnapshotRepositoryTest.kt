package tech.arnav.twofac.watch.storage

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.arnav.twofac.lib.watchsync.WatchSyncSnapshot

class WatchSyncSnapshotRepositoryTest {

    @Test
    fun `initial state is empty`() = runTest {
        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.applicationContext } returns mockContext
        every { mockContext.filesDir.absolutePath } returns "/tmp"

        val repository = WatchSyncSnapshotRepository.get(mockContext)
        repository.initialize()

        val state = repository.state.value
        assertEquals(null, state.snapshot)
        assertEquals(null, state.lastSyncedAtEpochSec)
        assertEquals(null, state.lastError)
    }
}
