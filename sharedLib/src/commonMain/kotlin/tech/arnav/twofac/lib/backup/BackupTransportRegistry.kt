package tech.arnav.twofac.lib.backup

data class BackupProviderInfo(
    val id: String,
    val displayName: String,
    val description: String? = null,
    val supportsManualBackup: Boolean = true,
    val supportsManualRestore: Boolean = true,
    val supportsAutomaticRestore: Boolean = false,
    val requiresAuthentication: Boolean = false,
)

data class BackupProvider(
    val info: BackupProviderInfo,
    val transport: BackupTransport,
)

interface BackupTransportRegistry {
    fun all(): List<BackupProvider>
    fun get(id: String): BackupProvider?
}

class StaticBackupTransportRegistry(
    providers: List<BackupProvider>,
) : BackupTransportRegistry {
    private val providersById = linkedMapOf<String, BackupProvider>().apply {
        providers.forEach { provider ->
            require(provider.info.id == provider.transport.id) {
                "Backup provider '${provider.info.displayName}' info.id '${provider.info.id}' must match transport.id '${provider.transport.id}'"
            }
            require(!containsKey(provider.info.id)) {
                "Duplicate backup provider id '${provider.info.id}' is not allowed"
            }
            put(provider.info.id, provider)
        }
    }

    override fun all(): List<BackupProvider> = providersById.values.toList()

    override fun get(id: String): BackupProvider? = providersById[id]
}

fun backupTransportRegistryOf(vararg providers: BackupProvider): BackupTransportRegistry {
    return StaticBackupTransportRegistry(providers.toList())
}

val LocalBackupProviderInfo = BackupProviderInfo(
    id = "local",
    displayName = "Local Backup",
    description = "Export or import accounts as a plaintext JSON backup file.",
)

val ICloudBackupProviderInfo = BackupProviderInfo(
    id = "icloud",
    displayName = "iCloud Backup",
    description = "Sync backup snapshots with your private iCloud app container on iPhone and iPad.",
    supportsAutomaticRestore = true,
)

val GoogleDriveAppDataBackupProviderInfo = BackupProviderInfo(
    id = "gdrive-appdata",
    displayName = "Google Drive Backup",
    description = "Sync backup snapshots to your app-private Google Drive appData folder.",
    supportsAutomaticRestore = true,
    requiresAuthentication = true,
)
