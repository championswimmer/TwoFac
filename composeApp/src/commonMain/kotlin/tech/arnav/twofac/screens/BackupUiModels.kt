package tech.arnav.twofac.screens

import tech.arnav.twofac.lib.backup.BackupDescriptor

internal data class BackupProviderAvailability(
    val isAvailable: Boolean,
    val detail: String? = null,
)

internal fun backupAvailabilityLabel(availability: BackupProviderAvailability): String {
    return if (availability.isAvailable) {
        availability.detail ?: "Available"
    } else {
        availability.detail ?: "Unavailable"
    }
}

internal fun backupDescriptorMetadataLines(descriptor: BackupDescriptor): List<String> {
    return buildList {
        add("Created: ${timestampDisplayText(descriptor.createdAt)}")
        add("Size: ${descriptor.byteSize} byte(s)")
        add("Schema: v${descriptor.schemaVersion}")
        descriptor.remoteId?.let { add("Remote snapshot ID: $it") }
        descriptor.checksum?.let { add("Checksum: $it") }
    }
}
