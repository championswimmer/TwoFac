package tech.arnav.twofac.cli

import ca.gosyer.appdirs.AppDirs

interface Platform {
    val name: String

    val appDirs get() = _appDirs

    companion object {
        private val _appDirs = AppDirs {
            appName = "TwoFac"
            appAuthor = "tech.arnav"
            macOS.useSpaceBetweenAuthorAndApp = false
        }
    }
}

expect fun getPlatform(): Platform