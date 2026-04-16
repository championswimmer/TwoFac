package tech.arnav.twofac.app

import android.app.Application
import ca.gosyer.appdirs.impl.attachAppDirs
import tech.arnav.twofac.initKoin
import tech.arnav.twofac.di.androidBackupModule
import tech.arnav.twofac.di.androidBiometricModule
import tech.arnav.twofac.di.androidOnboardingModule
import tech.arnav.twofac.di.androidQrModule
import tech.arnav.twofac.di.androidWearSyncModule
import tech.arnav.twofac.wear.PhoneCapabilityRegistrar

class TwoFacApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        attachAppDirs()
        PhoneCapabilityRegistrar.register(this)
        initKoin {
            modules(
                androidWearSyncModule(this@TwoFacApplication),
                androidBiometricModule(this@TwoFacApplication) {
                    MainActivity.currentActivityOrThrow()
                },
                androidQrModule,
                androidOnboardingModule,
                androidBackupModule(this@TwoFacApplication) {
                    MainActivity.currentActivityOrThrow()
                },
            )
        }
    }
}
