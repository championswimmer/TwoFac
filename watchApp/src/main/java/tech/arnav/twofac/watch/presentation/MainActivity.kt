package tech.arnav.twofac.watch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.watch.datalayer.WatchCompanionRegistrar
import tech.arnav.twofac.watch.otp.WatchOtpEntry
import tech.arnav.twofac.watch.otp.WatchOtpProvider
import tech.arnav.twofac.watch.presentation.theme.TwofacTheme
import tech.arnav.twofac.watch.storage.WatchSyncSnapshotRepository
import tech.arnav.twofac.watch.ui.EmptyState
import tech.arnav.twofac.watch.ui.OtpPagerScreen
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class MainActivity : ComponentActivity() {

    private var isAmbient by mutableStateOf(false)

    private val ambientObserver = AmbientLifecycleObserver(
        this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                isAmbient = true
            }
            override fun onExitAmbient() {
                isAmbient = false
            }
            // onUpdateAmbient fires ~1/min in ambient; no-op — ticker handles OTP refresh.
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)
        lifecycle.addObserver(ambientObserver)

        lifecycleScope.launch {
            WatchSyncSnapshotRepository.get(applicationContext).initialize()
        }

        setContent {
            WearApp(isAmbient = isAmbient)
        }
    }
}

/** Full live composable — wires up real repo, registrar, and OTP provider. */
@OptIn(ExperimentalTime::class)
@Composable
fun WearApp(isAmbient: Boolean = false) {
    TwofacTheme {
        val context = LocalContext.current
        val repository = remember(context) { WatchSyncSnapshotRepository.get(context.applicationContext) }
        val registrar = remember(context) { WatchCompanionRegistrar(context.applicationContext) }
        val otpProvider = remember { WatchOtpProvider() }

        // MainActivity.onCreate already calls repository.initialize(); no duplicate needed here.
        val snapshotFlow = remember(repository) { repository.state.map { it.snapshot } }
        val otpEntries by otpProvider.ticker(snapshotFlow).collectAsState(initial = emptyList())
        val state by repository.state.collectAsState()

        if (state.snapshot == null || otpEntries.isEmpty()) {
            EmptyState(registrar = registrar)
        } else {
            WearAppContent(
                entries = otpEntries,
                isAmbient = isAmbient,
            )
        }
    }
}

/**
 * Pure display composable — no Play Services, no KStore.
 * Accepts pre-built OTP entries so it is safe for use in Compose Previews.
 */
@OptIn(ExperimentalTime::class)
@Composable
fun WearAppContent(
    entries: List<WatchOtpEntry>,
    isAmbient: Boolean = false,
) {
    // Wall-clock ticker for smooth countdown-arc animation.
    // Paused in ambient mode — display cannot refresh at 30 fps when ambient.
    var currentEpochMillis by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(isAmbient) {
        if (!isAmbient) {
            while (isActive) {
                currentEpochMillis = Clock.System.now().toEpochMilliseconds()
                delay(33)
            }
        }
        // Ambient: loop stops; arc shows a static snapshot until interactive mode resumes.
    }

    OtpPagerScreen(
        entries = entries,
        currentEpochMillis = currentEpochMillis,
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    TwofacTheme {
        WearAppContent(
            entries = listOf(
                WatchOtpEntry.Valid(
                    account = StoredAccount.DisplayAccount(
                        accountID = "preview-account",
                        accountLabel = "arnav@example.com",
                    ),
                    issuer = "GitHub",
                    otpCode = "123456",
                    nextRefreshAtEpochSec = 1_762_304_840L,
                    periodSec = 30L,
                ),
            ),
            isAmbient = false,
        )
    }
}
