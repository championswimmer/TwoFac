package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.onboarding_title
import twofac.composeapp.generated.resources.action_back
import org.koin.compose.viewmodel.koinViewModel
import tech.arnav.twofac.components.onboarding.OnboardingCompletedState
import tech.arnav.twofac.components.onboarding.OnboardingEmptyState
import tech.arnav.twofac.components.onboarding.OnboardingGuideHeader
import tech.arnav.twofac.components.onboarding.OnboardingLoadingState
import tech.arnav.twofac.components.onboarding.OnboardingStepCard
import tech.arnav.twofac.onboarding.OnboardingCompletionRule
import tech.arnav.twofac.onboarding.OnboardingGuideAction
import tech.arnav.twofac.onboarding.deriveStepCompletion
import tech.arnav.twofac.onboarding.isStepUnseen
import tech.arnav.twofac.viewmodels.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingGuideScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    unseenOnly: Boolean,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val seenKey = remember(unseenOnly) { "onboarding_seen_$unseenOnly" }
    val expandedCompletedSteps = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(seenKey) {
        viewModel.refreshAndSyncDerivedCompletion()
    }

    LaunchedEffect(state.progress.hasSeenInitialOnboardingGuide) {
        if (!state.progress.hasSeenInitialOnboardingGuide) {
            viewModel.markInitialGuideSeen()
        }
    }

    val visibleSteps = if (unseenOnly) {
        state.resolvedSteps.filter { step -> step.id !in state.progress.stepStates.keys }
    } else {
        state.resolvedSteps
    }

    val completedCount = visibleSteps.count { step ->
        deriveStepCompletion(
            step = step,
            context = state.context,
            manuallyCompleted = state.progress.stepStates[step.id]?.completedAtEpochMillis != null,
        )
    }
    val requiredRemainingCount = visibleSteps.count { step ->
        step.required && !deriveStepCompletion(
            step = step,
            context = state.context,
            manuallyCompleted = state.progress.stepStates[step.id]?.completedAtEpochMillis != null,
        )
    }
    val unseenVisibleSteps = visibleSteps.filter { isStepUnseen(it, state.progress.stepStates[it.id]) }

    LaunchedEffect(
        visibleSteps.map { it.id },
        state.context.accountCount,
        state.context.secureUnlockReady,
        state.progress.stepStates.keys,
    ) {
        visibleSteps.forEach { step ->
            val isCompleted = deriveStepCompletion(
                step = step,
                context = state.context,
                manuallyCompleted = state.progress.stepStates[step.id]?.completedAtEpochMillis != null,
            )
            if (!isCompleted) {
                expandedCompletedSteps.remove(step.id)
            } else if (expandedCompletedSteps[step.id] == null) {
                expandedCompletedSteps[step.id] = false
            }
        }
    }

    LaunchedEffect(unseenVisibleSteps.map { it.id }, state.progress.stepStates.keys) {
        if (unseenVisibleSteps.isNotEmpty()) {
            viewModel.markVisibleStepsSeen(unseenVisibleSteps)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.onboarding_title)) },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.action_back))
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            state.isLoading -> {
                OnboardingLoadingState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }

            visibleSteps.isEmpty() && state.resolvedSteps.isEmpty() -> {
                OnboardingEmptyState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }

            visibleSteps.isEmpty() -> {
                OnboardingCompletedState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OnboardingGuideHeader(
                        completedCount = completedCount,
                        totalCount = visibleSteps.size,
                        requiredRemainingCount = requiredRemainingCount,
                    )

                    visibleSteps.forEach { step ->
                        val isCompleted = deriveStepCompletion(
                            step = step,
                            context = state.context,
                            manuallyCompleted = state.progress.stepStates[step.id]?.completedAtEpochMillis != null,
                        )
                        val isExpanded = expandedCompletedSteps[step.id] ?: false
                        OnboardingStepCard(
                            step = step,
                            isCompleted = isCompleted,
                            isExpanded = if (isCompleted) isExpanded else true,
                            onPrimaryClick = {
                                viewModel.markStepSeen(step)
                                if (step.completionRule == OnboardingCompletionRule.MANUAL) {
                                    viewModel.markStepCompleted(step)
                                }
                                when (step.action) {
                                    OnboardingGuideAction.OpenAddAccount -> onNavigateToAddAccount()
                                    OnboardingGuideAction.OpenAccounts -> onNavigateToAccounts()
                                    OnboardingGuideAction.OpenSettings -> onNavigateToSettings()
                                    OnboardingGuideAction.None -> Unit
                                }
                            },
                            onDoneClick = { viewModel.markStepCompleted(step) },
                            onToggleExpanded = {
                                if (isCompleted) {
                                    expandedCompletedSteps[step.id] = !(expandedCompletedSteps[step.id] ?: false)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
