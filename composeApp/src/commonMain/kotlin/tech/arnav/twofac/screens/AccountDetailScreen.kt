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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import tech.arnav.twofac.viewmodels.AccountsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: String,
    onNavigateBack: () -> Unit,
    viewModel: AccountsViewModel = koinViewModel()
) {
    var passkeyText by remember { mutableStateOf("") }
    var currentOtp by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleteInProgress by remember { mutableStateOf(false) }

    val accounts by viewModel.accounts.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLibUnlocked = viewModel.twoFacLibUnlocked

    val account = accounts.find { it.accountID == accountId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Details") },
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

            if (isLoading) {
                Text(
                    text = "Loading account details...",
                    style = MaterialTheme.typography.bodyLarge
                )
                return@Column
            }

            if (account != null) {
                Text(
                    text = "Account: ${account.accountLabel}",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (!isLibUnlocked) {
                    OutlinedTextField(
                        value = passkeyText,
                        onValueChange = { passkeyText = it },
                        label = { Text("Passkey") },
                        placeholder = { Text("Enter your passkey") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = {
                        currentOtp = viewModel.getOtpForAccount(accountId)
                    },
                    enabled = isLibUnlocked || passkeyText.isNotBlank()
                ) {
                    Text("Generate OTP")
                }

                currentOtp?.let { otp ->
                    Text(
                        text = "OTP: $otp",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Button(
                    onClick = { showDeleteDialog = true },
                    enabled = !isDeleteInProgress && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete Account")
                }

                error?.let { errorMessage ->
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                Text(
                    text = "Account not found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

        }
    }

    if (showDeleteDialog && account != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDeleteInProgress) {
                    showDeleteDialog = false
                }
            },
            title = { Text("Delete account?") },
            text = {
                Text("This will permanently remove ${account.accountLabel} from your vault.")
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
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    enabled = !isDeleteInProgress
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
