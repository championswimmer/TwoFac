package tech.arnav.twofac.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OnboardingAutoShowResolverTest {
    private val resolver = OnboardingAutoShowResolver()

    @Test
    fun firstTimeUserSeesFullGuide() {
        val steps = listOf(step(OnboardingStepIds.ADD_FIRST_ACCOUNT), step(OnboardingStepIds.MANAGE_ACCOUNTS))
        val decision = resolver.resolve(
            progress = OnboardingProgressSnapshot(hasSeenInitialOnboardingGuide = false),
            resolvedSteps = steps,
        )
        val show = assertIs<OnboardingAutoShowDecision.ShowGuide>(decision)
        assertEquals(OnboardingAutoShowDecision.ShowGuide.Mode.FULL_INITIAL, show.mode)
        assertEquals(steps, show.steps)
    }

    @Test
    fun returningUserWithNoUnseenStepsDoesNotAutoShow() {
        val steps = listOf(step(OnboardingStepIds.ADD_FIRST_ACCOUNT), step(OnboardingStepIds.MANAGE_ACCOUNTS))
        val decision = resolver.resolve(
            progress = OnboardingProgressSnapshot(
                hasSeenInitialOnboardingGuide = true,
                stepStates = mapOf(
                    OnboardingStepIds.ADD_FIRST_ACCOUNT to OnboardingStepProgressState(seenAtEpochMillis = 1L),
                    OnboardingStepIds.MANAGE_ACCOUNTS to OnboardingStepProgressState(seenAtEpochMillis = 1L),
                ),
            ),
            resolvedSteps = steps,
        )
        assertIs<OnboardingAutoShowDecision.DoNotAutoShow>(decision)
    }

    @Test
    fun returningUserWithNewStepSeesOnlyDelta() {
        val stepA = step(OnboardingStepIds.ADD_FIRST_ACCOUNT)
        val stepB = step(OnboardingStepIds.MANAGE_ACCOUNTS)
        val stepD = step(OnboardingStepIds.SECURE_UNLOCK, slot = OnboardingStepSlot.SECURE_UNLOCK)
        val decision = resolver.resolve(
            progress = OnboardingProgressSnapshot(
                hasSeenInitialOnboardingGuide = true,
                stepStates = mapOf(
                    stepA.id to OnboardingStepProgressState(seenAtEpochMillis = 1L),
                    stepB.id to OnboardingStepProgressState(seenAtEpochMillis = 1L),
                ),
            ),
            resolvedSteps = listOf(stepA, stepB, stepD),
        )
        val show = assertIs<OnboardingAutoShowDecision.ShowGuide>(decision)
        assertEquals(OnboardingAutoShowDecision.ShowGuide.Mode.UNSEEN_DELTA, show.mode)
        assertEquals(listOf(stepD), show.steps)
    }

    private fun step(
        id: String,
        slot: OnboardingStepSlot = OnboardingStepSlot.ADD_FIRST_ACCOUNT,
    ) = OnboardingGuideStep(
        id = id,
        slot = slot,
        title = id,
        description = "desc",
        required = false,
        icon = OnboardingStepIcon.ACCOUNT,
        action = OnboardingGuideAction.None,
        completionRule = OnboardingCompletionRule.MANUAL,
    )
}
