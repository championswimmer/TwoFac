package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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

@Composable
fun AddAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountsViewModel = koinViewModel()
) {
    var uriText by remember { mutableStateOf("") }
    var passkeyText by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Add Account",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = uriText,
            onValueChange = { uriText = it },
            label = { Text("2FA URI") },
            placeholder = { Text("otpauth://totp/...") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = passkeyText,
            onValueChange = { passkeyText = it },
            label = { Text("Passkey") },
            placeholder = { Text("Enter your passkey") },
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { errorMessage ->
            Text(
                text = "Error: $errorMessage",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        Button(
            onClick = {
                if (uriText.isNotBlank() && passkeyText.isNotBlank()) {
                    viewModel.addAccount(uriText, passkeyText)
                    if (viewModel.error.value == null) {
                        onNavigateBack()
                    }
                }
            },
            enabled = !isLoading && uriText.isNotBlank() && passkeyText.isNotBlank()
        ) {
            Text(if (isLoading) "Adding..." else "Add Account")
        }
        
        Button(onClick = onNavigateBack) {
            Text("Back")
        }
    }
}