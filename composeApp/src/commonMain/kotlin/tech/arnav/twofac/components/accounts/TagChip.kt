package tech.arnav.twofac.components.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.arnav.twofac.lib.storage.StoredTag
import tech.arnav.twofac.lib.storage.TagColor
import tech.arnav.twofac.theme.TwoFacTheme

/** Converts the shared-lib [TagColor] to a Compose [Color]. */
fun TagColor.toComposeColor(): Color = Color(argb.toInt())

/**
 * A small color-coded chip that displays a tag name with a colored dot indicator.
 *
 * @param tag      The tag to display.
 * @param onClick  Optional click handler; if null, the chip is non-interactive.
 * @param selected Whether to draw the chip in a "selected/active" style (used for filters).
 */
@Composable
fun TagChip(
    tag: StoredTag,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val tagColor = tag.color.toComposeColor()
    val containerColor = if (selected) {
        tagColor.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        tonalElevation = if (selected) 2.dp else 0.dp,
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(tagColor, CircleShape)
            )
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) tagColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun TagChipPreview() {
    TwoFacTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TagChip(tag = StoredTag(tagId = "1", name = "Work", color = TagColor.BLUE))
            TagChip(tag = StoredTag(tagId = "2", name = "Finance", color = TagColor.GREEN), selected = true)
            TagChip(tag = StoredTag(tagId = "3", name = "Social", color = TagColor.PINK))
        }
    }
}
