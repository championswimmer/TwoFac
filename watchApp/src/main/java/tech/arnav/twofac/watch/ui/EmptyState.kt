package tech.arnav.twofac.watch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import kotlinx.coroutines.launch
import tech.arnav.twofac.watch.R
import tech.arnav.twofac.watch.datalayer.WatchCompanionInitResult
import tech.arnav.twofac.watch.datalayer.WatchCompanionRegistrar

@Composable
fun EmptyState(registrar: WatchCompanionRegistrar) {
    val scope = rememberCoroutineScope()
    var isInitializing by remember { mutableStateOf(false) }
    var initMessage by remember { mutableStateOf<String?>(null) }

    val syncRequestSent = stringResource(R.string.watch_sync_request_sent)
    val syncCompanionMissing = stringResource(R.string.watch_sync_companion_missing)
    val syncCompanionUnreachable = stringResource(R.string.watch_sync_companion_unreachable)
    val syncButtonLabel = stringResource(R.string.watch_sync_button)
    val syncRequestingLabel = stringResource(R.string.watch_sync_requesting)

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
                text = stringResource(R.string.watch_empty_message),
            )
            Button(
                onClick = {
                    scope.launch {
                        isInitializing = true
                        initMessage = when (registrar.requestInitialization()) {
                            WatchCompanionInitResult.RequestSent -> syncRequestSent
                            WatchCompanionInitResult.CompanionAppMissing -> syncCompanionMissing
                            WatchCompanionInitResult.CompanionNotReachable -> syncCompanionUnreachable
                        }
                        isInitializing = false
                    }
                },
                enabled = !isInitializing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(if (isInitializing) syncRequestingLabel else syncButtonLabel)
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
