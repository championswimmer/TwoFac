package tech.arnav.twofac.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.arnav.twofac.backup.LocalFileBackupTransport
import tech.arnav.twofac.lib.backup.BackupTransport
import tech.arnav.twofac.onboarding.PlatformOnboardingStepContributor
import tech.arnav.twofac.onboarding.SecureUnlockOnboardingContributor
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.onboarding_step_secure_unlock_wasm_description
import tech.arnav.twofac.qr.ClipboardQRCodeReader
import tech.arnav.twofac.qr.WasmClipboardQRCodeReader
import tech.arnav.twofac.session.BrowserSessionManager
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager

val wasmSessionModule = module {
    single<SecureSessionManager> { BrowserSessionManager() }
    single<SessionManager> { get<SecureSessionManager>() }
}

val wasmQrModule = module {
    single<ClipboardQRCodeReader> { WasmClipboardQRCodeReader() }
}

val wasmBackupModule = module {
    single<BackupTransport>(named("local")) {
        LocalFileBackupTransport()
    }
}

val wasmOnboardingModule = module {
    single<PlatformOnboardingStepContributor> { SecureUnlockOnboardingContributor(Res.string.onboarding_step_secure_unlock_wasm_description) }
}
