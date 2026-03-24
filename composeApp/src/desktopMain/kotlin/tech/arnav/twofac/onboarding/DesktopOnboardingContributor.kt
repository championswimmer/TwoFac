package tech.arnav.twofac.onboarding

class DesktopOnboardingContributor : PlatformOnboardingStepContributor {
    override fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
        return listOf(omit(OnboardingStepSlot.SECURE_UNLOCK))
    }
}

