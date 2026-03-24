package tech.arnav.twofac.onboarding

import kotlinx.serialization.Serializable

enum class OnboardingStepSlot {
    ADD_FIRST_ACCOUNT,
    MANAGE_ACCOUNTS,
    SECURE_UNLOCK,
    IMPORT_OR_RESTORE,
    BACKUP_AND_RESTORE,
    COMPANION_SYNC,
}

object OnboardingStepIds {
    const val ADD_FIRST_ACCOUNT = "add-first-account"
    const val MANAGE_ACCOUNTS = "manage-accounts"
    const val SECURE_UNLOCK = "secure-unlock"
    const val IMPORT_OR_RESTORE = "import-or-restore"
    const val BACKUP_AND_RESTORE = "backup-and-restore"
    const val COMPANION_SYNC = "companion-sync"
}

enum class OnboardingStepIcon {
    ACCOUNT,
    MANAGE_ACCOUNTS,
    SECURE_UNLOCK,
    IMPORT_OR_RESTORE,
    BACKUP,
    COMPANION_SYNC,
}

sealed interface OnboardingGuideAction {
    data object OpenAddAccount : OnboardingGuideAction
    data object OpenAccounts : OnboardingGuideAction
    data object OpenSettings : OnboardingGuideAction
    data object None : OnboardingGuideAction
}

enum class OnboardingCompletionRule {
    MANUAL,
    ACCOUNT_EXISTS,
    SECURE_UNLOCK_READY,
}

data class OnboardingGuideStep(
    val id: String,
    val slot: OnboardingStepSlot,
    val title: String,
    val description: String,
    val required: Boolean,
    val icon: OnboardingStepIcon,
    val action: OnboardingGuideAction,
    val actionLabel: String? = null,
    val completionRule: OnboardingCompletionRule,
    val contentRevision: Int = 1,
)

data class OnboardingGuideContext(
    val accountCount: Int,
    val secureUnlockAvailable: Boolean,
    val secureUnlockReady: Boolean,
    val cameraQrImportAvailable: Boolean,
    val clipboardQrImportAvailable: Boolean,
    val availableBackupProviderNames: List<String> = emptyList(),
    val companionSyncAvailable: Boolean,
) {
    val hasBackupProviders: Boolean
        get() = availableBackupProviderNames.isNotEmpty()
}

@Serializable
data class OnboardingStepProgressState(
    val seenAtEpochMillis: Long? = null,
    val dismissedAtEpochMillis: Long? = null,
    val completedAtEpochMillis: Long? = null,
    val contentRevisionSeen: Int? = null,
)

@Serializable
data class OnboardingProgressSnapshot(
    val hasSeenInitialOnboardingGuide: Boolean = false,
    val stepStates: Map<String, OnboardingStepProgressState> = emptyMap(),
)
