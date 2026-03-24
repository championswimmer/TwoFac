package tech.arnav.twofac.onboarding

/**
 * Returns true if [step] is considered complete, taking into account both
 * the auto-derived rule and any explicit manual completion stored in progress.
 */
fun deriveStepCompletion(
    step: OnboardingGuideStep,
    context: OnboardingGuideContext,
    manuallyCompleted: Boolean = false,
): Boolean {
    if (manuallyCompleted) return true
    return when (step.completionRule) {
        OnboardingCompletionRule.MANUAL -> false
        OnboardingCompletionRule.ACCOUNT_EXISTS -> context.accountCount > 0
        OnboardingCompletionRule.SECURE_UNLOCK_READY -> context.secureUnlockReady
    }
}

/**
 * Returns true if the user has never seen [step] or if the step's content has
 * been updated since the user last viewed it (i.e. [OnboardingGuideStep.contentRevision]
 * is greater than [OnboardingStepProgressState.contentRevisionSeen]).
 */
fun isStepUnseen(
    step: OnboardingGuideStep,
    stepState: OnboardingStepProgressState?,
): Boolean {
    if (stepState == null) return true
    val revisionSeen = stepState.contentRevisionSeen ?: return true
    return revisionSeen < step.contentRevision
}

