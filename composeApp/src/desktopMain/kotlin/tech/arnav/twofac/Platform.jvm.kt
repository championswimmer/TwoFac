package tech.arnav.twofac

import ca.gosyer.appdirs.AppDirs

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"

    private val appDirs = AppDirs {
        appName = "TwoFac"
        appAuthor = "tech.arnav"
        macOS.useSpaceBetweenAuthorAndApp = false
    }

    override fun getAppDataDir(): String {
        return appDirs.getUserDataDir()
    }
}

actual fun getPlatform(): Platform = JVMPlatform()