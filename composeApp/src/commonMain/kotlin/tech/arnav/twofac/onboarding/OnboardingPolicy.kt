package tech.arnav.twofac.onboarding

object OnboardingPolicy {
    const val IS_NON_BLOCKING_GUIDE: Boolean = true

    val initialStepOrdering: List<OnboardingStepSlot> = OnboardingStepSlot.entries

    val requiredStepSlots: Set<OnboardingStepSlot> = setOf(
        OnboardingStepSlot.ADD_FIRST_ACCOUNT,
    )

    val optionalStepSlots: Set<OnboardingStepSlot> = OnboardingStepSlot.entries
        .filterNot { it in requiredStepSlots }
        .toSet()
}

