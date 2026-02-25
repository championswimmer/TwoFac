package tech.arnav.twofac.di

import org.koin.dsl.module
import tech.arnav.twofac.session.BrowserSessionManager
import tech.arnav.twofac.session.SessionManager

val wasmSessionModule = module {
    single<SessionManager> { BrowserSessionManager() }
}
