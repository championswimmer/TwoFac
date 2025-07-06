package tech.arnav.twofac

interface Platform {
    val name: String
    fun getAppDataDir(): String
}

expect fun getPlatform(): Platform