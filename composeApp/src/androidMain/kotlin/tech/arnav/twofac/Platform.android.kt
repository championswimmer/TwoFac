package tech.arnav.twofac

import android.os.Build
import ca.gosyer.appdirs.AppDirs

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    private val appDirs = AppDirs {
        appName = "TwoFac"
        appAuthor = "tech.arnav"
    }

    override fun getAppDataDir(): String {
        return appDirs.getUserDataDir()
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()