package tech.arnav.twofac.lib.backup

import tech.arnav.twofac.lib.PublicApi

/**
 * Well-known IDs for built-in backup transports.
 */
@PublicApi
object BackupProviderIds {
    const val LOCAL = "local"
    const val ICLOUD = "icloud"
    const val GOOGLE_DRIVE_APPDATA = "gdrive-appdata"
}
