package tech.arnav.twofac.watch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import tech.arnav.twofac.lib.theme.colorForState
import tech.arnav.twofac.lib.storage.StoredAccount
import tech.arnav.twofac.lib.theme.timerStateByRemainingProgress
import tech.arnav.twofac.watch.R
import tech.arnav.twofac.watch.otp.WatchOtpEntry
import tech.arnav.twofac.watch.presentation.theme.TwofacTheme
import tech.arnav.twofac.watch.presentation.theme.toComposeColor

@Composable
fun OtpAccountScreen(
    entry: WatchOtpEntry,
    currentEpochMillis: Long,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        if (entry is WatchOtpEntry.Valid && entry.nextRefreshAtEpochSec != null && entry.periodSec != null) {
            CountdownArc(
                currentEpochMillis = currentEpochMillis,
                nextRefreshAtEpochSec = entry.nextRefreshAtEpochSec,
                periodSec = entry.periodSec,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val issuer = entry.issuer
            val issuerOrLabel = issuer?.takeIf { it.isNotBlank() } ?: entry.account.accountLabel
            IssuerBrandIcon(
                issuer = issuer,
                size = 18.dp,
                tint = MaterialTheme.colors.onBackground.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                text = issuerOrLabel,
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (!issuer.isNullOrBlank()) {
                Text(
                    text = entry.account.accountLabel,
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }

            when (entry) {
                is WatchOtpEntry.Valid -> {
                    // Format OTP code with a space in the middle for readability (e.g. "123 456")
                    val code = entry.otpCode
                    val formattedCode = if (code.length == 6) {
                        "${code.substring(0, 3)} ${code.substring(3)}"
                    } else {
                        code
                    }
                    Text(
                        text = formattedCode,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                is WatchOtpEntry.Invalid -> {
                    Text(
                        text = stringResource(R.string.watch_otp_error),
                        fontSize = 18.sp,
                        color = TwofacTheme.tokens.danger.toComposeColor(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CountdownArc(
    currentEpochMillis: Long,
    nextRefreshAtEpochSec: Long,
    periodSec: Long,
) {
    val periodMillis = periodSec * 1000L
    val nextRefreshAtMillis = nextRefreshAtEpochSec * 1000L
    val remainingMillis = (nextRefreshAtMillis - currentEpochMillis).coerceIn(0L, periodMillis)
    val progress = remainingMillis.toFloat() / periodMillis.toFloat()
    val tokens = TwofacTheme.tokens
    val timerState = timerStateByRemainingProgress(progress)
    val arcColor = tokens.timer.colorForState(timerState).toComposeColor()
    val trackColor = Color.Black

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 8.dp.toPx()
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            topLeft = Offset(inset, inset),
            size = arcSize,
        )

        if (progress > 0f) {
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset),
                size = arcSize,
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun OtpAccountScreenPreview() {
    TwofacTheme {
        OtpAccountScreen(
            entry = WatchOtpEntry.Valid(
                account = StoredAccount.DisplayAccount(
                    accountID = "preview-account",
                    accountLabel = "arnav@example.com",
                    issuer = "GitHub",
                ),
                issuer = "GitHub",
                otpCode = "123456",
                nextRefreshAtEpochSec = 1_762_304_840L,
                periodSec = 30L,
            ),
            currentEpochMillis = 1_762_304_820_000L,
        )
    }
}
