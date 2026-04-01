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
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.action_back
import twofac.composeapp.generated.resources.add_account_title
import twofac.composeapp.generated.resources.add_account_adding
import twofac.composeapp.generated.resources.accounts_add_account
import twofac.composeapp.generated.resources.add_account_error_clipboard_permission
import twofac.composeapp.generated.resources.add_account_error_clipboard_unsupported
import twofac.composeapp.generated.resources.add_account_error_clipboard_failed
import twofac.composeapp.generated.resources.add_account_error_camera_permission
import twofac.composeapp.generated.resources.add_account_error_camera_unsupported
import twofac.composeapp.generated.resources.add_account_error_camera_failed
import twofac.composeapp.generated.resources.add_account_error_qr_prefix
import twofac.composeapp.generated.resources.error_prefix
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tech.arnav.twofac.components.accounts.AddAccountPasskeyField
import tech.arnav.twofac.components.accounts.InlineErrorMessage
import tech.arnav.twofac.components.accounts.OtpUriInputField
import tech.arnav.twofac.components.accounts.QrImportActions
import tech.arnav.twofac.qr.ComposableCameraQRCodeReader
import tech.arnav.twofac.qr.QRCodeReadResult
import tech.arnav.twofac.viewmodels.AccountsViewModel
import tech.arnav.twofac.viewmodels.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountsViewModel = koinInject(),
    onboardingViewModel: OnboardingViewModel = koinInject(),
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

    // Pre-resolve localizable error strings for use in coroutine lambdas
    val errorClipboardPermission = stringResource(Res.string.add_account_error_clipboard_permission)
    val errorClipboardUnsupported = stringResource(Res.string.add_account_error_clipboard_unsupported)
    val errorClipboardFailed = stringResource(Res.string.add_account_error_clipboard_failed)
    val errorCameraPermission = stringResource(Res.string.add_account_error_camera_permission)
    val errorCameraUnsupported = stringResource(Res.string.add_account_error_camera_unsupported)
    val errorCameraFailed = stringResource(Res.string.add_account_error_camera_failed)

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
                        scanError = errorClipboardPermission
                    }
                    QRCodeReadResult.Unsupported -> {
                        scanError = errorClipboardUnsupported
                    }
                    QRCodeReadResult.Canceled -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                scanError = e.message ?: errorClipboardFailed
            } finally {
                isPasting = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.add_account_title)) },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.action_back))
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
                                        scanError = errorCameraPermission
                                    }
                                    QRCodeReadResult.Unsupported -> {
                                        scanError = errorCameraUnsupported
                                    }
                                    QRCodeReadResult.Canceled -> Unit
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                scanError = e.message ?: errorCameraFailed
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
                        message = stringResource(Res.string.add_account_error_qr_prefix, scanErrorMessage),
                    )
                }

                error?.let { errorMessage ->
                    InlineErrorMessage(
                        message = stringResource(Res.string.error_prefix, errorMessage),
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
                                        onboardingViewModel.refreshAndSyncDerivedCompletion()
                                        onNavigateBack()
                                    }
                                }
                            )
                        }
                    },
                    enabled = !isLoading && uriText.isNotBlank() && (!requiresUnlock || passkeyText.isNotBlank())
                ) {
                    Text(if (isLoading) stringResource(Res.string.add_account_adding) else stringResource(Res.string.accounts_add_account))
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
