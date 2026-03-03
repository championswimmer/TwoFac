package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
    val coroutineScope = rememberCoroutineScope()
    val requiresUnlock = !viewModel.twoFacLibUnlocked
    val cameraReader = viewModel.cameraQRCodeReader
    val composableCameraReader = cameraReader as? ComposableCameraQRCodeReader

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Account") },
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
                OutlinedTextField(
                    value = uriText,
                    onValueChange = { uriText = it },
                    label = { Text("2FA URI") },
                    placeholder = { Text("otpauth://totp/...") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (cameraReader != null) {
                    Button(
                        onClick = {
                            if (isScanning) return@Button
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
                        enabled = !isLoading && !isScanning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isScanning) "Scanning..." else "Scan QR with Camera")
                    }
                }

                if (requiresUnlock) {
                    OutlinedTextField(
                        value = passkeyText,
                        onValueChange = { passkeyText = it },
                        label = { Text("Passkey") },
                        placeholder = { Text("Enter your passkey") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                scanError?.let { scanErrorMessage ->
                    Text(
                        text = "Scan error: $scanErrorMessage",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                error?.let { errorMessage ->
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
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
