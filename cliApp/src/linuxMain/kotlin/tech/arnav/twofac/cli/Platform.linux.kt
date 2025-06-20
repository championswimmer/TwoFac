@file:Suppress("MatchingDeclarationName")

package tech.arnav.twofac.cli

import kotlinx.cinterop.*
import platform.posix.uname
import platform.posix.utsname

class LinuxPlatform : Platform {
    @OptIn(ExperimentalForeignApi::class)
    private fun getLinuxKernelVersion(): String {
        memScoped {
            val utsname = alloc<utsname>()
            if (uname(utsname.ptr) == 0) {
                return "${utsname.sysname.toKString()} ${utsname.release.toKString()}"
            }
            return "Linux"
        }
    }

    override val name: String = getLinuxKernelVersion()
}

actual fun getPlatform(): Platform = LinuxPlatform()
