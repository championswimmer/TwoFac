package tech.arnav.twofac.onboarding

class AndroidOnboardingContributor : PlatformOnboardingStepContributor {
    override fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
        if (!context.secureUnlockAvailable) return listOf(omit(OnboardingStepSlot.SECURE_UNLOCK))
        return listOf(
            OnboardingGuideStep(
                id = OnboardingStepIds.SECURE_UNLOCK,
                slot = OnboardingStepSlot.SECURE_UNLOCK,
                title = "Enable secure unlock",
                description = "Enable biometric unlock in Settings to unlock quickly with your fingerprint or face.",
                required = false,
                icon = OnboardingStepIcon.SECURE_UNLOCK,
                action = OnboardingGuideAction.OpenSettings,
                actionLabel = "Open security settings",
                completionRule = OnboardingCompletionRule.SECURE_UNLOCK_READY,
            ).provide()
        )
    }
}

