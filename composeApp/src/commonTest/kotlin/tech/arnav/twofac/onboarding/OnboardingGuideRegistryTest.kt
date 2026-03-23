package tech.arnav.twofac.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingGuideRegistryTest {
    @Test
    fun registryOrdersBySlotAndAllowsPlatformOverride() {
        val common = object : CommonOnboardingStepContributor {
            override fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
                return listOf(
                    step(OnboardingStepSlot.ADD_FIRST_ACCOUNT, "a").provide(),
                    step(OnboardingStepSlot.MANAGE_ACCOUNTS, "b").provide(),
                    step(OnboardingStepSlot.SECURE_UNLOCK, "common-secure").provide(),
                )
            }
        }
        val platform = object : PlatformOnboardingStepContributor {
            override fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
                return listOf(
                    step(OnboardingStepSlot.SECURE_UNLOCK, "platform-secure").provide(),
                )
            }
        }

        val registry = OnboardingGuideRegistry(
            commonContributors = listOf(common),
            platformContributors = listOf(platform),
        )
        val resolved = registry.resolveSteps(defaultContext())

        assertEquals(listOf("a", "b", "platform-secure"), resolved.map { it.id })
    }

    @Test
    fun registryOmitsUnsupportedSlotWhenPlatformOmits() {
        val common = object : CommonOnboardingStepContributor {
            override fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
                return listOf(step(OnboardingStepSlot.SECURE_UNLOCK, "common-secure").provide())
            }
        }
        val platform = object : PlatformOnboardingStepContributor {
            override fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
                return listOf(omit(OnboardingStepSlot.SECURE_UNLOCK))
            }
        }

        val registry = OnboardingGuideRegistry(
            commonContributors = listOf(common),
            platformContributors = listOf(platform),
        )
        val resolved = registry.resolveSteps(defaultContext())
        assertEquals(emptyList(), resolved)
    }

    private fun defaultContext() = OnboardingGuideContext(
        accountCount = 0,
        secureUnlockAvailable = false,
        secureUnlockReady = false,
        cameraQrImportAvailable = false,
        clipboardQrImportAvailable = false,
        availableBackupProviderNames = emptyList(),
        companionSyncAvailable = false,
    )

    private fun step(slot: OnboardingStepSlot, id: String): OnboardingGuideStep {
        return OnboardingGuideStep(
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
}
