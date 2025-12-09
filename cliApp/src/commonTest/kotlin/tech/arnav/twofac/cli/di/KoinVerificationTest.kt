package tech.arnav.twofac.cli.di

import org.koin.core.logger.Level
import org.koin.dsl.koinApplication
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import kotlin.test.Test

class KoinVerificationTest : KoinTest {

    @Test
    fun shouldInjectKoinModules() {
        koinApplication {
            printLogger(Level.INFO)
            modules(appModule, storageModule)
            @Suppress("DEPRECATION") // verify() not supported outside JVM yet
            checkModules()
        }
    }
}