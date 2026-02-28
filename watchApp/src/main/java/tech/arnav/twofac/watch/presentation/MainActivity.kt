package tech.arnav.twofac.watch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.arnav.twofac.watch.datalayer.WatchCompanionRegistrar
import tech.arnav.twofac.watch.otp.WatchOtpProvider
import tech.arnav.twofac.watch.presentation.theme.TwofacTheme
import tech.arnav.twofac.watch.storage.WatchSyncSnapshotRepository
import tech.arnav.twofac.watch.ui.EmptyState
import tech.arnav.twofac.watch.ui.OtpPagerScreen
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)
        lifecycleScope.launch {
            WatchSyncSnapshotRepository.get(applicationContext).initialize()
        }

        setContent {
            WearApp()
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun WearApp() {
    TwofacTheme {
        val context = LocalContext.current
        val repository = remember(context) { WatchSyncSnapshotRepository.get(context.applicationContext) }
        val registrar = remember(context) { WatchCompanionRegistrar(context.applicationContext) }
        val otpProvider = remember { WatchOtpProvider() }

        LaunchedEffect(repository) {
            repository.initialize()
        }

        // Collect OTP codes refreshed every second via ticker
        val snapshotFlow = remember(repository) { repository.state.map { it.snapshot } }
        val otpEntries by otpProvider.ticker(snapshotFlow).collectAsState(initial = emptyList())

        // Wall clock ticking frequently for smooth countdown arc animation
        var currentEpochMillis by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
        LaunchedEffect(Unit) {
            while (isActive) {
                currentEpochMillis = Clock.System.now().toEpochMilliseconds()
                delay(33)
            }
        }

        val state by repository.state.collectAsState()

        if (state.snapshot == null || otpEntries.isEmpty()) {
            EmptyState(registrar = registrar)
        } else {
            OtpPagerScreen(
                entries = otpEntries,
                currentEpochMillis = currentEpochMillis,
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}
