package tech.arnav.twofac.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.awt.GraphicsEnvironment
import java.awt.HeadlessException
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.IOException

class DesktopClipboardQRCodeReader : ClipboardQRCodeReader {
    override suspend fun readQRCode(): QRCodeReadResult {
        if (GraphicsEnvironment.isHeadless()) {
            return QRCodeReadResult.Unsupported
        }

        val clipboard = try {
            Toolkit.getDefaultToolkit().systemClipboard
        } catch (_: HeadlessException) {
            return QRCodeReadResult.Unsupported
        } catch (_: UnsupportedOperationException) {
            return QRCodeReadResult.Unsupported
        }

        if (!clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
            return QRCodeReadResult.DecodeFailure("Clipboard does not contain an image")
        }

        val image = try {
            clipboard.getData(DataFlavor.imageFlavor) as? Image
                ?: return QRCodeReadResult.DecodeFailure("Clipboard image data is unavailable")
        } catch (_: HeadlessException) {
            return QRCodeReadResult.Unsupported
        } catch (_: UnsupportedOperationException) {
            return QRCodeReadResult.Unsupported
        } catch (_: IllegalStateException) {
            return QRCodeReadResult.DecodeFailure("Clipboard is currently unavailable")
        } catch (_: UnsupportedFlavorException) {
            return QRCodeReadResult.DecodeFailure("Clipboard does not contain a readable image")
        } catch (_: IOException) {
            return QRCodeReadResult.DecodeFailure("Failed to read image from clipboard")
        }

        val bufferedImage = image.toBufferedImageOrNull()
            ?: return QRCodeReadResult.DecodeFailure("Clipboard image has invalid dimensions")

        val decodeResult = try {
            MultiFormatReader().decode(
                BinaryBitmap(
                    HybridBinarizer(BufferedImageLuminanceSource(bufferedImage)),
                ),
            )
        } catch (_: NotFoundException) {
            return QRCodeReadResult.DecodeFailure("No QR code found in clipboard image")
        } catch (e: Exception) {
            return QRCodeReadResult.DecodeFailure(
                e.message ?: "Failed to decode QR code from clipboard image",
            )
        }

        return QRCodeUtils.decodedPayloadCandidatesToQRCodeReadResult(
            primaryPayload = decodeResult.text,
            rawBytes = decodeResult.rawBytes,
        )
    }
}

private fun Image.toBufferedImageOrNull(): BufferedImage? {
    if (this is BufferedImage) return this

    val width = getWidth(null)
    val height = getHeight(null)
    if (width <= 0 || height <= 0) return null

    return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { bufferedImage ->
        val graphics = bufferedImage.createGraphics()
        try {
            graphics.drawImage(this, 0, 0, null)
        } finally {
            graphics.dispose()
        }
    }
}
