package com.example.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.data.lists.ReadingListFolder
import com.example.data.lists.ReadingListRepository
import com.example.ui.components.EmptyState
import com.example.ui.theme.AcademicField
import com.example.ui.theme.Spacing
import com.example.ui.theme.accent
import com.example.ui.theme.eyebrow
import com.example.ui.theme.eyebrowTight
import com.example.ui.theme.numeric

@Composable
fun ReadingListsScreen(
    repository: ReadingListRepository,
    modifier: Modifier = Modifier,
) {
    // modifier param so LibraryScreen can pass weight(1f); see FieldsScreen for why.
    val folders by repository.observeFolders().collectAsStateWithLifecycle(initialValue = null)
    var showCreate by remember { mutableStateOf(false) }

    if (showCreate) {
        NewFolderDialog(
            onDismiss = { showCreate = false },
            onCreate = { title, description, isPrivate, field ->
                repository.createFolder(title, description, isPrivate, field)
                showCreate = false
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.md,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "READING LISTS",
                style = MaterialTheme.typography.eyebrow,
                color = MaterialTheme.colorScheme.primary,
            )
            FilledTonalButton(
                // Was a no-op. Reading lists had no repository to write to at all.
                onClick = { showCreate = true },
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text("New folder", style = MaterialTheme.typography.labelLarge)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.feedGutter),
        ) {
            val loaded = folders
            if (loaded == null) {
                // Matches the feed: never show an empty state before the data has arrived.
                item(key = "loading") { Spacer(modifier = Modifier.height(Spacing.xxl)) }
            } else if (loaded.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = "No reading lists",
                        message = "Group papers into folders to keep a project's sources together.",
                        icon = Icons.AutoMirrored.Outlined.LibraryBooks,
                    )
                }
            } else {
                items(loaded, key = { it.id }) { folder ->
                    ReadingListCard(
                        folder = folder,
                        onDelete = { repository.deleteFolder(folder.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
fun ReadingListCard(
    folder: ReadingListFolder,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accent = folder.field.accent()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            // Field-tinted header strip, so a list of folders reads as colour-coded by discipline.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent.container)
                    .padding(horizontal = Spacing.base, vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Outlined.LibraryBooks,
                        contentDescription = null,
                        tint = accent.onContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        folder.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = accent.onContainer,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (folder.isPrivate) Icons.Outlined.Lock else Icons.Outlined.Public,
                        contentDescription = if (folder.isPrivate) "Private" else "Shared",
                        tint = accent.onContainer,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete ${folder.title}",
                        tint = accent.onContainer,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onDelete)
                            .padding(Spacing.xxs)
                            .size(18.dp),
                    )
                }
            }

            Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                Text(
                    folder.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${folder.paperCount} papers",
                        style = MaterialTheme.typography.numeric,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (folder.isPrivate) "PRIVATE" else "SHARED",
                        style = MaterialTheme.typography.eyebrowTight,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                    )
                }
            }
        }
    }
}

/**
 * Create dialog for a reading list. Deliberately small: a title is the only required field, since
 * a create flow that demands five answers is one people abandon.
 */
@Composable
private fun NewFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Boolean, AcademicField) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(true) }
    var field by remember { mutableStateOf(AcademicField.PHYSICS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New reading list", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description") },
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                )

                Spacer(modifier = Modifier.height(Spacing.base))
                Text(
                    "FIELD",
                    style = MaterialTheme.typography.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                // Horizontally scrollable rather than wrapped: six chips do not fit a dialog's
                // width, and a wrapped grid makes the dialog jump height as rows reflow.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    AcademicField.entries.forEach { entry ->
                        FilterChip(
                            selected = field == entry,
                            onClick = { field = entry },
                            label = { Text(entry.label, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (isPrivate) "Private" else "Shared",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(checked = !isPrivate, onCheckedChange = { isPrivate = !it })
                }
            }
        },
        confirmButton = {
            // Disabled rather than silently ignoring an empty title, which is what the repository
            // would otherwise do.
            TextButton(
                onClick = { onCreate(title, description, isPrivate, field) },
                enabled = title.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
