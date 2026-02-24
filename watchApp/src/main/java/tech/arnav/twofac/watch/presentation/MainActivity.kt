package tech.arnav.twofac.watch.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch
import tech.arnav.twofac.watch.datalayer.WatchCompanionInitResult
import tech.arnav.twofac.watch.datalayer.WatchCompanionRegistrar
import tech.arnav.twofac.watch.presentation.theme.TwofacTheme
import tech.arnav.twofac.watch.storage.WatchSyncSnapshotRepository

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

@Composable
fun WearApp() {
    TwofacTheme {
        val context = LocalContext.current
        val repository =
            remember(context) { WatchSyncSnapshotRepository.get(context.applicationContext) }
        val registrar = remember(context) { WatchCompanionRegistrar(context.applicationContext) }
        val state by repository.state.collectAsState()
        val scope = rememberCoroutineScope()
        var isInitializing by remember { mutableStateOf(false) }
        var initMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(repository) {
            repository.initialize()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            if (state.snapshot == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.primary,
                        text = "No secrets yet. Initialize with phone."
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                isInitializing = true
                                initMessage = when (registrar.requestInitialization()) {
                                    WatchCompanionInitResult.RequestSent -> "Init requested. Open phone app to sync."
                                    WatchCompanionInitResult.CompanionAppMissing -> "Companion app not found on phone."
                                    WatchCompanionInitResult.CompanionNotReachable -> "Phone companion not reachable."
                                }
                                isInitializing = false
                            }
                        },
                        enabled = !isInitializing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text(if (isInitializing) "Initializing..." else "Initialize")
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
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    text = "Synced ${state.snapshot?.accounts?.size ?: 0} accounts"
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
