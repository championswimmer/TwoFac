package tech.arnav.twofac.app

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import io.github.vinceglb.filekit.manualFileKitCoreInitialization
import tech.arnav.twofac.App
import java.lang.ref.WeakReference

class MainActivity : FragmentActivity() {
    companion object {
        @Volatile
        private var currentActivity: WeakReference<FragmentActivity>? = null

        fun currentActivityOrThrow(): FragmentActivity {
            val activityRef = currentActivity
            return requireNotNull(activityRef?.get()) { "No active FragmentActivity available for biometric prompt" }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        FileKit.manualFileKitCoreInitialization(applicationContext)
        FileKit.init(this)
        currentActivity = WeakReference(this)

        setContent {
            val darkMode = LocalConfiguration.current.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    WindowInsetsControllerCompat(window, view).apply {
                        isAppearanceLightStatusBars = !darkMode
                        isAppearanceLightNavigationBars = !darkMode
                    }
                }
            }
            App()
        }
    }

    override fun onDestroy() {
        if (currentActivity?.get() === this) {
            currentActivity = null
        }
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
