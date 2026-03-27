package tech.arnav.twofac.components.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.*

@Composable
fun OnboardingGuideHeader(
    completedCount: Int,
    totalCount: Int,
    requiredRemainingCount: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.onboarding_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.onboarding_progress, completedCount.toString(), totalCount.toString()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (requiredRemainingCount > 0) {
                Text(
                    text = if (requiredRemainingCount == 1)
                        stringResource(Res.string.onboarding_required_remaining_singular, requiredRemainingCount.toString())
                    else
                        stringResource(Res.string.onboarding_required_remaining_plural, requiredRemainingCount.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
