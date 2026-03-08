package tech.arnav.twofac.screens

import tech.arnav.twofac.lib.backup.BackupDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackupUiModelsTest {
    @Test
    fun availabilityLabelFallsBackToSimpleStates() {
        assertEquals(
            "Available",
            backupAvailabilityLabel(BackupProviderAvailability(isAvailable = true)),
        )
        assertEquals(
            "Unavailable",
            backupAvailabilityLabel(BackupProviderAvailability(isAvailable = false)),
        )
        assertEquals(
            "Google Drive backup is configured but not connected.",
            backupAvailabilityLabel(
                BackupProviderAvailability(
                    isAvailable = false,
                    detail = "Google Drive backup is configured but not connected.",
                ),
            ),
        )
    }

    @Test
    fun descriptorMetadataIncludesUsefulSnapshotFields() {
        val descriptor = BackupDescriptor(
            id = "twofac-backup-1700000000-0.json",
            transportId = "gdrive-appdata",
            createdAt = 1_700_000_000,
            schemaVersion = 2,
            byteSize = 256,
            remoteId = "drive-file-123",
            checksum = "abc123",
        )

        val lines = backupDescriptorMetadataLines(descriptor)

        assertEquals("Created: Unix epoch second 1700000000", lines[0])
        assertTrue(lines.contains("Size: 256 byte(s)"))
        assertTrue(lines.contains("Schema: v2"))
        assertTrue(lines.contains("Remote snapshot ID: drive-file-123"))
        assertTrue(lines.contains("Checksum: abc123"))
    }
}
