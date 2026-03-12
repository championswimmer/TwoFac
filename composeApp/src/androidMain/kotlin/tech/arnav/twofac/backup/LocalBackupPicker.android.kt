package tech.arnav.twofac.backup

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver

internal actual suspend fun saveBackupFileWithPicker(
    suggestedFileName: String,
    content: ByteArray,
): Boolean {
    // On Android, directory picker returns URI-backed PlatformFile objects.
    // Building child paths via `/` requires a filesystem Path and fails for many content:// URIs.
    // Use the file saver dialog so the platform returns a concrete writable destination URI.
    val suggestedName = suggestedFileName.substringBeforeLast('.', suggestedFileName)
    val extension = suggestedFileName
        .substringAfterLast('.', missingDelimiterValue = "")
        .ifBlank { null }

    val destination = FileKit.openFileSaver(
        suggestedName = suggestedName,
        extension = extension,
    ) ?: return false

    destination write content
    return true
}

internal actual suspend fun pickBackupFileWithPicker(): PickedBackupFile? {
    // Restrict selection to JSON files; cancel yields null.
    val file = FileKit.openFilePicker(
        type = FileKitType.File(listOf("json")),
    ) ?: return null

    // Keep the picked file name as an identifier and load full bytes for import.
    return PickedBackupFile(
        id = file.name,
        content = file.readBytes(),
    )
}
