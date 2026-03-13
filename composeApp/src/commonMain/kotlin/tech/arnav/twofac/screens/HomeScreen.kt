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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    var showPasskeyDialog by remember { mutableStateOf(false) }
    var hasTriggeredUnlockFlow by remember { mutableStateOf(false) }
    var autoUnlockAttempted by remember { mutableStateOf(false) }
    val isUnlocked = viewModel.twoFacLibUnlocked

    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }

    LaunchedEffect(isLoading, hasTriggeredUnlockFlow, isUnlocked) {
        if (!isLoading && !hasTriggeredUnlockFlow && !isUnlocked) {
            hasTriggeredUnlockFlow = true
            // Try auto-unlock from saved session passkey first
            val savedPasskey = viewModel.getSavedPasskey()
            if (savedPasskey != null) {
                viewModel.loadAccountsWithOtps(savedPasskey, fromAutoUnlock = true)
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
                HomeLockedState()
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
