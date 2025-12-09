@file:Suppress("MatchingDeclarationName")

package tech.arnav.twofac.cli

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
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
