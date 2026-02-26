package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import tech.arnav.twofac.components.OTPCard
import tech.arnav.twofac.components.PasskeyDialog
import tech.arnav.twofac.viewmodels.AccountsViewModel

@Composable
fun HomeScreen(
    onNavigateToAccounts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: AccountsViewModel = koinViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val accountOtps by viewModel.accountOtps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showPasskeyDialog by remember { mutableStateOf(false) }
    var hasTriggeredUnlockFlow by remember { mutableStateOf(false) }
    var autoUnlockAttempted by remember { mutableStateOf(false) }
    val isUnlocked = viewModel.twoFacLibUnlocked

    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }

    LaunchedEffect(isLoading) {
        if (!isLoading && !hasTriggeredUnlockFlow && !isUnlocked) {
            hasTriggeredUnlockFlow = true
            // Try auto-unlock from saved session passkey first
            val savedPasskey = viewModel.getSavedPasskey()
            if (savedPasskey != null) {
                viewModel.loadAccountsWithOtps(savedPasskey)
                autoUnlockAttempted = true
            } else {
                showPasskeyDialog = true
            }
        }
    }

    // If auto-unlock failed (error set while we tried), clear saved passkey and show the passkey dialog
    LaunchedEffect(error) {
        if (autoUnlockAttempted && error != null && !viewModel.twoFacLibUnlocked) {
            autoUnlockAttempted = false
            viewModel.clearSavedPasskey()
            showPasskeyDialog = true
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            showPasskeyDialog = false
        }
    }

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                Button(
                    onClick = onNavigateToAccounts
                ) {
                    Text("Manage Accounts")
                }

                Button(
                    onClick = onNavigateToSettings
                ) {
                    Text("Settings")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // Show loading state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "TwoFac",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Two-Factor Authentication Manager",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        CircularProgressIndicator()
                    }
                }

                accounts.isEmpty() -> {
                    // Show empty state - no accounts exist
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "🔐",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "No accounts added",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Go to manage accounts to add an account",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = onNavigateToAccounts,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Manage Accounts")
                        }
                    }
                }

                !isUnlocked -> {
                    // Show welcome screen before unlock
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "TwoFac",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Two-Factor Authentication Manager",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                else -> {
                    // Show OTP cards
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Your Accounts",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(accountOtps) { (account, otpCode) ->
                            OTPCard(
                                account = account,
                                otpCode = otpCode,
                                timeInterval = 30L, // Default TOTP interval
                                onRefreshOTP = {
                                    viewModel.refreshOtps()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    PasskeyDialog(
        isVisible = showPasskeyDialog,
        isLoading = isLoading,
        error = error,
        onPasskeySubmit = { passkey ->
            viewModel.loadAccountsWithOtps(passkey)
        },
        onDismiss = {
            showPasskeyDialog = false
        }
    )
}
