@file:OptIn(ExperimentalWasmJsInterop::class)

package tech.arnav.twofac.qr

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val DEFAULT_PASTE_TIMEOUT_MS = 10000
private const val NO_CLIPBOARD_IMAGE_REASON = "No image found in clipboard"
private const val CLIPBOARD_QR_DECODE_FAILURE_REASON = "Failed to decode QR code from clipboard image"

class WasmClipboardQRCodeReader(
    private val pasteTimeoutMs: Int = DEFAULT_PASTE_TIMEOUT_MS,
) : ClipboardQRCodeReader {
    override suspend fun readQRCode(): QRCodeReadResult = suspendCoroutine { continuation ->
        var completed = false

        fun resolve(result: QRCodeReadResult) {
            if (completed) return
            completed = true
            continuation.resume(result)
        }

        runCatching {
            readQRCodeFromClipboard(
                pasteTimeoutMs = pasteTimeoutMs,
            ) { status, decodedPayload, message ->
                resolve(
                    when (status) {
                        "SUCCESS" ->
                            QRCodeUtils.decodedPayloadCandidatesToQRCodeReadResult(
                                primaryPayload = decodedPayload,
                            )

                        "PERMISSION_DENIED" -> QRCodeReadResult.PermissionDenied
                        "UNSUPPORTED", "UNAVAILABLE" -> QRCodeReadResult.Unsupported
                        "CANCELED" -> QRCodeReadResult.Canceled
                        "NO_IMAGE" ->
                            QRCodeReadResult.DecodeFailure(
                                message ?: NO_CLIPBOARD_IMAGE_REASON,
                            )

                        "DECODE_FAILURE", "FAILED" ->
                            QRCodeReadResult.DecodeFailure(
                                message ?: CLIPBOARD_QR_DECODE_FAILURE_REASON,
                            )

                        else ->
                            QRCodeReadResult.DecodeFailure(
                                message ?: CLIPBOARD_QR_DECODE_FAILURE_REASON,
                            )
                    }
                )
            }
        }.onFailure { error ->
            resolve(
                QRCodeReadResult.DecodeFailure(
                    error.message ?: CLIPBOARD_QR_DECODE_FAILURE_REASON,
                )
            )
        }
    }
}

@JsModule("qr-reader.js")
private external fun readQRCodeFromClipboard(
    pasteTimeoutMs: Int,
    onResult: (String, String?, String?) -> Unit,
)
