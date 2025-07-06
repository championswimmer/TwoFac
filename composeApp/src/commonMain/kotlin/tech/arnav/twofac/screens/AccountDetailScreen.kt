package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

    val accounts by viewModel.accounts.collectAsState()
    val error by viewModel.error.collectAsState()

    val account = accounts.find { it.accountID == accountId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

        account?.let { acc ->
            Text(
                text = "Account: ${acc.accountLabel}",
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = passkeyText,
                onValueChange = { passkeyText = it },
                label = { Text("Passkey") },
                placeholder = { Text("Enter your passkey") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (passkeyText.isNotBlank()) {
                        currentOtp = viewModel.getOtpForAccount(accountId, passkeyText)
                    }
                },
                enabled = passkeyText.isNotBlank()
            ) {
                Text("Generate OTP")
            }

            currentOtp?.let { otp ->
                Text(
                    text = "OTP: $otp",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            error?.let { errorMessage ->
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } ?: run {
            Text(
                text = "Account not found",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        }
    }
}