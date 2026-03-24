package tech.arnav.twofac.components.otp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import tech.arnav.twofac.lib.theme.TimerState
import tech.arnav.twofac.lib.theme.timerStateByElapsedProgress
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.theme.TwoFacTheme
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun OTPCard(
    account: StoredAccount.DisplayAccount,
    otpCode: String,
    timeInterval: Long = 30L,
    onRefreshOTP: () -> Unit,
    onCopyOtp: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    fun currentTimeMillis() = Clock.System.now().epochSeconds
    var currentTime by remember { mutableLongStateOf(currentTimeMillis()) }


    // Calculate the starting progress and create a synchronized animation
    val startTime = remember { Clock.System.now().epochSeconds }
    val timeInInterval = startTime % timeInterval
    val initialProgress = timeInInterval.toFloat() / timeInterval.toFloat()

    // Create a truly smooth infinite animation that syncs with TOTP timing
    val infiniteTransition = rememberInfiniteTransition(label = "progress")

    // This animation will run continuously, synced to the TOTP interval
    val rawProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (timeInterval * 1000).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Adjust the progress to start from the correct position
    val progress = (rawProgress + initialProgress) % 1f

    // Monitor for OTP refresh - check every second
    LaunchedEffect(Unit) {
        while (true) {
            val newTime = Clock.System.now().epochSeconds

            // Check if we crossed into a new TOTP interval
            if (hasTotpIntervalChanged(currentTime, newTime, timeInterval)) {
                onRefreshOTP()
            }

            currentTime = newTime
            delay(1000) // Check every second for OTP refresh
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCopyOtp(otpCode) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Account label
            Text(
                text = account.accountLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            // OTP Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatOTPCode(otpCode),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Time remaining
                val timeRemaining = timeInterval - (currentTime % timeInterval)
                Text(
                    text = "${timeRemaining}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress bar
            val timerState = timerStateByElapsedProgress(progress)
            val extendedColors = TwoFacTheme.extendedColors
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = when (timerState) {
                    TimerState.Healthy -> extendedColors.timerHealthy
                    TimerState.Warning -> extendedColors.timerWarning
                    TimerState.Critical -> extendedColors.timerCritical
                },
                trackColor = extendedColors.timerTrack ?: MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

internal const val OTP_COPIED_SNACKBAR_MESSAGE = "📋 OTP copied"

@Preview
@Composable
fun OTPCardPreview() {
    TwoFacTheme {
        OTPCard(
            account = StoredAccount.DisplayAccount(
                "arnav@gmail.com",
                accountLabel = "Google",
            ),
            otpCode = "123456",
            onRefreshOTP = {},
        )
    }
}

private fun formatOTPCode(code: String): String {
    return if (code.length == 6) {
        "${code.substring(0, 3)} ${code.substring(3)}"
    } else {
        code
    }
}

internal fun hasTotpIntervalChanged(
    previousEpochSeconds: Long,
    currentEpochSeconds: Long,
    intervalSeconds: Long
): Boolean {
    require(intervalSeconds > 0) { "intervalSeconds must be positive" }
    return previousEpochSeconds / intervalSeconds != currentEpochSeconds / intervalSeconds
}
