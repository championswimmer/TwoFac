package tech.arnav.twofac.qr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume

class AndroidCameraQRCodeReader : ComposableCameraQRCodeReader {
    private data class PendingScan(
        val id: Long,
        val continuation: kotlinx.coroutines.CancellableContinuation<QRCodeReadResult>,
    )

    private val pendingScan = MutableStateFlow<PendingScan?>(null)
    private val scanIdGenerator = AtomicLong(0L)

    override suspend fun readQRCode(): QRCodeReadResult = suspendCancellableCoroutine { continuation ->
        val scan = PendingScan(
            id = scanIdGenerator.incrementAndGet(),
            continuation = continuation,
        )
        if (!pendingScan.compareAndSet(null, scan)) {
            continuation.resume(QRCodeReadResult.Canceled)
            return@suspendCancellableCoroutine
        }

        continuation.invokeOnCancellation {
            pendingScan.compareAndSet(scan, null)
        }
    }

    @Composable
    override fun RenderScanner(modifier: Modifier) {
        val scan by pendingScan.collectAsState()
        val activeScan = scan ?: return
        val context = LocalContext.current
        val hasCameraPermission = remember(activeScan.id) {
            mutableStateOf(hasPermission(context))
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            hasCameraPermission.value = granted
            if (!granted) {
                finishScan(activeScan, QRCodeReadResult.PermissionDenied)
            }
        }

        LaunchedEffect(activeScan.id, hasCameraPermission.value) {
            if (!hasCameraPermission.value) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        if (!hasCameraPermission.value) return

        ScannerView(
            modifier = modifier,
            codeTypes = listOf(BarcodeFormat.FORMAT_QR_CODE),
        ) { result ->
            when (result) {
                is BarcodeResult.OnSuccess ->
                    finishScan(
                        activeScan,
                        QRCodeUtils.decodedPayloadCandidatesToQRCodeReadResult(
                            primaryPayload = result.barcode.data,
                            rawBytes = result.barcode.rawBytes,
                        ),
                    )

                is BarcodeResult.OnFailed ->
                    finishScan(
                        activeScan,
                        QRCodeReadResult.DecodeFailure(
                            result.exception.message ?: "Failed to decode QR code",
                        ),
                    )

                BarcodeResult.OnCanceled ->
                    finishScan(activeScan, QRCodeReadResult.Canceled)
            }
        }
    }

    private fun finishScan(scan: PendingScan, result: QRCodeReadResult) {
        if (!pendingScan.compareAndSet(scan, null)) return
        if (scan.continuation.isActive) {
            scan.continuation.resume(result)
        }
    }

    private fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
}
