package tech.arnav.twofac.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ComposableCameraQRCodeReader : CameraQRCodeReader {
    @Composable
    fun RenderScanner(modifier: Modifier = Modifier)
}
