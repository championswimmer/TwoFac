package tech.arnav.twofac.watch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import tech.arnav.twofac.watch.otp.WatchOtpEntry

@Composable
fun OtpPagerScreen(
    entries: List<WatchOtpEntry>,
    currentEpochMillis: Long,
) {
    val pagerState = rememberPagerState(pageCount = { entries.size })

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            OtpAccountScreen(
                entry = entries[page],
                currentEpochMillis = currentEpochMillis,
            )
        }

        if (pagerState.currentPage == 0) {
            TimeText()
        }

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
