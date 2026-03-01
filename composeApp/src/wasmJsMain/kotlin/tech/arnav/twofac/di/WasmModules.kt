package tech.arnav.twofac.di

import org.koin.dsl.module
import tech.arnav.twofac.session.BrowserSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.session.WebAuthnSessionManager

val wasmSessionModule = module {
    single<WebAuthnSessionManager> { BrowserSessionManager() }
    single<SessionManager> { get<WebAuthnSessionManager>() }
}
