package tech.arnav.twofac.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import tech.arnav.twofac.settings.DesktopSettingsManager

@Composable
actual fun PlatformSettingsContent() {
    val settingsManager = koinInject<DesktopSettingsManager>()
    val isTrayIconEnabled by settingsManager.isTrayIconEnabledFlow.collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()

    val osName = System.getProperty("os.name").lowercase()
    val trayLabel = when {
        osName.contains("mac") -> "Show Menu Bar icon"
        osName.contains("win") -> "Show System Tray icon"
        else -> "Show System Tray / AppIndicator icon"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = trayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isTrayIconEnabled,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            settingsManager.setTrayIconEnabled(checked)
                        }
                    }
                )
            }
        }
    }
}
