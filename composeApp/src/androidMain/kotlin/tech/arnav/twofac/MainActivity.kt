package tech.arnav.twofac

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    companion object {
        @Volatile
        private var currentActivity: FragmentActivity? = null

        fun currentActivityOrThrow(): FragmentActivity {
            return requireNotNull(currentActivity) { "MainActivity is not available" }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        currentActivity = this

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        if (currentActivity === this) {
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
