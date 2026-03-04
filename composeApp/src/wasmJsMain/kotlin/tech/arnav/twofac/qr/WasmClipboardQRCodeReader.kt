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

@JsFun(
    """
    (pasteTimeoutMs, onResult) => {
      const finishOnce = (() => {
        let finished = false;
        return (status, payload, message) => {
          if (finished) return;
          finished = true;
          onResult(status, payload ?? null, message ?? null);
        };
      })();

      if (
        typeof window === "undefined" ||
        typeof document === "undefined" ||
        window.isSecureContext !== true
      ) {
        finishOnce("UNSUPPORTED", null, "Clipboard APIs require a secure browser context");
        return;
      }

      const importJsQr = async () => {
        const jsQrModule = await import("jsqr");
        return jsQrModule && jsQrModule.default ? jsQrModule.default : jsQrModule;
      };

      const decodeBlobWithJsQr = async (blob) => {
        if (
          typeof createImageBitmap !== "function" ||
          !blob
        ) {
          return { status: "UNSUPPORTED", payload: null, message: "Image bitmap decoding is unavailable" };
        }

        const jsQR = await importJsQr();
        if (typeof jsQR !== "function") {
          return { status: "DECODE_FAILURE", payload: null, message: "jsQR decoder is unavailable" };
        }

        let imageBitmap = null;
        try {
          imageBitmap = await createImageBitmap(blob);
          const canvas = document.createElement("canvas");
          canvas.width = imageBitmap.width;
          canvas.height = imageBitmap.height;

          const context = canvas.getContext("2d", { willReadFrequently: true });
          if (!context || typeof context.getImageData !== "function") {
            return { status: "DECODE_FAILURE", payload: null, message: "Canvas image decoding is unavailable" };
          }

          context.drawImage(imageBitmap, 0, 0);
          const imageData = context.getImageData(0, 0, imageBitmap.width, imageBitmap.height);
          const decoded = jsQR(imageData.data, imageData.width, imageData.height);

          if (!decoded || typeof decoded.data !== "string" || decoded.data.length === 0) {
            return { status: "DECODE_FAILURE", payload: null, message: "No QR code detected in clipboard image" };
          }

          return { status: "SUCCESS", payload: decoded.data, message: null };
        } catch (error) {
          const name = (error && error.name) || "DecodeError";
          return { status: "DECODE_FAILURE", payload: null, message: name };
        } finally {
          if (imageBitmap && typeof imageBitmap.close === "function") {
            imageBitmap.close();
          }
        }
      };

      const decodeFirstImageBlob = async (blobPromises) => {
        if (!blobPromises || blobPromises.length === 0) {
          return { status: "NO_IMAGE", payload: null, message: "No image found in clipboard" };
        }

        let hadImage = false;
        for (let index = 0; index < blobPromises.length; index++) {
          let blob = null;
          try {
            blob = await blobPromises[index];
          } catch (_) {
            blob = null;
          }
          if (!blob) continue;
          hadImage = true;

          const decoded = await decodeBlobWithJsQr(blob);
          if (decoded.status === "SUCCESS" || decoded.status === "UNSUPPORTED") {
            return decoded;
          }
        }

        if (!hadImage) {
          return { status: "NO_IMAGE", payload: null, message: "No image found in clipboard" };
        }
        return { status: "DECODE_FAILURE", payload: null, message: "Unable to decode QR code from clipboard image" };
      };

      const extractImageBlobPromisesFromClipboardItems = (clipboardItems) => {
        const blobPromises = [];
        if (!clipboardItems || typeof clipboardItems.length !== "number") {
          return blobPromises;
        }

        for (let itemIndex = 0; itemIndex < clipboardItems.length; itemIndex++) {
          const clipboardItem = clipboardItems[itemIndex];
          if (!clipboardItem || typeof clipboardItem.getType !== "function") continue;

          const types = clipboardItem.types || [];
          for (let typeIndex = 0; typeIndex < types.length; typeIndex++) {
            const mimeType = types[typeIndex];
            if (typeof mimeType === "string" && mimeType.toLowerCase().startsWith("image/")) {
              blobPromises.push(Promise.resolve(clipboardItem.getType(mimeType)));
            }
          }
        }

        return blobPromises;
      };

      const extractImageBlobPromisesFromPasteData = (clipboardData) => {
        const blobPromises = [];
        if (!clipboardData) return blobPromises;

        const dataItems = clipboardData.items;
        if (dataItems && typeof dataItems.length === "number") {
          for (let index = 0; index < dataItems.length; index++) {
            const item = dataItems[index];
            const mimeType = (item && item.type ? item.type : "").toLowerCase();
            if (item && typeof item.getAsFile === "function" && mimeType.startsWith("image/")) {
              blobPromises.push(Promise.resolve(item.getAsFile()));
            }
          }
        }

        if (blobPromises.length === 0) {
          const files = clipboardData.files;
          if (files && typeof files.length === "number") {
            for (let index = 0; index < files.length; index++) {
              const file = files[index];
              const mimeType = (file && file.type ? file.type : "").toLowerCase();
              if (file && mimeType.startsWith("image/")) {
                blobPromises.push(Promise.resolve(file));
              }
            }
          }
        }

        return blobPromises;
      };

      const readUsingAsyncClipboardApi = async () => {
        if (
          typeof navigator === "undefined" ||
          navigator.clipboard == null ||
          typeof navigator.clipboard.read !== "function"
        ) {
          return { status: "UNAVAILABLE", payload: null, message: "Async clipboard API is unavailable" };
        }

        try {
          const clipboardItems = await navigator.clipboard.read();
          return await decodeFirstImageBlob(extractImageBlobPromisesFromClipboardItems(clipboardItems));
        } catch (error) {
          const name = (error && error.name) || "ClipboardReadError";
          if (name === "NotAllowedError") {
            return { status: "PERMISSION_DENIED", payload: null, message: name };
          }
          if (name === "AbortError") {
            return { status: "CANCELED", payload: null, message: name };
          }
          if (name === "NotSupportedError" || name === "SecurityError") {
            return { status: "UNSUPPORTED", payload: null, message: name };
          }
          return { status: "DECODE_FAILURE", payload: null, message: name };
        }
      };

      const readUsingPasteEvent = (timeoutMs, timeoutStatus, timeoutMessage) => new Promise((resolve) => {
        if (
          typeof window.addEventListener !== "function" ||
          typeof window.removeEventListener !== "function"
        ) {
          resolve({ status: "UNSUPPORTED", payload: null, message: "Paste event API is unavailable" });
          return;
        }

        let completed = false;
        let timeoutHandle = null;

        const finalize = (result) => {
          if (completed) return;
          completed = true;
          if (timeoutHandle != null) {
            clearTimeout(timeoutHandle);
          }
          window.removeEventListener("paste", onPaste, true);
          resolve(result);
        };

        const onPaste = (event) => {
          const clipboardData = event && event.clipboardData ? event.clipboardData : null;
          decodeFirstImageBlob(extractImageBlobPromisesFromPasteData(clipboardData))
            .then(finalize)
            .catch((error) => {
              const name = (error && error.name) || "PasteDecodeError";
              finalize({ status: "DECODE_FAILURE", payload: null, message: name });
            });
        };

        window.addEventListener("paste", onPaste, true);
        timeoutHandle = setTimeout(
          () => finalize({ status: timeoutStatus, payload: null, message: timeoutMessage }),
          timeoutMs,
        );
      });

      (async () => {
        const immediatePasteResult = await readUsingPasteEvent(
          250,
          "NO_PASTE_EVENT",
          "No immediate paste event",
        );
        if (immediatePasteResult.status !== "NO_PASTE_EVENT") {
          finishOnce(immediatePasteResult.status, immediatePasteResult.payload, immediatePasteResult.message);
          return;
        }

        const asyncClipboardResult = await readUsingAsyncClipboardApi();
        if (
          asyncClipboardResult.status === "UNAVAILABLE" ||
          asyncClipboardResult.status === "UNSUPPORTED"
        ) {
          const fallbackTimeoutMs =
            typeof pasteTimeoutMs === "number" && pasteTimeoutMs > 0
              ? pasteTimeoutMs
              : 10000;
          const pasteFallbackResult = await readUsingPasteEvent(
            fallbackTimeoutMs,
            "CANCELED",
            "Paste event timed out",
          );
          if (
            pasteFallbackResult.status === "UNSUPPORTED" &&
            asyncClipboardResult.status === "UNSUPPORTED"
          ) {
            finishOnce("UNSUPPORTED", null, asyncClipboardResult.message || pasteFallbackResult.message);
            return;
          }
          finishOnce(pasteFallbackResult.status, pasteFallbackResult.payload, pasteFallbackResult.message);
          return;
        }

        finishOnce(asyncClipboardResult.status, asyncClipboardResult.payload, asyncClipboardResult.message);
      })().catch((error) => {
        const name = (error && error.name) || "ClipboardReadFailed";
        finishOnce("DECODE_FAILURE", null, name);
      });
    }
    """
)
private external fun readQRCodeFromClipboard(
    pasteTimeoutMs: Int,
    onResult: (String, String?, String?) -> Unit,
)
