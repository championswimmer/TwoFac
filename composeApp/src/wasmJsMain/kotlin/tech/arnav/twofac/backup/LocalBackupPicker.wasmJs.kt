@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalEncodingApi::class)

package tech.arnav.twofac.backup

import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

internal actual suspend fun saveBackupFileWithPicker(
    suggestedFileName: String,
    content: ByteArray,
): Boolean {
    val contentBase64 = Base64.encode(content)
    return runCatching {
        BackupInterop.saveBackupFile(suggestedFileName, contentBase64)
            .await<SaveBackupInteropResult>()
            .success
    }.getOrElse { false }
}

internal actual suspend fun pickBackupFileWithPicker(): PickedBackupFile? {
    val picked = runCatching {
        BackupInterop.pickBackupFile(".json").await<BackupFileInteropResult?>()
    }.getOrNull() ?: return null

    val content = runCatching {
        Base64.decode(picked.contentBase64)
    }.getOrNull() ?: return null

    return PickedBackupFile(
        id = picked.name,
        content = content,
    )
}

private external interface SaveBackupInteropResult : JsAny {
    val success: Boolean
}

private external interface BackupFileInteropResult : JsAny {
    val name: String
    val contentBase64: String
}

@JsModule("./backup.mjs")
private external object BackupInterop {
    fun saveBackupFile(fileName: String, contentBase64: String): Promise<SaveBackupInteropResult>
    fun pickBackupFile(accept: String = definedExternally): Promise<BackupFileInteropResult?>
}
