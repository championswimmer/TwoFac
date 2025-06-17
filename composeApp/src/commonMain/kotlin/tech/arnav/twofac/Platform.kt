package tech.arnav.twofac

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform