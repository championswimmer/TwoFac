package tech.arnav.twofac.watch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.arnav.twofac.watch.datalayer.WatchCompanionInitResult
import tech.arnav.twofac.watch.datalayer.WatchCompanionRegistrar
import tech.arnav.twofac.watch.otp.WatchOtpEntry
import tech.arnav.twofac.watch.otp.WatchOtpProvider
import tech.arnav.twofac.watch.presentation.theme.TwofacTheme
import tech.arnav.twofac.watch.storage.WatchSyncSnapshotRepository
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

        // Wall clock ticking every second for countdown arc
        var currentEpochSec by remember { mutableLongStateOf(Clock.System.now().epochSeconds) }
        LaunchedEffect(Unit) {
            while (isActive) {
                currentEpochSec = Clock.System.now().epochSeconds
                delay(1000)
            }
        }

        val state by repository.state.collectAsState()

        if (state.snapshot == null || otpEntries.isEmpty()) {
            EmptyState(registrar = registrar)
        } else {
            OtpPagerScreen(
                entries = otpEntries,
                currentEpochSec = currentEpochSec,
            )
        }
    }
}

@Composable
private fun OtpPagerScreen(
    entries: List<WatchOtpEntry>,
    currentEpochSec: Long,
) {
    val pagerState = rememberPagerState(pageCount = { entries.size })

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            OtpAccountScreen(
                entry = entries[page],
                currentEpochSec = currentEpochSec,
            )
        }

        // Time display at the top, only on first page to avoid clutter
        if (pagerState.currentPage == 0) {
            TimeText()
        }

        // Page indicator dots on the right edge
        if (entries.size > 1) {
            PageDots(
                pageCount = entries.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
            )
        }
    }
}

@Composable
private fun PageDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val dotColor = MaterialTheme.colors.primary
    val inactiveColor = MaterialTheme.colors.onBackground.copy(alpha = 0.3f)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(pageCount.coerceAtMost(8)) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 6.dp else 4.dp)
                    .background(
                        color = if (index == currentPage) dotColor else inactiveColor,
                        shape = CircleShape,
                    )
            )
        }
    }
}

@Composable
private fun EmptyState(registrar: WatchCompanionRegistrar) {
    val scope = rememberCoroutineScope()
    var isInitializing by remember { mutableStateOf(false) }
    var initMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        TimeText()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                text = "No accounts synced yet.\nOpen phone app to sync.",
            )
            Button(
                onClick = {
                    scope.launch {
                        isInitializing = true
                        initMessage = when (registrar.requestInitialization()) {
                            WatchCompanionInitResult.RequestSent -> "Sync requested. Open phone app."
                            WatchCompanionInitResult.CompanionAppMissing -> "Companion app not found."
                            WatchCompanionInitResult.CompanionNotReachable -> "Phone not reachable."
                        }
                        isInitializing = false
                    }
                },
                enabled = !isInitializing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(if (isInitializing) "Requesting..." else "Sync now")
            }
            if (initMessage != null) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    text = initMessage!!,
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}
