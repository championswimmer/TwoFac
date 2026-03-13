package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import tech.arnav.twofac.components.accounts.AddAccountPasskeyField
import tech.arnav.twofac.components.accounts.InlineErrorMessage
import tech.arnav.twofac.components.accounts.OtpUriInputField
import tech.arnav.twofac.components.accounts.QrImportActions
import tech.arnav.twofac.qr.ComposableCameraQRCodeReader
import tech.arnav.twofac.qr.QRCodeReadResult
import tech.arnav.twofac.viewmodels.AccountsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountsViewModel = koinViewModel()
) {
    var uriText by remember { mutableStateOf("") }
    var passkeyText by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var isPasting by remember { mutableStateOf(false) }
    var isUriFieldFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val requiresUnlock = !viewModel.twoFacLibUnlocked
    val cameraReader = viewModel.cameraQRCodeReader
    val clipboardReader = viewModel.clipboardQRCodeReader
    val composableCameraReader = cameraReader as? ComposableCameraQRCodeReader

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    fun triggerClipboardRead(suppressNoImageFailure: Boolean = false) {
        if (clipboardReader == null || isScanning || isPasting) return
        isPasting = true
        scanError = null
        coroutineScope.launch {
            try {
                when (val scanResult = clipboardReader.readQRCode()) {
                    is QRCodeReadResult.Success -> {
                        uriText = scanResult.otpAuthUri
                    }
                    is QRCodeReadResult.DecodeFailure -> {
                        if (!suppressNoImageFailure || !scanResult.reason.isNoClipboardImageReason()) {
                            scanError = scanResult.reason
                        }
                    }
                    QRCodeReadResult.PermissionDenied -> {
                        scanError = "Clipboard permission denied"
                    }
                    QRCodeReadResult.Unsupported -> {
                        scanError = "Clipboard QR reading is not supported on this platform"
                    }
                    QRCodeReadResult.Canceled -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                scanError = e.message ?: "Failed to read QR code from clipboard"
            } finally {
                isPasting = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Account") },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                OtpUriInputField(
                    value = uriText,
                    onValueChange = { uriText = it },
                    isFocused = isUriFieldFocused,
                    onFocusChanged = { isUriFieldFocused = it },
                    onPasteShortcut = {
                        triggerClipboardRead(suppressNoImageFailure = true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )

                QrImportActions(
                    hasCameraImport = cameraReader != null,
                    hasClipboardImport = clipboardReader != null,
                    isLoading = isLoading,
                    isScanning = isScanning,
                    isPasting = isPasting,
                    onScanWithCamera = {
                        if (cameraReader == null || isScanning || isPasting) return@QrImportActions
                        isScanning = true
                        scanError = null
                        coroutineScope.launch {
                            try {
                                when (val scanResult = cameraReader.readQRCode()) {
                                    is QRCodeReadResult.Success -> {
                                        uriText = scanResult.otpAuthUri
                                    }
                                    is QRCodeReadResult.DecodeFailure -> {
                                        scanError = scanResult.reason
                                    }
                                    QRCodeReadResult.PermissionDenied -> {
                                        scanError = "Camera permission denied"
                                    }
                                    QRCodeReadResult.Unsupported -> {
                                        scanError = "Camera QR scanning is not supported on this platform"
                                    }
                                    QRCodeReadResult.Canceled -> Unit
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                scanError = e.message ?: "Failed to scan QR code"
                            } finally {
                                isScanning = false
                            }
                        }
                    },
                    onPasteFromClipboard = {
                        triggerClipboardRead()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (requiresUnlock) {
                    AddAccountPasskeyField(
                        value = passkeyText,
                        onValueChange = { passkeyText = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                scanError?.let { scanErrorMessage ->
                    InlineErrorMessage(
                        message = "QR error: $scanErrorMessage",
                    )
                }

                error?.let { errorMessage ->
                    InlineErrorMessage(
                        message = "Error: $errorMessage",
                    )
                }

                Button(
                    onClick = {
                        if (uriText.isNotBlank() && (!requiresUnlock || passkeyText.isNotBlank())) {
                            viewModel.addAccount(
                                uri = uriText,
                                passkey = passkeyText.ifBlank { null },
                                onComplete = { success ->
                                    if (success) {
                                        onNavigateBack()
                                    }
                                }
                            )
                        }
                    },
                    enabled = !isLoading && uriText.isNotBlank() && (!requiresUnlock || passkeyText.isNotBlank())
                ) {
                    Text(if (isLoading) "Adding..." else "Add Account")
                }
            }
        }
        composableCameraReader?.RenderScanner(modifier = Modifier.fillMaxSize())
    }
}

private fun String.isNoClipboardImageReason(): Boolean {
    val normalized = lowercase()
    return normalized.contains("no image") || normalized.contains("does not contain an image")
}
