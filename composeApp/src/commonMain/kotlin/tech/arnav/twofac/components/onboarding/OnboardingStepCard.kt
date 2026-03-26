package tech.arnav.twofac.components.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import twofac.composeapp.generated.resources.*
import tech.arnav.twofac.onboarding.OnboardingGuideStep
import tech.arnav.twofac.onboarding.OnboardingStepIcon

@Composable
fun OnboardingStepCard(
    step: OnboardingGuideStep,
    isCompleted: Boolean,
    isExpanded: Boolean,
    onPrimaryClick: () -> Unit,
    onDoneClick: () -> Unit,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconTint = if (isCompleted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = isCompleted,
                onClick = onToggleExpanded,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isCompleted && !isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = step.icon.toMaterialIcon(),
                            contentDescription = step.title,
                            tint = iconTint,
                        )
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(Res.string.onboarding_expand_step),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Card
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = step.icon.toMaterialIcon(),
                        contentDescription = step.title,
                        tint = iconTint,
                    )
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandLess,
                        contentDescription = stringResource(Res.string.onboarding_collapse_step),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (step.required) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(Res.string.onboarding_required)) },
                )
            }

            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isCompleted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = stringResource(Res.string.onboarding_completed),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(Res.string.onboarding_completed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                step.actionLabel?.let { label ->
                    Button(onClick = onPrimaryClick) {
                        Text(label)
                    }
                }
                if (!isCompleted) {
                    OutlinedButton(onClick = onDoneClick) {
                        Text(stringResource(Res.string.onboarding_done_button))
                    }
                }
            }
        }
    }
}

private fun OnboardingStepIcon.toMaterialIcon(): ImageVector {
    return when (this) {
        OnboardingStepIcon.ACCOUNT -> Icons.Rounded.AccountCircle
        OnboardingStepIcon.MANAGE_ACCOUNTS -> Icons.Rounded.ManageAccounts
        OnboardingStepIcon.SECURE_UNLOCK -> Icons.Rounded.Password
        OnboardingStepIcon.IMPORT_OR_RESTORE -> Icons.Rounded.Restore
        OnboardingStepIcon.BACKUP -> Icons.Rounded.Backup
        OnboardingStepIcon.COMPANION_SYNC -> Icons.Rounded.CloudSync
    }
}
