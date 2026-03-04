import jsQR from "jsqr";
const decodeBlobWithJsQr = async (blob) => {
    if (typeof createImageBitmap !== "function" || !blob) {
        return { status: "UNSUPPORTED", decodedPayload: null, message: "Image bitmap decoding is unavailable" };
    }
    if (typeof jsQR !== "function") {
        return { status: "DECODE_FAILURE", decodedPayload: null, message: "jsQR decoder is unavailable" };
    }
    let imageBitmap = null;
    try {
        imageBitmap = await createImageBitmap(blob);
        const canvas = document.createElement("canvas");
        canvas.width = imageBitmap.width;
        canvas.height = imageBitmap.height;
        const context = canvas.getContext("2d", { willReadFrequently: true });
        if (!context || typeof context.getImageData !== "function") {
            return { status: "DECODE_FAILURE", decodedPayload: null, message: "Canvas image decoding is unavailable" };
        }
        context.drawImage(imageBitmap, 0, 0);
        const imageData = context.getImageData(0, 0, imageBitmap.width, imageBitmap.height);
        const decoded = jsQR(imageData.data, imageData.width, imageData.height);
        if (!decoded || typeof decoded.data !== "string" || decoded.data.length === 0) {
            return { status: "DECODE_FAILURE", decodedPayload: null, message: "No QR code detected in clipboard image" };
        }
        return { status: "SUCCESS", decodedPayload: decoded.data, message: null };
    }
    catch (error) {
        const name = error?.name ?? "DecodeError";
        return { status: "DECODE_FAILURE", decodedPayload: null, message: name };
    }
    finally {
        if (imageBitmap && typeof imageBitmap.close === "function") {
            imageBitmap.close();
        }
    }
};
const decodeFirstImageBlob = async (blobPromises) => {
    if (blobPromises.length === 0) {
        return { status: "NO_IMAGE", decodedPayload: null, message: "No image found in clipboard" };
    }
    let hadImage = false;
    for (const blobPromise of blobPromises) {
        let blob = null;
        try {
            blob = await blobPromise;
        }
        catch {
            blob = null;
        }
        if (!blob)
            continue;
        hadImage = true;
        const decoded = await decodeBlobWithJsQr(blob);
        if (decoded.status === "SUCCESS" || decoded.status === "UNSUPPORTED") {
            return decoded;
        }
    }
    if (!hadImage) {
        return { status: "NO_IMAGE", decodedPayload: null, message: "No image found in clipboard" };
    }
    return { status: "DECODE_FAILURE", decodedPayload: null, message: "Unable to decode QR code from clipboard image" };
};
const extractImageBlobPromisesFromClipboardItems = (clipboardItems) => {
    const blobPromises = [];
    if (!clipboardItems)
        return blobPromises;
    for (const clipboardItem of clipboardItems) {
        for (const mimeType of clipboardItem.types ?? []) {
            if (typeof mimeType === "string" && mimeType.toLowerCase().startsWith("image/")) {
                blobPromises.push(Promise.resolve(clipboardItem.getType(mimeType)));
            }
        }
    }
    return blobPromises;
};
const extractImageBlobPromisesFromPasteData = (clipboardData) => {
    const blobPromises = [];
    if (!clipboardData)
        return blobPromises;
    const dataItems = clipboardData.items;
    if (dataItems && typeof dataItems.length === "number") {
        for (let index = 0; index < dataItems.length; index++) {
            const item = dataItems[index];
            const mimeType = (item?.type ?? "").toLowerCase();
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
                const mimeType = (file?.type ?? "").toLowerCase();
                if (file && mimeType.startsWith("image/")) {
                    blobPromises.push(Promise.resolve(file));
                }
            }
        }
    }
    return blobPromises;
};
const readUsingAsyncClipboardApi = async () => {
    if (typeof navigator === "undefined" || navigator.clipboard == null || typeof navigator.clipboard.read !== "function") {
        return { status: "UNAVAILABLE", decodedPayload: null, message: "Async clipboard API is unavailable" };
    }
    try {
        const clipboardItems = await navigator.clipboard.read();
        return decodeFirstImageBlob(extractImageBlobPromisesFromClipboardItems(clipboardItems));
    }
    catch (error) {
        const name = error?.name ?? "ClipboardReadError";
        if (name === "NotAllowedError") {
            return { status: "PERMISSION_DENIED", decodedPayload: null, message: name };
        }
        if (name === "AbortError") {
            return { status: "CANCELED", decodedPayload: null, message: name };
        }
        if (name === "NotSupportedError" || name === "SecurityError") {
            return { status: "UNSUPPORTED", decodedPayload: null, message: name };
        }
        return { status: "DECODE_FAILURE", decodedPayload: null, message: name };
    }
};
const readUsingPasteEvent = (timeoutMs, timeoutStatus, timeoutMessage) => new Promise((resolve) => {
    if (typeof window.addEventListener !== "function" || typeof window.removeEventListener !== "function") {
        resolve({ status: "UNSUPPORTED", decodedPayload: null, message: "Paste event API is unavailable" });
        return;
    }
    let completed = false;
    let timeoutHandle = null;
    const finalize = (result) => {
        if (completed)
            return;
        completed = true;
        if (timeoutHandle != null) {
            clearTimeout(timeoutHandle);
        }
        window.removeEventListener("paste", onPaste, true);
        resolve(result);
    };
    const onPaste = (event) => {
        const clipboardData = event?.clipboardData ?? null;
        decodeFirstImageBlob(extractImageBlobPromisesFromPasteData(clipboardData))
            .then(finalize)
            .catch((error) => {
            const name = error?.name ?? "PasteDecodeError";
            finalize({ status: "DECODE_FAILURE", decodedPayload: null, message: name });
        });
    };
    window.addEventListener("paste", onPaste, true);
    timeoutHandle = setTimeout(() => finalize({ status: timeoutStatus, decodedPayload: null, message: timeoutMessage }), timeoutMs);
});
export const readQRCodeFromClipboard = async (pasteTimeoutMs) => {
    if (typeof window === "undefined" || typeof document === "undefined" || window.isSecureContext !== true) {
        return { status: "UNSUPPORTED", decodedPayload: null, message: "Clipboard APIs require a secure browser context" };
    }
    const immediatePasteResult = await readUsingPasteEvent(250, "NO_PASTE_EVENT", "No immediate paste event");
    if (immediatePasteResult.status !== "NO_PASTE_EVENT") {
        return immediatePasteResult;
    }
    const asyncClipboardResult = await readUsingAsyncClipboardApi();
    if (asyncClipboardResult.status === "UNAVAILABLE" || asyncClipboardResult.status === "UNSUPPORTED") {
        const fallbackTimeoutMs = typeof pasteTimeoutMs === "number" && pasteTimeoutMs > 0 ? pasteTimeoutMs : 10000;
        const pasteFallbackResult = await readUsingPasteEvent(fallbackTimeoutMs, "CANCELED", "Paste event timed out");
        if (pasteFallbackResult.status === "UNSUPPORTED" && asyncClipboardResult.status === "UNSUPPORTED") {
            return {
                status: "UNSUPPORTED",
                decodedPayload: null,
                message: asyncClipboardResult.message ?? pasteFallbackResult.message,
            };
        }
        return pasteFallbackResult;
    }
    return asyncClipboardResult;
};
