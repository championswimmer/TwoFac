package tech.arnav.twofac.components.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.arnav.twofac.lib.storage.StoredTag
import tech.arnav.twofac.lib.storage.TagColor
import tech.arnav.twofac.theme.TwoFacTheme

/**
 * A horizontally scrollable row of tag filter chips.
 * Tapping a chip toggles it as the active filter; tapping an already-selected
 * chip clears the filter.
 *
 * @param tags           All available tags to display as filters.
 * @param selectedTagId  The [StoredTag.tagId] of the currently active filter, or null for no filter.
 * @param onTagSelected  Called with the tag's id when a chip is tapped, or null to clear the filter.
 */
@Composable
fun TagFilterRow(
    tags: List<StoredTag>,
    selectedTagId: String?,
    onTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(tags, key = { it.tagId }) { tag ->
            val isSelected = tag.tagId == selectedTagId
            TagChip(
                tag = tag,
                selected = isSelected,
                onClick = {
                    onTagSelected(if (isSelected) null else tag.tagId)
                },
            )
        }
    }
}

@Preview
@Composable
private fun TagFilterRowPreview() {
    TwoFacTheme {
        TagFilterRow(
            tags = listOf(
                StoredTag("1", "Work", TagColor.BLUE),
                StoredTag("2", "Finance", TagColor.GREEN),
                StoredTag("3", "Social", TagColor.PINK),
            ),
            selectedTagId = "2",
            onTagSelected = {},
        )
    }
}
