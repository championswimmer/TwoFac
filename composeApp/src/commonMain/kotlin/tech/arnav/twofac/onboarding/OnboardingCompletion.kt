package tech.arnav.twofac.onboarding

fun deriveStepCompletion(
    step: OnboardingGuideStep,
    context: OnboardingGuideContext,
): Boolean {
    return when (step.completionRule) {
        OnboardingCompletionRule.MANUAL -> false
        OnboardingCompletionRule.ACCOUNT_EXISTS -> context.accountCount > 0
        OnboardingCompletionRule.SECURE_UNLOCK_READY -> context.secureUnlockReady
    }
}

