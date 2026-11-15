package com.example.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ActivityLogEntity
import com.example.data.local.ProjectChecklist
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectPhoto
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SolarCard
import com.example.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProjectPhotosSection(
    project: ProjectEntity,
    photos: List<ProjectPhoto>,
    onTakePhoto: () -> Unit,
    onPreviewPhoto: (ProjectPhoto) -> Unit,
    onCreateQuote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Site Photos (${photos.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedButton(
                onClick = onTakePhoto,
                modifier = Modifier.testTag("take_photo_button_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = "Take Site Photo",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take Photo")
            }
        }

        if (photos.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Capture at least 1 site photo to attach locally and unlock Create Quote.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(photos, key = { it.id }) { photo ->
                    LocalUriThumbnail(
                        uriString = photo.localUri,
                        contentDescription = "Site photo ${photo.id}",
                        onClick = { onPreviewPhoto(photo) },
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .testTag("project_photo_${photo.id}")
                    )
                }
            }

            Button(
                onClick = onCreateQuote,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_quote_button_${project.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.PictureAsPdf,
                    contentDescription = "Create Quote",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Quote")
            }
        }
    }
}

@Composable
fun LocalUriThumbnail(
    uriString: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    maxDimensionPx: Int = 360,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uriString, maxDimensionPx) {
        value = loadSampledBitmapFromUri(context, uriString, maxDimensionPx)
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.PhotoCamera,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private suspend fun loadSampledBitmapFromUri(
    context: Context,
    uriString: String,
    reqSizePx: Int
): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val uri = Uri.parse(uriString)
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > reqSizePx &&
            boundsOpts.outHeight / sampleSize > reqSizePx
        ) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)?.asImageBitmap()
        }
    }.getOrNull()
}

fun LazyListScope.checklistTabItems(
    projects: List<ProjectEntity>,
    selectedProjectId: String?,
    onSelectProject: (String) -> Unit,
    checklistsByProject: Map<String, List<ProjectChecklist>>,
    onToggleItem: (String, Boolean) -> Unit,
    onEditNote: (ProjectChecklist) -> Unit,
    newStepTitle: String,
    onNewStepTitleChange: (String) -> Unit,
    onAddCustomStep: (String, String) -> Unit,
    onRequestCreateProject: () -> Unit,
    maxContentWidth: androidx.compose.ui.unit.Dp
) {
    if (projects.isEmpty()) {
        item {
            Box(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
                EmptyStateCard(
                    icon = Icons.Outlined.Checklist,
                    title = "No projects for checklist",
                    description = "Create a solar project first to track its 12 execution checklist steps.",
                    action = {
                        Button(onClick = onRequestCreateProject) {
                            Text("Create Project")
                        }
                    }
                )
            }
        }
        return
    }

    val activeProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.first()
    val itemsForProject = checklistsByProject[activeProject.id].orEmpty()
    val completedCount = itemsForProject.count { it.isCompleted }
    val totalCount = itemsForProject.size
    val progressFraction = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    item(key = "checklist_header") {
        Column(
            modifier = Modifier
                .widthIn(max = maxContentWidth)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (projects.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    projects.forEach { proj ->
                        FilterChip(
                            selected = proj.id == activeProject.id,
                            onClick = { onSelectProject(proj.id) },
                            label = { Text("${proj.id} • ${proj.name}", maxLines = 1) },
                            modifier = Modifier.testTag("checklist_project_chip_${proj.id}")
                        )
                    }
                }
            }

            SolarCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${activeProject.name} Checklist",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "$completedCount of $totalCount steps completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (totalCount > 0 && completedCount == totalCount) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Checklist complete",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    items(items = itemsForProject, key = { it.id }) { item ->
        SolarCard(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleItem(item.id, !item.isCompleted) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { checked -> onToggleItem(item.id, checked) },
                    modifier = Modifier.testTag("checklist_item_checkbox_${item.id}")
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${item.stepOrder}. ${item.title}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (item.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    if (item.completedAt != null) {
                        Text(
                            text = "Completed ${Formatters.formatDate(item.completedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (item.notes.isNotBlank()) {
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = { onEditNote(item) },
                    modifier = Modifier.testTag("checklist_note_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.NoteAdd,
                        contentDescription = "Edit note for ${item.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    item(key = "add_custom_checklist_step") {
        SolarCard(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newStepTitle,
                    onValueChange = onNewStepTitleChange,
                    label = { Text("Add custom checklist step") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("custom_checklist_step_input")
                )
                OutlinedButton(
                    onClick = { onAddCustomStep(activeProject.id, newStepTitle) },
                    enabled = newStepTitle.trim().length >= 2,
                    modifier = Modifier.testTag("add_checklist_step_button")
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add step", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
    }
}

fun LazyListScope.activityTabItems(
    activities: List<ActivityLogEntity>,
    maxContentWidth: androidx.compose.ui.unit.Dp
) {
    item(key = "activity_header") {
        Box(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
            SectionHeader(
                title = "Activity History (${activities.size})",
                subtitle = "Recorded changes, photos, checklists, and quotes"
            )
        }
    }
    if (activities.isEmpty()) {
        item(key = "activity_empty") {
            SolarCard(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
                Text(
                    text = "No activity recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    } else {
        item(key = "activity_list_card") {
            SolarCard(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    activities.forEachIndexed { idx, act ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "Activity",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = act.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${act.id} • ${Formatters.formatDateTime(act.timestamp)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (idx < activities.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoPreviewDialog(
    photo: ProjectPhoto,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Site Photo (${photo.id})", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LocalUriThumbnail(
                    uriString = photo.localUri,
                    contentDescription = "Full preview of ${photo.id}",
                    maxDimensionPx = 1080,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Text(
                    text = "Captured ${Formatters.formatDateTime(photo.capturedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("delete_photo_confirm_button")
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete Photo", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        }
    )
}

@Composable
fun ChecklistNoteDialog(
    item: ProjectChecklist,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var noteText by remember(item.id) { mutableStateOf(item.notes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = item.title, style = MaterialTheme.typography.titleMedium) },
        text = {
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Step Notes / Reference") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(noteText) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
