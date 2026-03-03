package tech.arnav.twofac.qr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSProcessInfo
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
class IosCameraQRCodeReader : ComposableCameraQRCodeReader {
    private val pendingScanResult = MutableStateFlow<CompletableDeferred<QRCodeReadResult>?>(null)

    override suspend fun readQRCode(): QRCodeReadResult {
        if (isRunningOnSimulator()) {
            return QRCodeReadResult.Unsupported
        }
        if (!ensureCameraAccess()) {
            return QRCodeReadResult.PermissionDenied
        }

        val scanResult = CompletableDeferred<QRCodeReadResult>()
        if (!pendingScanResult.compareAndSet(null, scanResult)) {
            return QRCodeReadResult.Canceled
        }
        return try {
            scanResult.await()
        } finally {
            pendingScanResult.compareAndSet(scanResult, null)
        }
    }

    @Composable
    override fun RenderScanner(modifier: Modifier) {
        val activeScan by pendingScanResult.collectAsState()
        val scan = activeScan ?: return

        ScannerView(
            modifier = modifier,
            codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
        ) { result ->
            if (pendingScanResult.value !== scan || scan.isCompleted) {
                return@ScannerView
            }

            val mappedResult = when (result) {
                is BarcodeResult.OnSuccess -> mapScannedBarcode(result.barcode)
                is BarcodeResult.OnFailed ->
                    QRCodeReadResult.DecodeFailure(
                        result.exception.message ?: "Failed to scan QR code",
                    )
                BarcodeResult.OnCanceled -> QRCodeReadResult.Canceled
            }

            scan.complete(mappedResult)
            pendingScanResult.compareAndSet(scan, null)
        }
    }

    private suspend fun ensureCameraAccess(): Boolean {
        return when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> true
            AVAuthorizationStatusNotDetermined -> requestCameraAccess()
            else -> false
        }
    }

    private suspend fun requestCameraAccess(): Boolean = suspendCoroutine { continuation ->
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted: Boolean ->
            continuation.resume(granted)
        }
    }

    private fun mapScannedBarcode(barcode: Barcode): QRCodeReadResult =
        QRCodeUtils.decodedPayloadCandidatesToQRCodeReadResult(
            primaryPayload = barcode.data,
            rawBytes = barcode.rawBytes,
        )

    private fun isRunningOnSimulator(): Boolean {
        return NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null
    }
}
