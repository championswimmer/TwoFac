package tech.arnav.twofac.components.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.arnav.twofac.lib.storage.StoredTag
import tech.arnav.twofac.lib.storage.TagColor
import tech.arnav.twofac.theme.TwoFacTheme
import twofac.composeapp.generated.resources.Res
import twofac.composeapp.generated.resources.action_cancel
import twofac.composeapp.generated.resources.action_delete
import twofac.composeapp.generated.resources.tags_color_label
import twofac.composeapp.generated.resources.tags_create_button
import twofac.composeapp.generated.resources.tags_delete_confirm_message
import twofac.composeapp.generated.resources.tags_delete_confirm_title
import twofac.composeapp.generated.resources.tags_dialog_done
import twofac.composeapp.generated.resources.tags_dialog_title
import twofac.composeapp.generated.resources.tags_empty_message
import twofac.composeapp.generated.resources.tags_name_label
import twofac.composeapp.generated.resources.tags_name_placeholder

/**
 * Dialog that provides full CRUD for tags plus (optional) assignment of tags
 * to an account.
 *
 * When [assignedTagIds] / [onAssignmentChanged] are non-null the dialog also
 * shows checkboxes so the user can toggle which tags belong to the account.
 *
 * @param allTags              All tags currently in the vault.
 * @param assignedTagIds       Tag IDs currently assigned to the account (null = management-only mode).
 * @param onAssignmentChanged  Called when the user toggles a tag assignment.
 * @param onCreateTag          Called with (name, color) when user creates a new tag.
 * @param onUpdateTag          Called with (tagId, name, color) when user edits a tag.
 * @param onDeleteTag          Called with tagId when user deletes a tag.
 * @param onDismiss            Called when the dialog is closed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManageTagsDialog(
    allTags: List<StoredTag>,
    assignedTagIds: List<String>? = null,
    onAssignmentChanged: ((tagId: String, assigned: Boolean) -> Unit)? = null,
    onCreateTag: (name: String, color: TagColor) -> Unit,
    onUpdateTag: (tagId: String, name: String, color: TagColor) -> Unit,
    onDeleteTag: (tagId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editingTag by remember { mutableStateOf<StoredTag?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }
    var pendingDeleteTagId by remember { mutableStateOf<String?>(null) }

    // For the create/edit form
    var formName by remember { mutableStateOf("") }
    var formColor by remember { mutableStateOf(TagColor.BLUE) }

    // Reset form when edit target changes
    fun openEdit(tag: StoredTag) {
        editingTag = tag
        formName = tag.name
        formColor = tag.color
        showCreateForm = false
    }

    fun openCreate() {
        editingTag = null
        formName = ""
        formColor = TagColor.BLUE
        showCreateForm = true
    }

    // Confirm-delete dialog (shown on top of main dialog)
    if (pendingDeleteTagId != null) {
        val tagToDelete = allTags.find { it.tagId == pendingDeleteTagId }
        AlertDialog(
            onDismissRequest = { pendingDeleteTagId = null },
            title = { Text(stringResource(Res.string.tags_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        Res.string.tags_delete_confirm_message,
                        tagToDelete?.name.orEmpty(),
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteTagId?.let { onDeleteTag(it) }
                    pendingDeleteTagId = null
                }) {
                    Text(stringResource(Res.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTagId = null }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.tags_dialog_title)) },
        text = {
            Column {
                // ── Tag list ────────────────────────────────────────────────
                if (allTags.isEmpty() && !showCreateForm) {
                    Text(
                        text = stringResource(Res.string.tags_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(allTags, key = { it.tagId }) { tag ->
                            val isAssigned = assignedTagIds?.contains(tag.tagId) == true
                            TagListRow(
                                tag = tag,
                                showAssignment = assignedTagIds != null,
                                isAssigned = isAssigned,
                                isEditing = editingTag?.tagId == tag.tagId,
                                onAssignToggle = {
                                    onAssignmentChanged?.invoke(tag.tagId, !isAssigned)
                                },
                                onEditClick = { openEdit(tag) },
                                onDeleteClick = { pendingDeleteTagId = tag.tagId },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Inline create / edit form ────────────────────────────────
                if (showCreateForm || editingTag != null) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    TagEditForm(
                        name = formName,
                        color = formColor,
                        onNameChange = { formName = it },
                        onColorChange = { formColor = it },
                        onConfirm = {
                            if (formName.isNotBlank()) {
                                val editing = editingTag
                                if (editing != null) {
                                    onUpdateTag(editing.tagId, formName, formColor)
                                } else {
                                    onCreateTag(formName, formColor)
                                }
                                editingTag = null
                                showCreateForm = false
                                formName = ""
                                formColor = TagColor.BLUE
                            }
                        },
                        onCancel = {
                            editingTag = null
                            showCreateForm = false
                        },
                    )
                } else {
                    TextButton(
                        onClick = { openCreate() },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text(stringResource(Res.string.tags_create_button))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.tags_dialog_done))
            }
        },
    )
}

@Composable
private fun TagListRow(
    tag: StoredTag,
    showAssignment: Boolean,
    isAssigned: Boolean,
    isEditing: Boolean,
    onAssignToggle: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val bgColor = if (isEditing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(enabled = showAssignment, onClick = onAssignToggle)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showAssignment) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAssigned) tag.color.toComposeColor()
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isAssigned) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(tag.color.toComposeColor(), CircleShape)
            )
        }

        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditForm(
    name: String,
    color: TagColor,
    onNameChange: (String) -> Unit,
    onColorChange: (TagColor) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(Res.string.tags_name_label)) },
            placeholder = { Text(stringResource(Res.string.tags_name_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(Res.string.tags_color_label),
            style = MaterialTheme.typography.labelMedium,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TagColor.palette.forEach { tagColor ->
                val isSelected = tagColor == color
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(tagColor.toComposeColor())
                        .then(
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            else Modifier
                        )
                        .clickable { onColorChange(tagColor) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(Res.string.action_cancel))
            }
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(Res.string.tags_create_button))
            }
        }
    }
}

@Preview
@Composable
private fun ManageTagsDialogPreview() {
    TwoFacTheme {
        ManageTagsDialog(
            allTags = listOf(
                StoredTag("1", "Work", TagColor.BLUE),
                StoredTag("2", "Finance", TagColor.GREEN),
            ),
            assignedTagIds = listOf("1"),
            onAssignmentChanged = { _, _ -> },
            onCreateTag = { _, _ -> },
            onUpdateTag = { _, _, _ -> },
            onDeleteTag = {},
            onDismiss = {},
        )
    }
}
