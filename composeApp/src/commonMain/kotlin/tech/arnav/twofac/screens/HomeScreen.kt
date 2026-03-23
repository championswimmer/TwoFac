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
import tech.arnav.twofac.onboarding.OnboardingAutoShowDecision
import tech.arnav.twofac.viewmodels.OnboardingViewModel
import tech.arnav.twofac.viewmodels.AccountsViewModel

@Composable
fun HomeScreen(
    onNavigateToAccounts: () -> Unit,
    onNavigateToOnboarding: (unseenOnly: Boolean) -> Unit,
    viewModel: AccountsViewModel = koinViewModel(),
    onboardingViewModel: OnboardingViewModel = koinViewModel(),
) {
    val accounts by viewModel.accounts.collectAsState()
    val accountOtps by viewModel.accountOtps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val otpListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showPasskeyDialog by remember { mutableStateOf(false) }
    var hasTriggeredUnlockFlow by remember { mutableStateOf(false) }
    var hasProcessedOnboardingAutoShow by remember { mutableStateOf(false) }
    val isUnlocked = viewModel.twoFacLibUnlocked
    // Snapshot at composition time — doesn't change after page load.
    val isSecureUnlockReady = remember { viewModel.isSecureUnlockReady() }

    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
        onboardingViewModel.refresh()
    }

    LaunchedEffect(isLoading, hasTriggeredUnlockFlow, isUnlocked) {
        if (!isLoading && !hasTriggeredUnlockFlow && !isUnlocked) {
            hasTriggeredUnlockFlow = true
            if (!isSecureUnlockReady) {
                // No secure unlock readiness — go straight to manual passkey dialog.
                showPasskeyDialog = true
            }
            // When secure unlock is ready the locked state shows a button;
            // the user must tap it to trigger the biometric prompt.
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            showPasskeyDialog = false
        }
    }

    val onboardingState by onboardingViewModel.uiState.collectAsState()
    LaunchedEffect(isUnlocked, isLoading, onboardingState.autoShowDecision, hasProcessedOnboardingAutoShow) {
        if (!isUnlocked || isLoading || hasProcessedOnboardingAutoShow) return@LaunchedEffect
        when (val decision = onboardingState.autoShowDecision) {
            is OnboardingAutoShowDecision.ShowGuide -> {
                val shouldShow = when (decision.mode) {
                    OnboardingAutoShowDecision.ShowGuide.Mode.FULL_INITIAL -> accounts.isEmpty()
                    OnboardingAutoShowDecision.ShowGuide.Mode.UNSEEN_DELTA -> true
                }
                if (!shouldShow) return@LaunchedEffect
                hasProcessedOnboardingAutoShow = true
                onNavigateToOnboarding(decision.mode == OnboardingAutoShowDecision.ShowGuide.Mode.UNSEEN_DELTA)
            }
            OnboardingAutoShowDecision.DoNotAutoShow -> Unit
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            isLoading -> {
                HomeLoadingState()
            }

            !isUnlocked -> {
                HomeLockedState(
                    onSecureUnlock = if (isSecureUnlockReady) {
                        {
                            coroutineScope.launch {
                                val savedPasskey = viewModel.getSavedPasskey()
                                if (savedPasskey != null) {
                                    viewModel.loadAccountsWithOtps(savedPasskey, fromAutoUnlock = true)
                                } else {
                                    // Secure unlock failed or was cancelled — fall back to manual entry.
                                    showPasskeyDialog = true
                                }
                            }
                        }
                    } else null,
                    onManualUnlock = { showPasskeyDialog = true },
                )
            }

            accounts.isEmpty() -> {
                HomeEmptyState(
                    onManageAccounts = onNavigateToAccounts,
                    onOpenGettingStarted = { onNavigateToOnboarding(false) },
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
