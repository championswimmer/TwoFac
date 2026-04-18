package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tech.arnav.twofac.components.accounts.AccountsErrorState
import tech.arnav.twofac.components.accounts.AccountsListContent
import tech.arnav.twofac.components.accounts.AccountsLockedState
import tech.arnav.twofac.components.security.PasskeyDialog
import tech.arnav.twofac.viewmodels.AccountsViewModel
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.accounts_add_account
import twofac.composeapp.generated.resources.accounts_search_placeholder
import twofac.composeapp.generated.resources.accounts_title
import twofac.composeapp.generated.resources.action_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateToAddAccount: () -> Unit,
    onNavigateToAccountDetail: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: AccountsViewModel = koinInject()
) {
    val accounts by viewModel.accounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showPasskeyDialog by remember { mutableStateOf(false) }
    val requiresUnlock = !viewModel.twoFacLibUnlocked
    val isSecureUnlockReady = remember { viewModel.isSecureUnlockReady() }
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(requiresUnlock) {
        if (requiresUnlock && !isSecureUnlockReady) {
            showPasskeyDialog = true
        } else {
            showPasskeyDialog = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.accounts_title)) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    onNavigateBack?.let { navigateBack ->
                        IconButton(onClick = navigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddAccount) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(Res.string.accounts_add_account))
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
                        onUnlockClick = if (isSecureUnlockReady) {
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
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(Res.string.accounts_search_placeholder)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                    AccountsListContent(
                        accounts = accounts,
                        onAccountClick = onNavigateToAccountDetail,
                        searchQuery = searchQuery,
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
