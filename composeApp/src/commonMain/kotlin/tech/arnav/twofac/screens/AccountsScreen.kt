package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import tech.arnav.twofac.components.accounts.AccountsErrorState
import tech.arnav.twofac.components.accounts.AccountsListContent
import tech.arnav.twofac.components.accounts.AccountsLockedState
import tech.arnav.twofac.components.security.PasskeyDialog
import tech.arnav.twofac.viewmodels.AccountsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateToAddAccount: () -> Unit,
    onNavigateToAccountDetail: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: AccountsViewModel = koinViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showPasskeyDialog by remember { mutableStateOf(false) }
    val requiresUnlock = !viewModel.twoFacLibUnlocked
    val isWebAuthnReady = remember { viewModel.isWebAuthnUnlockReady() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(requiresUnlock) {
        if (requiresUnlock && !isWebAuthnReady) {
            showPasskeyDialog = true
        } else {
            showPasskeyDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    onNavigateBack?.let { navigateBack ->
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddAccount) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Account")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                requiresUnlock -> {
                    AccountsLockedState(
                        onUnlockClick = if (isWebAuthnReady) {
                            {
                                coroutineScope.launch {
                                    val savedPasskey = viewModel.getSavedPasskey()
                                    if (savedPasskey != null) {
                                        viewModel.loadAccountsWithOtps(savedPasskey, fromAutoUnlock = true)
                                    } else {
                                        showPasskeyDialog = true
                                    }
                                }
                            }
                        } else {
                            { showPasskeyDialog = true }
                        },
                    )
                }

                error != null -> {
                    AccountsErrorState(
                        error = error.orEmpty()
                    )
                }

                else -> {
                    AccountsListContent(
                        accounts = accounts,
                        onAccountClick = onNavigateToAccountDetail,
                    )
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
