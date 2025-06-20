@file:Suppress("MatchingDeclarationName")

package tech.arnav.twofac.cli

import kotlinx.cinterop.*
import platform.windows.GetVersionExW
import platform.windows.OSVERSIONINFOW

class WindowsPlatform : Platform {
    @OptIn(ExperimentalForeignApi::class)
    private fun getWindowsVersion(): String {
        memScoped {
            val versionInfo = alloc<OSVERSIONINFOW>()
            versionInfo.dwOSVersionInfoSize = sizeOf<OSVERSIONINFOW>().toUInt()
            GetVersionExW(versionInfo.ptr)

            return "${versionInfo.dwMajorVersion}.${versionInfo.dwMinorVersion}.${versionInfo.dwBuildNumber}"
        }
    }

    override val name: String = "Windows ${getWindowsVersion()}"
}

actual fun getPlatform(): Platform = WindowsPlatform()