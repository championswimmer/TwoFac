package tech.arnav.twofac.watch.presentation

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import tech.arnav.twofac.watch.otp.WatchOtpEntry

@Composable
fun OtpAccountScreen(
    entry: WatchOtpEntry,
    currentEpochSec: Long,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        if (entry is WatchOtpEntry.Valid && entry.nextRefreshAtEpochSec != null && entry.periodSec != null) {
            CountdownArc(
                currentEpochSec = currentEpochSec,
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
            val issuerOrLabel = entry.issuer?.takeIf { it.isNotBlank() } ?: entry.accountLabel
            Text(
                text = issuerOrLabel,
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            if (entry.issuer != null && entry.issuer.isNotBlank()) {
                Text(
                    text = entry.accountLabel,
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
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                is WatchOtpEntry.Invalid -> {
                    Text(
                        text = "Error",
                        fontSize = 18.sp,
                        color = Color.Red,
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
    currentEpochSec: Long,
    nextRefreshAtEpochSec: Long,
    periodSec: Long,
) {
    val remaining = (nextRefreshAtEpochSec - currentEpochSec).coerceIn(0L, periodSec)
    val progress = remaining.toFloat() / periodSec.toFloat()

    // Color transitions: green (full) → amber (half) → red (nearly expired)
    val arcColor = when {
        progress > 0.5f -> Color(0xFF4CAF50) // green
        progress > 0.25f -> Color(0xFFFF9800) // amber
        else -> Color(0xFFF44336) // red
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 8.dp.toPx()
        val inset = strokeWidth / 2f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

        // Background track (dim ring)
        drawArc(
            color = Color.White.copy(alpha = 0.1f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth),
            topLeft = Offset(inset, inset),
            size = arcSize,
        )

        // Foreground arc — full circle at start, shrinks as time runs out
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
