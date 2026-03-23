package tech.arnav.twofac.onboarding

import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.lib.TwoFacLib
import tech.arnav.twofac.lib.backup.BackupService
import tech.arnav.twofac.qr.CameraQRCodeReader
import tech.arnav.twofac.qr.ClipboardQRCodeReader
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager

interface OnboardingGuideContextProvider {
    suspend fun currentContext(): OnboardingGuideContext
}

class DefaultOnboardingGuideContextProvider(
    private val twoFacLib: TwoFacLib,
    private val sessionManager: SessionManager?,
    private val cameraQRCodeReader: CameraQRCodeReader?,
    private val clipboardQRCodeReader: ClipboardQRCodeReader?,
    private val backupService: BackupService?,
    private val companionSyncCoordinator: CompanionSyncCoordinator?,
) : OnboardingGuideContextProvider {
    override suspend fun currentContext(): OnboardingGuideContext {
        val secureSessionManager = sessionManager as? SecureSessionManager
        val availableBackupProviderNames = backupService
            ?.listProviders()
            ?.filter { it.isAvailable }
            ?.map { it.displayName }
            .orEmpty()

        return OnboardingGuideContext(
            accountCount = twoFacLib.getAllAccounts().size,
            secureUnlockAvailable = secureSessionManager?.isSecureUnlockAvailable() == true,
            secureUnlockReady = secureSessionManager?.isSecureUnlockReady() == true,
            cameraQrImportAvailable = cameraQRCodeReader != null,
            clipboardQrImportAvailable = clipboardQRCodeReader != null,
            availableBackupProviderNames = availableBackupProviderNames,
            companionSyncAvailable = companionSyncCoordinator != null,
        )
    }
}
