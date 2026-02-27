package tech.arnav.twofac

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
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
        currentActivity = WeakReference(this)

        setContent {
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
