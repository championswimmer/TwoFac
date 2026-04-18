package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import twofac.composeapp.generated.resources.account_detail_title
import twofac.composeapp.generated.resources.action_back
import twofac.composeapp.generated.resources.account_detail_loading
import twofac.composeapp.generated.resources.account_detail_account_label
import twofac.composeapp.generated.resources.label_passkey
import twofac.composeapp.generated.resources.label_passkey_placeholder
import twofac.composeapp.generated.resources.account_detail_generate_otp
import twofac.composeapp.generated.resources.account_detail_otp_display
import twofac.composeapp.generated.resources.account_detail_delete
import twofac.composeapp.generated.resources.error_prefix
import twofac.composeapp.generated.resources.account_detail_not_found
import twofac.composeapp.generated.resources.account_detail_delete_dialog_title
import twofac.composeapp.generated.resources.account_detail_delete_dialog_message
import twofac.composeapp.generated.resources.action_delete
import twofac.composeapp.generated.resources.action_cancel
import twofac.composeapp.generated.resources.account_detail_show_qr
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tech.arnav.twofac.components.qr.QRCodeDialog
import tech.arnav.twofac.lib.otp.OtpCodes
import tech.arnav.twofac.viewmodels.AccountsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    onNavigateBack: () -> Unit,
    viewModel: AccountsViewModel = koinInject()
) {
    var passkeyText by remember { mutableStateOf("") }
    var currentOtp by remember { mutableStateOf<OtpCodes?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleteInProgress by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var qrOtpAuthUri by remember { mutableStateOf<String?>(null) }

    val accounts by viewModel.accounts.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLibUnlocked = viewModel.twoFacLibUnlocked
    val coroutineScope = rememberCoroutineScope()

    val account = accounts.find { it.accountID == accountId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.account_detail_title)) },
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

            if (isLoading) {
                Text(
                    text = stringResource(Res.string.account_detail_loading),
                    style = MaterialTheme.typography.bodyLarge
                )
                return@Column
            }

            if (account != null) {
                Text(
                    text = stringResource(Res.string.account_detail_account_label, account.accountLabel),
                    style = MaterialTheme.typography.bodyLarge
                )

                if (!isLibUnlocked) {
                    OutlinedTextField(
                        value = passkeyText,
                        onValueChange = { passkeyText = it },
                        label = { Text(stringResource(Res.string.label_passkey)) },
                        placeholder = { Text(stringResource(Res.string.label_passkey_placeholder)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            currentOtp = viewModel.getFreshOtpForAccount(accountId)
                        }
                    },
                    enabled = isLibUnlocked
                ) {
                    Text(stringResource(Res.string.account_detail_generate_otp))
                }

                currentOtp?.let { otp ->
                    Text(
                        text = stringResource(Res.string.account_detail_otp_display, otp.currentOTP),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            qrOtpAuthUri = viewModel.getOtpAuthUriForAccount(accountId)
                            if (qrOtpAuthUri != null) {
                                showQrDialog = true
                            }
                        }
                    },
                    enabled = isLibUnlocked
                ) {
                    Text(stringResource(Res.string.account_detail_show_qr))
                }

                Button(
                    onClick = { showDeleteDialog = true },
                    enabled = !isDeleteInProgress && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(Res.string.account_detail_delete))
                }

                error?.let { errorMessage ->
                    Text(
                        text = stringResource(Res.string.error_prefix, errorMessage),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                Text(
                    text = stringResource(Res.string.account_detail_not_found),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

        }
    }

    if (showQrDialog && qrOtpAuthUri != null) {
        QRCodeDialog(
            otpAuthUri = qrOtpAuthUri!!,
            onDismiss = {
                showQrDialog = false
                qrOtpAuthUri = null
            },
        )
    }

    if (showDeleteDialog && account != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleteInProgress) {
                    showDeleteDialog = false
                }
            },
            title = { Text(stringResource(Res.string.account_detail_delete_dialog_title)) },
            text = {
                Text(stringResource(Res.string.account_detail_delete_dialog_message, account.accountLabel))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleteInProgress = true
                        viewModel.deleteAccount(accountId) { success ->
                            isDeleteInProgress = false
                            showDeleteDialog = false
                            if (success) {
                                onNavigateBack()
                            }
                        }
                    },
                    enabled = !isDeleteInProgress
                ) {
                    Text(stringResource(Res.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleteInProgress
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }
}
