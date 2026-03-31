package tech.arnav.twofac.internal

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal fun getDocumentDirectory(): String {
    val documentDirectories = NSSearchPathForDirectoriesInDomains(
        directory = NSDocumentDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true,
    )
    return documentDirectories.firstOrNull() as? String ?: ""
}
