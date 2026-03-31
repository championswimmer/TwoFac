package tech.arnav.twofac.onboarding

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.onboarding_step_secure_unlock_action
import twofac.composeapp.generated.resources.onboarding_step_secure_unlock_title

/**
 * A single parameterised platform contributor for the SECURE_UNLOCK step.
 *
 * Android, iOS, and Wasm each have an identical contributor body except for
 * the platform-specific description string resource.  This class replaces the
 * three per-platform files; each platform DI module passes its own
 * [descriptionRes] when registering the binding.
 */
class SecureUnlockOnboardingContributor(
    private val descriptionRes: StringResource,
) : PlatformOnboardingStepContributor {
    override suspend fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
        if (!context.secureUnlockAvailable) return listOf(omit(OnboardingStepSlot.SECURE_UNLOCK))
        return listOf(
            OnboardingGuideStep(
                id = OnboardingStepIds.SECURE_UNLOCK,
                slot = OnboardingStepSlot.SECURE_UNLOCK,
                title = getString(Res.string.onboarding_step_secure_unlock_title),
                description = getString(descriptionRes),
                required = false,
                icon = OnboardingStepIcon.SECURE_UNLOCK,
                action = OnboardingGuideAction.OpenSettings,
                actionLabel = getString(Res.string.onboarding_step_secure_unlock_action),
                completionRule = OnboardingCompletionRule.SECURE_UNLOCK_READY,
            ).provide()
        )
    }
}
