@file:Suppress("MatchingDeclarationName")

package tech.arnav.twofac.cli

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSProcessInfo

class MacOSPlatform : Platform {
    @OptIn(ExperimentalForeignApi::class)
    private fun getMacOSVersion(): String {
        val osVersion = NSProcessInfo.processInfo.operatingSystemVersion
        osVersion.useContents {
            return "${majorVersion}.${minorVersion}.${patchVersion}"
        }
    }

    override val name: String = "macOS ${getMacOSVersion()}"
}

actual fun getPlatform(): Platform = MacOSPlatform()