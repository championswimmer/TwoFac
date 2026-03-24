package tech.arnav.twofac.onboarding

sealed interface OnboardingStepContribution {
    val slot: OnboardingStepSlot

    data class Provide(
        val step: OnboardingGuideStep,
    ) : OnboardingStepContribution {
        override val slot: OnboardingStepSlot = step.slot
    }

    data class Omit(
        override val slot: OnboardingStepSlot,
    ) : OnboardingStepContribution
}

fun OnboardingGuideStep.provide(): OnboardingStepContribution = OnboardingStepContribution.Provide(this)

fun omit(slot: OnboardingStepSlot): OnboardingStepContribution = OnboardingStepContribution.Omit(slot)

interface OnboardingStepContributor {
    fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution>
}

interface CommonOnboardingStepContributor : OnboardingStepContributor

interface PlatformOnboardingStepContributor : OnboardingStepContributor

class OnboardingGuideRegistry(
    private val commonContributors: List<OnboardingStepContributor>,
    private val platformContributors: List<OnboardingStepContributor>,
) {
    private val slotOrder = OnboardingPolicy.initialStepOrdering

    fun resolveSteps(context: OnboardingGuideContext): List<OnboardingGuideStep> {
        val merged = LinkedHashMap<OnboardingStepSlot, OnboardingGuideStep>()

        commonContributors
            .flatMap { it.contribute(context) }
            .forEach { contribution ->
                when (contribution) {
                    is OnboardingStepContribution.Provide -> merged[contribution.slot] = contribution.step
                    is OnboardingStepContribution.Omit -> merged.remove(contribution.slot)
                }
            }

        platformContributors
            .flatMap { it.contribute(context) }
            .forEach { contribution ->
                when (contribution) {
                    is OnboardingStepContribution.Provide -> merged[contribution.slot] = contribution.step
                    is OnboardingStepContribution.Omit -> merged.remove(contribution.slot)
                }
            }

        return slotOrder.mapNotNull { slot -> merged[slot] }
    }
}

sealed interface OnboardingAutoShowDecision {
    data object DoNotAutoShow : OnboardingAutoShowDecision
    data class ShowGuide(
        val steps: List<OnboardingGuideStep>,
        val mode: Mode,
    ) : OnboardingAutoShowDecision {
        enum class Mode {
            FULL_INITIAL,
            UNSEEN_DELTA,
        }
    }
}

class OnboardingAutoShowResolver {
    fun resolve(
        progress: OnboardingProgressSnapshot,
        resolvedSteps: List<OnboardingGuideStep>,
    ): OnboardingAutoShowDecision {
        if (resolvedSteps.isEmpty()) return OnboardingAutoShowDecision.DoNotAutoShow

        if (!progress.hasSeenInitialOnboardingGuide) {
            return OnboardingAutoShowDecision.ShowGuide(
                steps = resolvedSteps,
                mode = OnboardingAutoShowDecision.ShowGuide.Mode.FULL_INITIAL,
            )
        }

        val unseenSteps = resolvedSteps.filter { step ->
            isStepUnseen(step, progress.stepStates[step.id])
        }

        if (unseenSteps.isEmpty()) return OnboardingAutoShowDecision.DoNotAutoShow

        return OnboardingAutoShowDecision.ShowGuide(
            steps = unseenSteps,
            mode = OnboardingAutoShowDecision.ShowGuide.Mode.UNSEEN_DELTA,
        )
    }
}
