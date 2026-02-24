package tech.arnav.twofac

import android.app.Application
import ca.gosyer.appdirs.impl.attachAppDirs
import tech.arnav.twofac.di.androidWearSyncModule

class TwoFacApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        attachAppDirs()
        initKoin {
            modules(androidWearSyncModule(this@TwoFacApplication))
        }
    }
}
