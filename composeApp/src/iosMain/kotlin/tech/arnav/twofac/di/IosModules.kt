package tech.arnav.twofac.di

import org.koin.dsl.module
import tech.arnav.twofac.companion.CompanionSyncCoordinator
import tech.arnav.twofac.companion.IosCompanionSyncCoordinator
import tech.arnav.twofac.session.BiometricSessionManager
import tech.arnav.twofac.session.IosBiometricSessionManager
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager

fun iosCompanionSyncModule() = module {
    single<CompanionSyncCoordinator> {
        IosCompanionSyncCoordinator(twoFacLib = get())
    }
}

val iosBiometricModule = module {
    single<BiometricSessionManager> { IosBiometricSessionManager() }
    single<SecureSessionManager> { get<BiometricSessionManager>() }
    single<SessionManager> { get<BiometricSessionManager>() }
}
