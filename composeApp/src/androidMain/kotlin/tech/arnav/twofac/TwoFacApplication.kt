package tech.arnav.twofac

import android.app.Application
import ca.gosyer.appdirs.impl.attachAppDirs
import tech.arnav.twofac.di.androidWearSyncModule
import tech.arnav.twofac.wear.PhoneCapabilityRegistrar

class TwoFacApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        attachAppDirs()
        PhoneCapabilityRegistrar.register(this)
        initKoin {
            modules(androidWearSyncModule(this@TwoFacApplication))
        }
    }
}
