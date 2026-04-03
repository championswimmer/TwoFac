package tech.arnav.twofac.cli.storage

import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

enum class CliStorageBackend(val cliValue: String) {
    STANDALONE("standalone"),
    COMMON("common");

    companion object {
        fun fromCliValue(value: String?): CliStorageBackend? {
            return entries.firstOrNull { it.cliValue == value }
        }
    }
}

data class CliConfig(
    val storageBackend: CliStorageBackend = CliStorageBackend.STANDALONE,
)

object CliConfigStore {
    private val backendRegex = Regex("\"storageBackend\"\\s*:\\s*\"([^\"]+)\"")

    fun read(): CliConfig {
        val path = AppDirUtils.getCliConfigFilePath()
        if (!SystemFileSystem.exists(path)) return CliConfig()

        return runCatching {
            val raw = readFile(path)
            val backend = backendRegex.find(raw)?.groupValues?.getOrNull(1)
            CliConfig(storageBackend = CliStorageBackend.fromCliValue(backend) ?: CliStorageBackend.STANDALONE)
        }.getOrDefault(CliConfig())
    }

    fun write(config: CliConfig): Boolean {
        val path = AppDirUtils.getCliConfigFilePath(forceCreate = true)
        val json = """
            {
              "storageBackend": "${config.storageBackend.cliValue}"
            }
        """.trimIndent()

        return runCatching {
            SystemFileSystem.sink(path).buffered().use { sink ->
                sink.write(json.encodeToByteArray())
                sink.flush()
            }
            true
        }.getOrDefault(false)
    }

    private fun readFile(path: kotlinx.io.files.Path): String {
        val buffer = Buffer()
        SystemFileSystem.source(path).buffered().use { source ->
            source.transferTo(buffer)
        }
        return buffer.readByteArray().decodeToString()
    }
}
