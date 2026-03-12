package tech.arnav.twofac.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.arnav.twofac.backup.LocalFileBackupTransport
import tech.arnav.twofac.lib.backup.BackupTransport
import tech.arnav.twofac.qr.ClipboardQRCodeReader
import tech.arnav.twofac.qr.WasmClipboardQRCodeReader
import tech.arnav.twofac.session.BrowserSessionManager
import tech.arnav.twofac.session.SecureSessionManager
import tech.arnav.twofac.session.SessionManager
import tech.arnav.twofac.session.WebAuthnSessionManager

val wasmSessionModule = module {
    single<WebAuthnSessionManager> { BrowserSessionManager() }
    single<SecureSessionManager> { get<WebAuthnSessionManager>() }
    single<SessionManager> { get<WebAuthnSessionManager>() }
}

val wasmQrModule = module {
    single<ClipboardQRCodeReader> { WasmClipboardQRCodeReader() }
}

val wasmBackupModule = module {
    single<BackupTransport>(named("local")) {
        LocalFileBackupTransport()
    }
}
