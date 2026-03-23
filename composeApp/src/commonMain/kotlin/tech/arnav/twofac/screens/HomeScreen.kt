package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import tech.arnav.twofac.components.home.HomeEmptyState
import tech.arnav.twofac.components.home.HomeLoadingState
import tech.arnav.twofac.components.home.HomeLockedState
import tech.arnav.twofac.components.otp.HomeOtpListSection
import tech.arnav.twofac.components.security.PasskeyDialog
import tech.arnav.twofac.viewmodels.AccountsViewModel

@Composable
fun HomeScreen(
    onNavigateToAccounts: () -> Unit,
    viewModel: AccountsViewModel = koinViewModel(),
) {
    val accounts by viewModel.accounts.collectAsState()
    val accountOtps by viewModel.accountOtps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val otpListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showPasskeyDialog by remember { mutableStateOf(false) }
    var hasTriggeredUnlockFlow by remember { mutableStateOf(false) }
    val isUnlocked = viewModel.twoFacLibUnlocked
    // Snapshot at composition time — doesn't change after page load.
    val isWebAuthnReady = remember { viewModel.isWebAuthnUnlockReady() }

    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }

    LaunchedEffect(isLoading, hasTriggeredUnlockFlow, isUnlocked) {
        if (!isLoading && !hasTriggeredUnlockFlow && !isUnlocked) {
            hasTriggeredUnlockFlow = true
            if (!isWebAuthnReady) {
                // No WebAuthn enrolled — go straight to manual passkey dialog.
                showPasskeyDialog = true
            }
            // When WebAuthn is enrolled the locked state shows a button;
            // the user must tap it to trigger the biometric prompt.
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            showPasskeyDialog = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            isLoading -> {
                HomeLoadingState()
            }

            accounts.isEmpty() -> {
                HomeEmptyState(onManageAccounts = onNavigateToAccounts)
            }

            !isUnlocked -> {
                HomeLockedState(
                    onWebAuthnUnlock = if (isWebAuthnReady) {
                        {
                            coroutineScope.launch {
                                val savedPasskey = viewModel.getSavedPasskey()
                                if (savedPasskey != null) {
                                    viewModel.loadAccountsWithOtps(savedPasskey, fromAutoUnlock = true)
                                } else {
                                    // WebAuthn failed or was cancelled — fall back to manual entry.
                                    showPasskeyDialog = true
                                }
                            }
                        }
                    } else null,
                    onManualUnlock = { showPasskeyDialog = true },
                )
            }

            else -> {
                HomeOtpListSection(
                    accountsWithOtps = accountOtps,
                    listState = otpListState,
                    onRefreshOtp = {
                        viewModel.refreshOtps()
                    },
                )
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
        },
    )
}
