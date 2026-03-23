package tech.arnav.twofac.onboarding

class BaseCommonOnboardingStepContributor : CommonOnboardingStepContributor {
    override fun contribute(context: OnboardingGuideContext): List<OnboardingStepContribution> {
        val addAccountDescription = buildAddAccountDescription(context)
        val steps = mutableListOf<OnboardingStepContribution>()

        steps += OnboardingGuideStep(
            id = OnboardingStepIds.ADD_FIRST_ACCOUNT,
            slot = OnboardingStepSlot.ADD_FIRST_ACCOUNT,
            title = "Add your first account",
            description = addAccountDescription,
            required = true,
            icon = OnboardingStepIcon.ACCOUNT,
            action = OnboardingGuideAction.OpenAddAccount,
            actionLabel = "Add account",
            completionRule = OnboardingCompletionRule.ACCOUNT_EXISTS,
        ).provide()

        steps += OnboardingGuideStep(
            id = OnboardingStepIds.MANAGE_ACCOUNTS,
            slot = OnboardingStepSlot.MANAGE_ACCOUNTS,
            title = "Manage your accounts",
            description = "Browse your accounts, view OTP codes, and remove entries you no longer need.",
            required = false,
            icon = OnboardingStepIcon.MANAGE_ACCOUNTS,
            action = OnboardingGuideAction.OpenAccounts,
            actionLabel = "Open accounts",
            completionRule = OnboardingCompletionRule.MANUAL,
        ).provide()

        steps += OnboardingGuideStep(
            id = OnboardingStepIds.IMPORT_OR_RESTORE,
            slot = OnboardingStepSlot.IMPORT_OR_RESTORE,
            title = "Import or restore existing data",
            description = "Use Settings to import from your backup providers and restore existing accounts.",
            required = false,
            icon = OnboardingStepIcon.IMPORT_OR_RESTORE,
            action = OnboardingGuideAction.OpenSettings,
            actionLabel = "Open settings",
            completionRule = OnboardingCompletionRule.MANUAL,
        ).provide()

        if (context.hasBackupProviders) {
            val providerText = context.availableBackupProviderNames.joinToString()
            steps += OnboardingGuideStep(
                id = OnboardingStepIds.BACKUP_AND_RESTORE,
                slot = OnboardingStepSlot.BACKUP_AND_RESTORE,
                title = "Back up your vault",
                description = "Create encrypted backups from Settings. Available providers: $providerText.",
                required = false,
                icon = OnboardingStepIcon.BACKUP,
                action = OnboardingGuideAction.OpenSettings,
                actionLabel = "Open backup settings",
                completionRule = OnboardingCompletionRule.MANUAL,
            ).provide()
        } else {
            steps += omit(OnboardingStepSlot.BACKUP_AND_RESTORE)
        }

        if (context.companionSyncAvailable) {
            steps += OnboardingGuideStep(
                id = OnboardingStepIds.COMPANION_SYNC,
                slot = OnboardingStepSlot.COMPANION_SYNC,
                title = "Set up companion sync",
                description = "Send your accounts to your companion device from Settings when it is connected.",
                required = false,
                icon = OnboardingStepIcon.COMPANION_SYNC,
                action = OnboardingGuideAction.OpenSettings,
                actionLabel = "Open companion settings",
                completionRule = OnboardingCompletionRule.MANUAL,
            ).provide()
        } else {
            steps += omit(OnboardingStepSlot.COMPANION_SYNC)
        }

        return steps
    }
}

private fun buildAddAccountDescription(context: OnboardingGuideContext): String {
    val methods = buildList {
        if (context.cameraQrImportAvailable) add("scan a QR code")
        if (context.clipboardQrImportAvailable) add("paste a QR image from clipboard")
        add("paste or enter an otpauth:// URI manually")
    }
    return "Add your first account by ${methods.joinToString()}."
}

