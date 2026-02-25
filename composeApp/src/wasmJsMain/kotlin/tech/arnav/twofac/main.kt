package tech.arnav.twofac

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import tech.arnav.twofac.di.wasmSessionModule

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin {
        modules(wasmSessionModule)
    }

    ComposeViewport(document.body!!) {
        App()
    }
}