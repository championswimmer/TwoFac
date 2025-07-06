package tech.arnav.twofac

import android.app.Application
import ca.gosyer.appdirs.impl.attachAppDirs

class TwoFacApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        attachAppDirs()
        initKoin()
    }
}