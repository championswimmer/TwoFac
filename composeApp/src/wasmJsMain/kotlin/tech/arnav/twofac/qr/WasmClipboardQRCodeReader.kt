@file:OptIn(ExperimentalWasmJsInterop::class)

package tech.arnav.twofac.qr

import kotlinx.coroutines.await
import kotlin.js.Promise

private const val DEFAULT_PASTE_TIMEOUT_MS = 10000
private const val NO_CLIPBOARD_IMAGE_REASON = "No image found in clipboard"
private const val CLIPBOARD_QR_DECODE_FAILURE_REASON = "Failed to decode QR code from clipboard image"

class WasmClipboardQRCodeReader(
    private val pasteTimeoutMs: Int = DEFAULT_PASTE_TIMEOUT_MS,
) : ClipboardQRCodeReader {
    override suspend fun readQRCode(): QRCodeReadResult = runCatching {
        val result = QRReaderInterop.readQRCodeFromClipboard(
            pasteTimeoutMs = pasteTimeoutMs,
        ).await<ClipboardQRCodeInteropResult>()
        when (result.status) {
            "SUCCESS" ->
                QRCodeUtils.decodedPayloadCandidatesToQRCodeReadResult(
                    primaryPayload = result.decodedPayload,
                )

            "PERMISSION_DENIED" -> QRCodeReadResult.PermissionDenied
            "UNSUPPORTED", "UNAVAILABLE" -> QRCodeReadResult.Unsupported
            "CANCELED" -> QRCodeReadResult.Canceled
            "NO_IMAGE" ->
                QRCodeReadResult.DecodeFailure(
                    result.message ?: NO_CLIPBOARD_IMAGE_REASON,
                )

            "DECODE_FAILURE", "FAILED" ->
                QRCodeReadResult.DecodeFailure(
                    result.message ?: CLIPBOARD_QR_DECODE_FAILURE_REASON,
                )

            else ->
                QRCodeReadResult.DecodeFailure(
                    result.message ?: CLIPBOARD_QR_DECODE_FAILURE_REASON,
                )
        }
    }.getOrElse { error ->
        QRCodeReadResult.DecodeFailure(
            error.message ?: CLIPBOARD_QR_DECODE_FAILURE_REASON,
        )
    }
}

private external interface ClipboardQRCodeInteropResult {
    val status: String
    val decodedPayload: String?
    val message: String?
}

@JsModule("./qr-reader.mjs")
private external object QRReaderInterop {
    fun readQRCodeFromClipboard(pasteTimeoutMs: Int): Promise<JsAny?>
}
