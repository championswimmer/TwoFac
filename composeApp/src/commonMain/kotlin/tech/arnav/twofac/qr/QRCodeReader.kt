package tech.arnav.twofac.qr

/**
 * Common QR reader contract resolved through optional DI.
 *
 * Current platform stack:
 * - Android + iOS camera readers: KScan
 * - Desktop clipboard-image reader: ZXing
 * - Web/Wasm clipboard-image reader: jsQR
 */
interface QRCodeReader {
    suspend fun readQRCode(): QRCodeReadResult
}

interface CameraQRCodeReader : QRCodeReader

interface ClipboardQRCodeReader : QRCodeReader
