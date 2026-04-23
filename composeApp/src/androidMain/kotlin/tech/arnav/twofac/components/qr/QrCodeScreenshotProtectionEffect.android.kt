package tech.arnav.twofac.components.qr

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun QrCodeScreenshotProtectionEffect(enabled: Boolean) {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(activity, enabled) {
        if (!enabled || activity == null) {
            onDispose {}
        } else {
            val window = activity.window
            val flagSecure = WindowManager.LayoutParams.FLAG_SECURE
            val alreadySecure = (window.attributes.flags and flagSecure) != 0
            if (!alreadySecure) {
                window.addFlags(flagSecure)
            }
            onDispose {
                if (!alreadySecure) {
                    window.clearFlags(flagSecure)
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
