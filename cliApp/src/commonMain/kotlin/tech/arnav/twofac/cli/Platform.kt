package tech.arnav.twofac.cli

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform