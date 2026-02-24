package tech.arnav.twofac.watch

import android.app.Application
import tech.arnav.twofac.watch.datalayer.WatchCapabilityRegistrar

class WatchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WatchCapabilityRegistrar.register(this)
    }
}
