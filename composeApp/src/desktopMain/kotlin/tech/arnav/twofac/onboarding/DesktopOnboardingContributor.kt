package tech.arnav.twofac.onboarding

class DesktopOnboardingContributor : PlatformOnboardingStepContributor {
    override suspend fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
        return listOf(omit(OnboardingStepSlot.SECURE_UNLOCK))
    }
}

