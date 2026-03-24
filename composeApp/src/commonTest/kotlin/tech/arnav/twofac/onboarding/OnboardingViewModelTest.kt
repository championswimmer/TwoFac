package tech.arnav.twofac.onboarding

import tech.arnav.twofac.viewmodels.OnboardingViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OnboardingViewModelTest {
    @Test
    fun refreshProducesFullGuideForFirstTimeUser() = runTest {
        val repo = FakeOnboardingProgressRepository(
            snapshot = OnboardingProgressSnapshot(hasSeenInitialOnboardingGuide = false),
        )
        val viewModel = createViewModel(
            context = defaultContext(accountCount = 0),
            repository = repo,
            steps = listOf(addFirstAccountStep()),
        )

        viewModel.refreshNow(syncDerivedCompletion = false)

        val state = viewModel.uiState.value
        val show = assertIs<OnboardingAutoShowDecision.ShowGuide>(state.autoShowDecision)
        assertEquals(OnboardingAutoShowDecision.ShowGuide.Mode.FULL_INITIAL, show.mode)
        assertEquals(1, show.steps.size)
    }

    @Test
    fun refreshAndSyncDerivedCompletionCompletesFirstAccountStep() = runTest {
        val repo = FakeOnboardingProgressRepository(
            snapshot = OnboardingProgressSnapshot(
                hasSeenInitialOnboardingGuide = true,
                stepStates = emptyMap(),
            ),
        )
        val step = addFirstAccountStep()
        val viewModel = createViewModel(
            context = defaultContext(accountCount = 1),
            repository = repo,
            steps = listOf(step),
        )

        viewModel.refreshNow(syncDerivedCompletion = true)

        val completed = repo.snapshot.stepStates[step.id]?.completedAtEpochMillis
        assertTrue(completed != null)
    }

    @Test
    fun deltaModeShowsOnlyUnseenSteps() = runTest {
        val seenStep = addFirstAccountStep()
        val newStep = OnboardingGuideStep(
            id = OnboardingStepIds.SECURE_UNLOCK,
            slot = OnboardingStepSlot.SECURE_UNLOCK,
            title = "Secure",
            description = "desc",
            required = false,
            icon = OnboardingStepIcon.SECURE_UNLOCK,
            action = OnboardingGuideAction.None,
            completionRule = OnboardingCompletionRule.SECURE_UNLOCK_READY,
        )
        val repo = FakeOnboardingProgressRepository(
            snapshot = OnboardingProgressSnapshot(
                hasSeenInitialOnboardingGuide = true,
                stepStates = mapOf(
                    seenStep.id to OnboardingStepProgressState(seenAtEpochMillis = 1L, contentRevisionSeen = 1),
                ),
            ),
        )
        val viewModel = createViewModel(
            context = defaultContext(accountCount = 0),
            repository = repo,
            steps = listOf(seenStep, newStep),
        )

        viewModel.refreshNow(syncDerivedCompletion = false)

        val show = assertIs<OnboardingAutoShowDecision.ShowGuide>(viewModel.uiState.value.autoShowDecision)
        assertEquals(OnboardingAutoShowDecision.ShowGuide.Mode.UNSEEN_DELTA, show.mode)
        assertEquals(listOf(newStep.id), show.steps.map { it.id })
    }

    private fun createViewModel(
        context: OnboardingGuideContext,
        repository: FakeOnboardingProgressRepository,
        steps: List<OnboardingGuideStep>,
    ): OnboardingViewModel {
        val contextProvider = object : OnboardingGuideContextProvider {
            override suspend fun currentContext(): OnboardingGuideContext = context
        }
        val commonContributor = object : CommonOnboardingStepContributor {
            override fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
                return steps.map { it.provide() }
            }
        }
        val registry = OnboardingGuideRegistry(
            commonContributors = listOf(commonContributor),
            platformContributors = emptyList(),
        )
        return OnboardingViewModel(
            contextProvider = contextProvider,
            guideRegistry = registry,
            progressStore = repository,
            autoShowResolver = OnboardingAutoShowResolver(),
        )
    }

    private fun addFirstAccountStep(): OnboardingGuideStep {
        return OnboardingGuideStep(
            id = OnboardingStepIds.ADD_FIRST_ACCOUNT,
            slot = OnboardingStepSlot.ADD_FIRST_ACCOUNT,
            title = "Add account",
            description = "desc",
            required = true,
            icon = OnboardingStepIcon.ACCOUNT,
            action = OnboardingGuideAction.None,
            completionRule = OnboardingCompletionRule.ACCOUNT_EXISTS,
        )
    }

    private fun defaultContext(accountCount: Int): OnboardingGuideContext {
        return OnboardingGuideContext(
            accountCount = accountCount,
            secureUnlockAvailable = true,
            secureUnlockReady = false,
            cameraQrImportAvailable = true,
            clipboardQrImportAvailable = true,
            availableBackupProviderNames = listOf("Local"),
            companionSyncAvailable = false,
        )
    }
}

private class FakeOnboardingProgressRepository(
    var snapshot: OnboardingProgressSnapshot,
) : OnboardingProgressRepository {
    override suspend fun load(): OnboardingProgressSnapshot = snapshot

    override suspend fun markInitialGuideSeen(timestampEpochMillis: Long) {
        snapshot = snapshot.copy(hasSeenInitialOnboardingGuide = true)
    }

    override suspend fun markStepSeen(stepId: String, contentRevision: Int, timestampEpochMillis: Long) {
        val current = snapshot.stepStates[stepId] ?: OnboardingStepProgressState()
        snapshot = snapshot.copy(
            stepStates = snapshot.stepStates + (
                stepId to current.copy(
                    seenAtEpochMillis = current.seenAtEpochMillis ?: timestampEpochMillis,
                    contentRevisionSeen = contentRevision,
                )
                ),
        )
    }

    override suspend fun markStepDismissed(stepId: String, contentRevision: Int, timestampEpochMillis: Long) {
        val current = snapshot.stepStates[stepId] ?: OnboardingStepProgressState()
        snapshot = snapshot.copy(
            stepStates = snapshot.stepStates + (
                stepId to current.copy(
                    seenAtEpochMillis = current.seenAtEpochMillis ?: timestampEpochMillis,
                    dismissedAtEpochMillis = timestampEpochMillis,
                    contentRevisionSeen = contentRevision,
                )
                ),
        )
    }

    override suspend fun markStepCompleted(stepId: String, contentRevision: Int, timestampEpochMillis: Long) {
        val current = snapshot.stepStates[stepId] ?: OnboardingStepProgressState()
        snapshot = snapshot.copy(
            stepStates = snapshot.stepStates + (
                stepId to current.copy(
                    seenAtEpochMillis = current.seenAtEpochMillis ?: timestampEpochMillis,
                    completedAtEpochMillis = current.completedAtEpochMillis ?: timestampEpochMillis,
                    contentRevisionSeen = contentRevision,
                )
                ),
        )
    }
}
