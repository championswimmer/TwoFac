package tech.arnav.twofac.onboarding

import org.jetbrains.compose.resources.getString
import twofac.composeapp.generated.resources.*

class IosOnboardingContributor : PlatformOnboardingStepContributor {
    override suspend fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
        if (!context.secureUnlockAvailable) return listOf(omit(OnboardingStepSlot.SECURE_UNLOCK))
        return listOf(
            OnboardingGuideStep(
                id = OnboardingStepIds.SECURE_UNLOCK,
                slot = OnboardingStepSlot.SECURE_UNLOCK,
                title = getString(Res.string.onboarding_step_secure_unlock_title),
                description = getString(Res.string.onboarding_step_secure_unlock_ios_description),
                required = false,
                icon = OnboardingStepIcon.SECURE_UNLOCK,
                action = OnboardingGuideAction.OpenSettings,
                actionLabel = getString(Res.string.onboarding_step_secure_unlock_action),
                completionRule = OnboardingCompletionRule.SECURE_UNLOCK_READY,
            ).provide()
        )
    }
}

