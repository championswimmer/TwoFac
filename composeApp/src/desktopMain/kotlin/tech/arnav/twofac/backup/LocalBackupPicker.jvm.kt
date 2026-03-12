package tech.arnav.twofac.backup

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker

internal actual suspend fun saveBackupFileWithPicker(
    suggestedFileName: String,
    content: ByteArray,
): Boolean {
    val directory = FileKit.openDirectoryPicker() ?: return false
    val destination = directory / suggestedFileName
    destination write content
    return true
}

internal actual suspend fun pickBackupFileWithPicker(): PickedBackupFile? {
    val file = FileKit.openFilePicker(
        type = FileKitType.File(listOf("json")),
    ) ?: return null

    return PickedBackupFile(
        id = file.name,
        content = file.readBytes(),
    )
}
