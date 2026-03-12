package tech.arnav.twofac.backup

internal data class PickedBackupFile(
    val id: String,
    val content: ByteArray,
)

internal expect suspend fun saveBackupFileWithPicker(
    suggestedFileName: String,
    content: ByteArray,
): Boolean

internal expect suspend fun pickBackupFileWithPicker(): PickedBackupFile?
