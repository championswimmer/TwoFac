package tech.arnav.twofac.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import tech.arnav.twofac.onboarding.OnboardingAutoShowDecision
import tech.arnav.twofac.onboarding.OnboardingAutoShowResolver
import tech.arnav.twofac.onboarding.OnboardingCompletionRule
import tech.arnav.twofac.onboarding.OnboardingGuideContext
import tech.arnav.twofac.onboarding.OnboardingGuideContextProvider
import tech.arnav.twofac.onboarding.OnboardingGuideRegistry
import tech.arnav.twofac.onboarding.OnboardingGuideStep
import tech.arnav.twofac.onboarding.OnboardingProgressSnapshot
import tech.arnav.twofac.onboarding.OnboardingProgressRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class OnboardingUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val progress: OnboardingProgressSnapshot = OnboardingProgressSnapshot(),
    val context: OnboardingGuideContext = OnboardingGuideContext(
        accountCount = 0,
        secureUnlockAvailable = false,
        secureUnlockReady = false,
        cameraQrImportAvailable = false,
        clipboardQrImportAvailable = false,
        availableBackupProviderNames = emptyList(),
        companionSyncAvailable = false,
    ),
    val resolvedSteps: List<OnboardingGuideStep> = emptyList(),
    val autoShowDecision: OnboardingAutoShowDecision = OnboardingAutoShowDecision.DoNotAutoShow,
)

class OnboardingViewModel(
    private val contextProvider: OnboardingGuideContextProvider,
    private val guideRegistry: OnboardingGuideRegistry,
    private val progressStore: OnboardingProgressRepository,
    private val autoShowResolver: OnboardingAutoShowResolver,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            refreshNow(syncDerivedCompletion = false)
        }
    }

    fun refreshAndSyncDerivedCompletion() {
        viewModelScope.launch {
            refreshNow(syncDerivedCompletion = true)
        }
    }

    suspend fun refreshNow(syncDerivedCompletion: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        try {
            val context = contextProvider.currentContext()
            val resolvedSteps = guideRegistry.resolveSteps(context)
            if (syncDerivedCompletion) {
                syncDerivedCompletion(context = context, steps = resolvedSteps)
            }
            val progress = progressStore.load()
            val autoShowDecision = autoShowResolver.resolve(
                progress = progress,
                resolvedSteps = resolvedSteps,
            )
            _uiState.value = OnboardingUiState(
                isLoading = false,
                error = null,
                progress = progress,
                context = context,
                resolvedSteps = resolvedSteps,
                autoShowDecision = autoShowDecision,
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to refresh onboarding",
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    fun markInitialGuideSeen() {
        val now = Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            progressStore.markInitialGuideSeen(now)
            refresh()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun markVisibleStepsSeen(steps: List<OnboardingGuideStep>) {
        val now = Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            val progress = progressStore.load()
            var updated = false
            steps.forEach { step ->
                if (progress.stepStates[step.id] == null) {
                    progressStore.markStepSeen(step.id, step.contentRevision, now)
                    updated = true
                }
            }
            if (updated) {
                refresh()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun markStepSeen(step: OnboardingGuideStep) {
        val now = Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            progressStore.markStepSeen(step.id, step.contentRevision, now)
            refresh()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun markStepDismissed(step: OnboardingGuideStep) {
        val now = Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            progressStore.markStepDismissed(step.id, step.contentRevision, now)
            refresh()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun markStepCompleted(step: OnboardingGuideStep) {
        val now = Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            progressStore.markStepCompleted(step.id, step.contentRevision, now)
            refresh()
        }
    }

    @OptIn(ExperimentalTime::class)
    fun syncDerivedCompletion() {
        viewModelScope.launch {
            val context = contextProvider.currentContext()
            val steps = guideRegistry.resolveSteps(context)
            syncDerivedCompletion(context, steps)
            refresh()
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun syncDerivedCompletion(
        context: OnboardingGuideContext,
        steps: List<OnboardingGuideStep>,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        steps.forEach { step ->
            val complete = when (step.completionRule) {
                OnboardingCompletionRule.MANUAL -> false
                OnboardingCompletionRule.ACCOUNT_EXISTS -> context.accountCount > 0
                OnboardingCompletionRule.SECURE_UNLOCK_READY -> context.secureUnlockReady
            }
            if (complete) {
                progressStore.markStepCompleted(step.id, step.contentRevision, now)
            }
        }
    }
}
