package tech.arnav.twofac.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
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
import twofac.composeapp.generated.resources.add_account_migration_ready
import twofac.composeapp.generated.resources.add_account_migration_progress
import twofac.composeapp.generated.resources.add_account_migration_reset
import twofac.composeapp.generated.resources.add_account_migration_import
import twofac.composeapp.generated.resources.add_account_migration_invalid
import twofac.composeapp.generated.resources.add_account_migration_mismatch
import twofac.composeapp.generated.resources.add_account_migration_partial
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tech.arnav.twofac.components.accounts.AddAccountPasskeyField
import tech.arnav.twofac.components.accounts.InlineErrorMessage
import tech.arnav.twofac.components.accounts.OtpUriInputField
import tech.arnav.twofac.components.accounts.QrImportActions
import tech.arnav.twofac.qr.ComposableCameraQRCodeReader
import tech.arnav.twofac.qr.QRCodeReadResult
import tech.arnav.twofac.qr.GoogleMigrationBatchAccumulator
import tech.arnav.twofac.qr.MigrationBatchProgress
import tech.arnav.twofac.qr.INVALID_OTP_AUTH_URI_REASON
import tech.arnav.twofac.qr.decodedPayloadToQRCodeReadResult
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
    val migrationAccumulator = remember { GoogleMigrationBatchAccumulator() }
    var migrationProgress by remember { mutableStateOf<MigrationBatchProgress?>(null) }
    var pendingUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var partialImport by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    DisposableEffect(migrationAccumulator) {
        onDispose {
            migrationAccumulator.reset()
            pendingUris = emptyList()
            migrationProgress = null
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val requiresUnlock = !viewModel.twoFacLibUnlocked
    val cameraReader = viewModel.cameraQRCodeReader
    val clipboardReader = viewModel.clipboardQRCodeReader
    val composableCameraReader = cameraReader as? ComposableCameraQRCodeReader

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Pre-resolve localizable error strings for use in coroutine lambdas
    val errorClipboardPermission = stringResource(Res.string.add_account_error_clipboard_permission)
    val errorClipboardUnsupported = stringResource(Res.string.add_account_error_clipboard_unsupported)
    val errorClipboardFailed = stringResource(Res.string.add_account_error_clipboard_failed)
    val errorCameraPermission = stringResource(Res.string.add_account_error_camera_permission)
    val errorCameraUnsupported = stringResource(Res.string.add_account_error_camera_unsupported)
    val errorCameraFailed = stringResource(Res.string.add_account_error_camera_failed)
    val errorInvalidPayload = stringResource(Res.string.add_account_migration_invalid)
    val errorMismatchedBatch = stringResource(Res.string.add_account_migration_mismatch)

    fun resetMigration(clearError: Boolean = true) {
        migrationAccumulator.reset()
        migrationProgress = null
        pendingUris = emptyList()
        partialImport = null
        scanError = null
        if (clearError) viewModel.clearError()
    }

    fun acceptScan(result: QRCodeReadResult.Success) {
        if (result.migrationBatch == null) {
            resetMigration()
            uriText = result.otpAuthUris.single()
        } else {
            when (val progress = migrationAccumulator.accept(result.migrationBatch)) {
                MigrationBatchProgress.Rejected -> scanError = errorMismatchedBatch
                else -> {
                    migrationProgress = progress
                    pendingUris = (progress as? MigrationBatchProgress.Complete)?.uris.orEmpty()
                    partialImport = null
                    uriText = ""
                }
            }
        }
    }

    fun triggerClipboardRead(suppressNoImageFailure: Boolean = false) {
        if (clipboardReader == null || isScanning || isPasting || isLoading || pendingUris.isNotEmpty()) return
        isPasting = true
        scanError = null
        coroutineScope.launch {
            try {
                when (val scanResult = clipboardReader.readQRCode()) {
                    is QRCodeReadResult.Success -> {
                        acceptScan(scanResult)
                    }
                    is QRCodeReadResult.DecodeFailure -> {
                        if (!suppressNoImageFailure || !scanResult.reason.isNoClipboardImageReason()) {
                            scanError = if (scanResult.reason == INVALID_OTP_AUTH_URI_REASON) {
                                errorInvalidPayload
                            } else scanResult.reason
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
                    .padding(16.dp)
                    .verticalScroll(scrollState)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { focusManager.clearFocus() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OtpUriInputField(
                    value = uriText,
                    onValueChange = {
                        resetMigration()
                        uriText = it
                    },
                    enabled = !isLoading && !isScanning && !isPasting,
                    isFocused = isUriFieldFocused,
                    onFocusChanged = { isUriFieldFocused = it },
                    onPasteShortcut = {
                        triggerClipboardRead(suppressNoImageFailure = true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )

                migrationProgress?.let { progress ->
                    Text(when (progress) {
                        is MigrationBatchProgress.Awaiting -> stringResource(
                            Res.string.add_account_migration_progress, progress.scanned, progress.total,
                        )
                        is MigrationBatchProgress.Complete -> stringResource(
                            Res.string.add_account_migration_ready, pendingUris.size,
                        )
                        MigrationBatchProgress.Rejected -> errorMismatchedBatch
                    })
                    TextButton(onClick = { resetMigration() }, enabled = !isLoading && !isScanning && !isPasting) {
                        Text(stringResource(Res.string.add_account_migration_reset))
                    }
                }
                partialImport?.let { (imported, failed) ->
                    InlineErrorMessage(stringResource(Res.string.add_account_migration_partial, imported, failed))
                }

                QrImportActions(
                    hasCameraImport = cameraReader != null,
                    hasClipboardImport = clipboardReader != null,
                    isLoading = isLoading || pendingUris.isNotEmpty(),
                    isScanning = isScanning,
                    isPasting = isPasting,
                    onScanWithCamera = {
                        if (cameraReader == null || isScanning || isPasting || isLoading || pendingUris.isNotEmpty()) return@QrImportActions
                        isScanning = true
                        scanError = null
                        coroutineScope.launch {
                            try {
                                when (val scanResult = cameraReader.readQRCode()) {
                                    is QRCodeReadResult.Success -> {
                                        acceptScan(scanResult)
                                    }
                                    is QRCodeReadResult.DecodeFailure -> {
                                        scanError = if (scanResult.reason == INVALID_OTP_AUTH_URI_REASON) {
                                            errorInvalidPayload
                                        } else scanResult.reason
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
                        if (pendingUris.isNotEmpty()) {
                            val submitted = pendingUris
                            viewModel.importAccounts(submitted, passkeyText.ifBlank { null }) { result ->
                                if (result != null) {
                                    pendingUris = result.failedIndices.map(submitted::get)
                                    migrationProgress = MigrationBatchProgress.Complete(pendingUris)
                                    if (pendingUris.isEmpty()) {
                                        resetMigration(clearError = false)
                                        onboardingViewModel.refreshAndSyncDerivedCompletion()
                                        onNavigateBack()
                                    } else {
                                        partialImport = ((partialImport?.first ?: 0) + result.importedIndices.size) to pendingUris.size
                                    }
                                }
                            }
                        } else if (uriText.trim().startsWith("otpauth-migration://", ignoreCase = true)) {
                            // Manual paste follows the same preview/batch flow as camera and clipboard QR.
                            when (val result = decodedPayloadToQRCodeReadResult(uriText)) {
                                is QRCodeReadResult.Success -> acceptScan(result)
                                else -> scanError = errorInvalidPayload
                            }
                        } else if (uriText.isNotBlank() && (!requiresUnlock || passkeyText.isNotBlank())) {
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
                    enabled = !isLoading && !isScanning && !isPasting &&
                        (uriText.isNotBlank() || pendingUris.isNotEmpty()) &&
                        (!requiresUnlock || passkeyText.isNotBlank())
                ) {
                    Text(when {
                        isLoading -> stringResource(Res.string.add_account_adding)
                        pendingUris.isNotEmpty() -> stringResource(Res.string.add_account_migration_import, pendingUris.size)
                        else -> stringResource(Res.string.accounts_add_account)
                    })
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
