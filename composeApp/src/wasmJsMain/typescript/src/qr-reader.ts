interface DecodeResult {
  status: string;
  payload: string | null;
  message: string | null;
}

async function importJsQr(): Promise<typeof import("jsqr").default | null> {
  const jsQrModule: any = await import("jsqr");
  return jsQrModule && jsQrModule.default ? jsQrModule.default : jsQrModule;
}

async function decodeBlobWithJsQr(blob: Blob | null): Promise<DecodeResult> {
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

  let imageBitmap: ImageBitmap | null = null;
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
  } catch (error: unknown) {
    const name = (error instanceof Error && error.name) || "DecodeError";
    return { status: "DECODE_FAILURE", payload: null, message: name };
  } finally {
    if (imageBitmap && typeof imageBitmap.close === "function") {
      imageBitmap.close();
    }
  }
}

async function decodeFirstImageBlob(blobPromises: Promise<Blob | null>[]): Promise<DecodeResult> {
  if (!blobPromises || blobPromises.length === 0) {
    return { status: "NO_IMAGE", payload: null, message: "No image found in clipboard" };
  }

  let hadImage = false;
  for (let index = 0; index < blobPromises.length; index++) {
    let blob: Blob | null = null;
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
}

function extractImageBlobPromisesFromClipboardItems(clipboardItems: ClipboardItems): Promise<Blob | null>[] {
  const blobPromises: Promise<Blob | null>[] = [];
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
}

function extractImageBlobPromisesFromPasteData(clipboardData: DataTransfer | null): Promise<Blob | null>[] {
  const blobPromises: Promise<Blob | null>[] = [];
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
}

async function readUsingAsyncClipboardApi(): Promise<DecodeResult> {
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
  } catch (error: unknown) {
    const name = (error instanceof Error && error.name) || "ClipboardReadError";
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
}

function readUsingPasteEvent(
  timeoutMs: number,
  timeoutStatus: string,
  timeoutMessage: string,
): Promise<DecodeResult> {
  return new Promise((resolve) => {
    if (
      typeof window.addEventListener !== "function" ||
      typeof window.removeEventListener !== "function"
    ) {
      resolve({ status: "UNSUPPORTED", payload: null, message: "Paste event API is unavailable" });
      return;
    }

    let completed = false;
    let timeoutHandle: ReturnType<typeof setTimeout> | null = null;

    const finalize = (result: DecodeResult) => {
      if (completed) return;
      completed = true;
      if (timeoutHandle != null) {
        clearTimeout(timeoutHandle);
      }
      window.removeEventListener("paste", onPaste, true);
      resolve(result);
    };

    const onPaste = (event: Event) => {
      const clipboardEvent = event as ClipboardEvent;
      const clipboardData = clipboardEvent && clipboardEvent.clipboardData ? clipboardEvent.clipboardData : null;
      decodeFirstImageBlob(extractImageBlobPromisesFromPasteData(clipboardData))
        .then(finalize)
        .catch((error: unknown) => {
          const name = (error instanceof Error && error.name) || "PasteDecodeError";
          finalize({ status: "DECODE_FAILURE", payload: null, message: name });
        });
    };

    window.addEventListener("paste", onPaste, true);
    timeoutHandle = setTimeout(
      () => finalize({ status: timeoutStatus, payload: null, message: timeoutMessage }),
      timeoutMs,
    );
  });
}

export function readQRCodeFromClipboard(
  pasteTimeoutMs: number,
  onResult: (status: string, payload: string | null, message: string | null) => void,
): void {
  const finishOnce = (() => {
    let finished = false;
    return (status: string, payload: string | null, message: string | null) => {
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
  })().catch((error: unknown) => {
    const name = (error instanceof Error && error.name) || "ClipboardReadFailed";
    finishOnce("DECODE_FAILURE", null, name);
  });
}
