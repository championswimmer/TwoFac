package tech.arnav.twofac

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    initKoin()
    App()
}