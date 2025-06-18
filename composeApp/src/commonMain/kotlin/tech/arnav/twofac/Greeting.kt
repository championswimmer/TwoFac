package tech.arnav.twofac
import tech.arnav.twofac.sharedlib.platform as sharedLibPlatform
class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name} ${sharedLibPlatform()}!"
    }
}