package tech.arnav.twofac.cli

import ca.gosyer.appdirs.AppDirs

interface Platform {
    val name: String

    val appDirs get() = _appDirs

    companion object {
        private val _appDirs = AppDirs {
            appName = "2fac"
            appAuthor = "arnav.tech"
            macOS.useSpaceBetweenAuthorAndApp = false
        }
    }
}

expect fun getPlatform(): Platform