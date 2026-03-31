package tech.arnav.twofac.internal

import ca.gosyer.appdirs.AppDirs

internal val jvmAppDirs = AppDirs {
    appName = "TwoFac"
    appAuthor = "tech.arnav"
    macOS.useSpaceBetweenAuthorAndApp = false
}
