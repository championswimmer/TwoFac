package tech.arnav.twofac.backup

import io.github.xxfast.kstore.file.storeOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import tech.arnav.twofac.lib.backup.BackupBlob
import tech.arnav.twofac.lib.backup.BackupDescriptor
import tech.arnav.twofac.lib.backup.BackupProviderInfo
import tech.arnav.twofac.lib.backup.BackupResult
import tech.arnav.twofac.lib.backup.BackupTransport
import tech.arnav.twofac.lib.backup.BackupProvider
import tech.arnav.twofac.lib.backup.BackupPreferences
import tech.arnav.twofac.lib.backup.providerPreferences
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupPreferencesManagerTest {
    private class FakeTransport(
        override val id: String,
    ) : BackupTransport {
        override suspend fun isAvailable(): Boolean = true

        override suspend fun listBackups(): BackupResult<List<BackupDescriptor>> = BackupResult.Success(emptyList())

        override suspend fun upload(
            content: ByteArray,
            descriptor: BackupDescriptor,
        ): BackupResult<BackupDescriptor> = BackupResult.Success(descriptor)

        override suspend fun download(backupId: String): BackupResult<BackupBlob> {
            return BackupResult.Failure("not implemented")
        }

        override suspend fun delete(backupId: String): BackupResult<Unit> = BackupResult.Success(Unit)
    }

    @Test
    fun recordsCompletionTimeInsteadOfSnapshotCreationTime() = runTest {
        val provider = BackupProvider(
            info = BackupProviderInfo(
                id = "gdrive-appdata",
                displayName = "Google Drive Backup",
                supportsAutomaticRestore = true,
                requiresAuthentication = true,
            ),
            transport = FakeTransport("gdrive-appdata"),
        )
        val descriptor = BackupDescriptor(
            id = "twofac-backup-42-0.json",
            transportId = provider.info.id,
            createdAt = 42,
            byteSize = 128,
            remoteId = "remote-1",
            checksum = "etag-1",
        )
        val manager = BackupPreferencesManager(
            store = storeOf(
                file = Path(Files.createTempDirectory("backup-preferences").resolve("prefs.json").toString()),
                default = BackupPreferences(),
            ),
            nowEpochSeconds = { 123456789L },
        )

        manager.recordBackupSuccess(provider, descriptor)
        manager.recordRestoreSuccess(provider, descriptor)

        val providerState = manager.get().providerPreferences(provider.info.id)
        assertEquals(123456789L, providerState.lastSuccessfulBackupAtEpochSeconds)
        assertEquals(123456789L, providerState.lastSuccessfulRestoreAtEpochSeconds)
        assertEquals("remote-1", providerState.lastRemoteMarker?.identifier)
        assertEquals(42, providerState.lastRemoteMarker?.modifiedAtEpochSeconds)
        assertEquals("etag-1", providerState.lastRemoteMarker?.versionTag)
    }
}
