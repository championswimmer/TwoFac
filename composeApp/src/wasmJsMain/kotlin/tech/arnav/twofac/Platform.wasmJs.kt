package tech.arnav.twofac

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"

    override fun getAppDataDir(): String {
        return "/tmp/twofac"
    }
}

actual fun getPlatform(): Platform = WasmPlatform()