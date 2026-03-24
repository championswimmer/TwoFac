package tech.arnav.twofac.onboarding

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf

actual fun createOnboardingProgressStore(): KStore<OnboardingProgressSnapshot> {
    return storeOf(
        key = ONBOARDING_PROGRESS_STORAGE_KEY,
        default = OnboardingProgressSnapshot(),
    )
}
