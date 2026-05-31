package tech.arnav.twofac.components.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.arnav.twofac.lib.theme.AccountColorTag
import tech.arnav.twofac.theme.TwoFacTheme
import tech.arnav.twofac.theme.toComposeColor
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.account_detail_color_none
import twofac.composeapp.generated.resources.account_detail_color_selected
import twofac.composeapp.generated.resources.account_detail_color_title

@Composable
fun AccountColorSelector(
    selectedColor: AccountColorTag?,
    onColorSelected: (AccountColorTag?) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.account_detail_color_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        AccountColorTag.entries.chunked(4).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                rowColors.forEach { colorTag ->
                    AccountColorOption(
                        label = colorTag.displayName,
                        color = colorTag,
                        selected = selectedColor == colorTag,
                        enabled = enabled,
                        onClick = { onColorSelected(colorTag) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - rowColors.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            AccountColorOption(
                label = stringResource(Res.string.account_detail_color_none),
                color = null,
                selected = selectedColor == null,
                enabled = enabled,
                onClick = { onColorSelected(null) },
            )
        }
    }
}

@Composable
fun AccountColorSwatch(
    color: AccountColorTag?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val swatchColor = color
        ?.color(isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme())
        ?.toComposeColor()
        ?: MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(Res.string.account_detail_color_selected),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AccountColorOption(
    label: String,
    color: AccountColorTag?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val optionShape = RoundedCornerShape(999.dp)
    Surface(
        modifier = modifier
            .widthIn(min = 72.dp)
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .clip(optionShape)
            .then(if (enabled) Modifier.clickable(role = Role.RadioButton, onClick = onClick) else Modifier),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = optionShape,
        tonalElevation = if (selected) 1.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AccountColorSwatch(
                color = color,
                selected = selected,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview
@Composable
private fun AccountColorSelectorPreview() {
    TwoFacTheme {
        AccountColorSelector(
            selectedColor = AccountColorTag.TEAL,
            onColorSelected = {},
            enabled = true,
        )
    }
}
