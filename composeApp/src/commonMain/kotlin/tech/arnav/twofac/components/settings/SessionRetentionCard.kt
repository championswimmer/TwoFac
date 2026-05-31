package tech.arnav.twofac.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import tech.arnav.twofac.theme.TwoFacTheme

@Composable
fun SessionRetentionCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    RememberPasskeyCard(
        title = title,
        description = description,
        isEnabled = isEnabled,
        onEnabledChanged = onEnabledChanged,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun SessionRetentionCardPreview() {
    TwoFacTheme {
        SessionRetentionCard(
            title = "Remember authentication for this browser session",
            description = "After a successful secure unlock, keep the vault unlocked until the browser closes.",
            isEnabled = true,
            onEnabledChanged = {},
        )
    }
}
