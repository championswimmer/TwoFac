package tech.arnav.twofac.onboarding

import org.jetbrains.compose.resources.getString
import twofac.composeapp.generated.resources.*

class BaseCommonOnboardingStepContributor : CommonOnboardingStepContributor {
    override suspend fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
        val addAccountDescription = buildAddAccountDescription(context)
        val steps = mutableListOf<OnboardingStepContribution>()

        steps += OnboardingGuideStep(
            id = OnboardingStepIds.ADD_FIRST_ACCOUNT,
            slot = OnboardingStepSlot.ADD_FIRST_ACCOUNT,
            title = getString(Res.string.onboarding_step_add_account_title),
            description = addAccountDescription,
            required = true,
            icon = OnboardingStepIcon.ACCOUNT,
            action = OnboardingGuideAction.OpenAddAccount,
            actionLabel = getString(Res.string.onboarding_step_add_account_action),
            completionRule = OnboardingCompletionRule.ACCOUNT_EXISTS,
        ).provide()

        steps += OnboardingGuideStep(
            id = OnboardingStepIds.MANAGE_ACCOUNTS,
            slot = OnboardingStepSlot.MANAGE_ACCOUNTS,
            title = getString(Res.string.onboarding_step_manage_accounts_title),
            description = getString(Res.string.onboarding_step_manage_accounts_description),
            required = false,
            icon = OnboardingStepIcon.MANAGE_ACCOUNTS,
            action = OnboardingGuideAction.OpenAccounts,
            actionLabel = getString(Res.string.onboarding_step_manage_accounts_action),
            completionRule = OnboardingCompletionRule.MANUAL,
        ).provide()

        steps += OnboardingGuideStep(
            id = OnboardingStepIds.IMPORT_OR_RESTORE,
            slot = OnboardingStepSlot.IMPORT_OR_RESTORE,
            title = getString(Res.string.onboarding_step_import_title),
            description = getString(Res.string.onboarding_step_import_description),
            required = false,
            icon = OnboardingStepIcon.IMPORT_OR_RESTORE,
            action = OnboardingGuideAction.OpenSettings,
            actionLabel = getString(Res.string.onboarding_step_import_action),
            completionRule = OnboardingCompletionRule.MANUAL,
        ).provide()

        if (context.hasBackupProviders) {
            val providerText = context.availableBackupProviderNames.joinToString()
            steps += OnboardingGuideStep(
                id = OnboardingStepIds.BACKUP_AND_RESTORE,
                slot = OnboardingStepSlot.BACKUP_AND_RESTORE,
                title = getString(Res.string.onboarding_step_backup_title),
                description = getString(Res.string.onboarding_step_backup_description, providerText),
                required = false,
                icon = OnboardingStepIcon.BACKUP,
                action = OnboardingGuideAction.OpenSettings,
                actionLabel = getString(Res.string.onboarding_step_backup_action),
                completionRule = OnboardingCompletionRule.MANUAL,
            ).provide()
        } else {
            steps += omit(OnboardingStepSlot.BACKUP_AND_RESTORE)
        }

        if (context.companionSyncAvailable) {
            steps += OnboardingGuideStep(
                id = OnboardingStepIds.COMPANION_SYNC,
                slot = OnboardingStepSlot.COMPANION_SYNC,
                title = getString(Res.string.onboarding_step_companion_title),
                description = getString(Res.string.onboarding_step_companion_description),
                required = false,
                icon = OnboardingStepIcon.COMPANION_SYNC,
                action = OnboardingGuideAction.OpenSettings,
                actionLabel = getString(Res.string.onboarding_step_companion_action),
                completionRule = OnboardingCompletionRule.MANUAL,
            ).provide()
        } else {
            steps += omit(OnboardingStepSlot.COMPANION_SYNC)
        }

        return steps
    }
}

private suspend fun buildAddAccountDescription(context: OnboardingGuideContext): String {
    val methods = buildList {
        if (context.cameraQrImportAvailable) add(getString(Res.string.onboarding_method_scan_qr))
        if (context.clipboardQrImportAvailable) add(getString(Res.string.onboarding_method_paste_qr))
        add(getString(Res.string.onboarding_method_enter_uri))
    }
    return getString(Res.string.onboarding_step_add_account_description, methods.joinToString())
}

