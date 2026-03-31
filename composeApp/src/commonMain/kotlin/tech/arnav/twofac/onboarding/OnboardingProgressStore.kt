package tech.arnav.twofac.onboarding

import io.github.xxfast.kstore.KStore
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

const val ONBOARDING_PROGRESS_STORAGE_KEY = "twofac_onboarding_progress"
const val ONBOARDING_PROGRESS_STORAGE_FILE = "onboarding_progress.json"

expect fun createOnboardingProgressStore(): KStore<OnboardingProgressSnapshot>

internal fun ensureOnboardingFileExists(filePath: Path) {
    if (SystemFileSystem.exists(filePath)) return
    SystemFileSystem.sink(filePath).buffered().use { sink ->
        sink.write("""{"hasSeenInitialOnboardingGuide":false,"stepStates":{}}""".encodeToByteArray())
        sink.flush()
    }
}
interface OnboardingProgressRepository {
    suspend fun load(): OnboardingProgressSnapshot
    suspend fun markInitialGuideSeen(timestampEpochMillis: Long)
    suspend fun markStepSeen(stepId: String, contentRevision: Int, timestampEpochMillis: Long)
    suspend fun markStepDismissed(stepId: String, contentRevision: Int, timestampEpochMillis: Long)
    suspend fun markStepCompleted(stepId: String, contentRevision: Int, timestampEpochMillis: Long)
}

class OnboardingProgressStore(
    private val store: KStore<OnboardingProgressSnapshot>,
) : OnboardingProgressRepository {
    override suspend fun load(): OnboardingProgressSnapshot {
        return store.get() ?: OnboardingProgressSnapshot()
    }

    override suspend fun markInitialGuideSeen(@Suppress("UNUSED_PARAMETER") timestampEpochMillis: Long) {
        val current = load()
        if (current.hasSeenInitialOnboardingGuide) return
        val updated = current.copy(
            hasSeenInitialOnboardingGuide = true,
        )
        store.set(updated)
    }

    override suspend fun markStepSeen(stepId: String, contentRevision: Int, timestampEpochMillis: Long) {
        updateStep(stepId) { existing ->
            existing.copy(
                seenAtEpochMillis = existing.seenAtEpochMillis ?: timestampEpochMillis,
                contentRevisionSeen = contentRevision,
            )
        }
    }

    override suspend fun markStepDismissed(stepId: String, contentRevision: Int, timestampEpochMillis: Long) {
        updateStep(stepId) { existing ->
            existing.copy(
                seenAtEpochMillis = existing.seenAtEpochMillis ?: timestampEpochMillis,
                dismissedAtEpochMillis = timestampEpochMillis,
                contentRevisionSeen = contentRevision,
            )
        }
    }

    override suspend fun markStepCompleted(stepId: String, contentRevision: Int, timestampEpochMillis: Long) {
        updateStep(stepId) { existing ->
            existing.copy(
                seenAtEpochMillis = existing.seenAtEpochMillis ?: timestampEpochMillis,
                completedAtEpochMillis = existing.completedAtEpochMillis ?: timestampEpochMillis,
                contentRevisionSeen = contentRevision,
            )
        }
    }

    private suspend fun updateStep(
        stepId: String,
        transform: (OnboardingStepProgressState) -> OnboardingStepProgressState,
    ) {
        val current = load()
        val existing = current.stepStates[stepId] ?: OnboardingStepProgressState()
        val updatedStep = transform(existing)
        val updated = current.copy(
            stepStates = current.stepStates + (stepId to updatedStep),
        )
        store.set(updated)
    }
}
