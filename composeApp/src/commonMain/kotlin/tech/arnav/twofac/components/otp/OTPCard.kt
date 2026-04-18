package tech.arnav.twofac.components.otp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.*
import tech.arnav.twofac.components.icons.IssuerBrandIcon
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
    nextOtp: String?,
    showUpcomingCode: Boolean = true,
    timeInterval: Long = 30L,
    onCopyOtp: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    fun currentTimeMillis() = Clock.System.now().epochSeconds
    var currentTime by remember { mutableLongStateOf(currentTimeMillis()) }

    var elapsedDuration by remember { mutableLongStateOf(10L) }

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
            animation = tween(
                durationMillis = (timeInterval * 1000).toInt(),
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Adjust the progress to start from the correct position
    val progress = (rawProgress + initialProgress) % 1f

    // Keep the displayed countdown in sync with wall clock time.
    LaunchedEffect(timeInterval) {
        while (true) {
            currentTime = Clock.System.now().epochSeconds
            delay(1000)
        }
    }

    val timeRemaining = timeInterval - (currentTime % timeInterval)
    val timerState = timerStateByElapsedProgress(progress)
    val extendedColors = TwoFacTheme.extendedColors

    val progressColor by animateColorAsState(
        targetValue = when (timerState) {
            TimerState.Healthy -> extendedColors.timerHealthy
            TimerState.Warning -> extendedColors.timerWarning
            TimerState.Critical -> extendedColors.timerCritical
        },
        animationSpec = tween(durationMillis = 500),
        label = "progressColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCopyOtp(otpCode) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IssuerBrandIcon(
                    issuer = account.issuer,
                    size = 32.dp,
                    tint = MaterialTheme.colorScheme.primary,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    account.issuer?.takeIf { it.isNotBlank() }?.let { issuer ->
                        Text(
                            text = issuer.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = account.accountLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // OTP Code Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Current OTP with Swap Animation
                    AnimatedContent(
                        targetState = otpCode,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                slideOutVertically { height -> -height } + fadeOut())
                        },
                        label = "otpAnimation"
                    ) { targetCode ->
                        Text(
                            text = formatOTPCode(targetCode),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 32.sp,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Next OTP hint (visible in the last configured seconds)
                    if (showUpcomingCode) {
                        Column(modifier = Modifier.height(32.dp)) {
                            AnimatedVisibility(
                                visible = nextOtp != null && timeRemaining in 1..elapsedDuration,
                                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                            ) {
                                nextOtp?.let {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = "NEXT",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = formatOTPCode(it),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Time remaining Countdown
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = timeRemaining.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = progressColor
                    )
                    Text(
                        text = "SEC",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
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
                accountID = "google-account",
                accountLabel = "arnav@gmail.com",
                issuer = "Google",
            ),
            otpCode = "123456",
            nextOtp = "987654"
        )
    }
}

private fun formatOTPCode(code: String): String {
    return when (code.length) {
        6 -> "${code.substring(0, 3)} ${code.substring(3)}"
        8 -> "${code.substring(0, 4)} ${code.substring(4)}"
        else -> code
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
