package com.example.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.data.CollectionEntity
import com.example.data.SavedPaper

val ACADEMIC_COLLECTION_COLORS = listOf(
    "#1A73E8", // Scholar Blue
    "#0D904F", // Research Emerald
    "#EA4335", // Crimson
    "#F9AB00", // Archive Amber
    "#9334E6", // Thesis Purple
    "#007B83", // Teal Journal
    "#E37400", // Tangerine
    "#5F6368"  // Slate Grey
)

val COLLECTION_ICON_OPTIONS = listOf(
    "folder" to Icons.Outlined.Folder,
    "book" to Icons.Outlined.Book,
    "star" to Icons.Outlined.Star,
    "code" to Icons.Outlined.Code,
    "science" to Icons.Outlined.Science,
    "bookmark" to Icons.Outlined.Bookmark
)

fun parseCollectionColor(hex: String, fallback: Color = Color(0xFF1A73E8)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

fun getCollectionIcon(name: String): ImageVector = when (name.lowercase()) {
    "book" -> Icons.Outlined.Book
    "star" -> Icons.Outlined.Star
    "code" -> Icons.Outlined.Code
    "science" -> Icons.Outlined.Science
    "bookmark" -> Icons.Outlined.Bookmark
    else -> Icons.Outlined.Folder
}

/**
 * Dialog to create a new research collection/folder or edit an existing one.
 */
@Composable
fun CreateEditCollectionDialog(
    initialCollection: CollectionEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, colorHex: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf(initialCollection?.name.orEmpty()) }
    var description by remember { mutableStateOf(initialCollection?.description.orEmpty()) }
    var selectedColor by remember { mutableStateOf(initialCollection?.colorHex ?: ACADEMIC_COLLECTION_COLORS.first()) }
    var selectedIcon by remember { mutableStateOf(initialCollection?.iconName ?: "folder") }
    var isNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialCollection == null) "New Collection" else "Edit Collection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) isNameError = false
                    },
                    label = { Text("Collection Name *") },
                    placeholder = { Text("e.g. LLM Reasoning, Thesis Ch. 2") },
                    singleLine = true,
                    isError = isNameError,
                    supportingText = if (isNameError) {
                        { Text("Collection name cannot be blank") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Research topic, project objectives, or notes...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Icon Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Folder Icon",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        COLLECTION_ICON_OPTIONS.forEach { (iconKey, iconVector) ->
                            val isSelected = selectedIcon == iconKey
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { selectedIcon = iconKey },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = iconKey,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Color Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Color Tag",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ACADEMIC_COLLECTION_COLORS.forEach { hex ->
                            val color = parseCollectionColor(hex)
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                2.5.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                CircleShape
                                            )
                                        } else Modifier
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isNameError = true
                    } else {
                        onConfirm(name.trim(), description.trim(), selectedColor, selectedIcon)
                        onDismiss()
                    }
                }
            ) {
                Text(if (initialCollection == null) "Create Collection" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog allowing researchers to organize a paper into one or more collections.
 */
@Composable
fun OrganizePaperDialog(
    paper: SavedPaper,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    val collectionsState by viewModel.collections.collectAsStateWithLifecycle()
    val initialCollectionIds by viewModel.getCollectionIdsForPaper(paper.id).collectAsStateWithLifecycle(emptyList())

    // Local set tracking checked collections
    var selectedIds by remember(initialCollectionIds) { mutableStateOf(initialCollectionIds.toSet()) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateEditCollectionDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc, colorHex, iconName ->
                viewModel.createCollection(name, desc, colorHex, iconName)
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Organize in Collections",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    paper.title.ifBlank { paper.content },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))

                if (collectionsState.items.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No Collections Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Create your first collection to group papers by research project or topic.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(collectionsState.items, key = { it.id }) { col ->
                            val isChecked = selectedIds.contains(col.id)
                            val colColor = parseCollectionColor(col.colorHex)
                            val icon = getCollectionIcon(col.iconName)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedIds = if (isChecked) {
                                            selectedIds - col.id
                                        } else {
                                            selectedIds + col.id
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) {
                                            selectedIds + col.id
                                        } else {
                                            selectedIds - col.id
                                        }
                                    }
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = colColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        col.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (col.description.isNotBlank()) {
                                        Text(
                                            col.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${col.paperCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // Quick Add Collection button
                OutlinedButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New Collection", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.setPaperCollections(paper.id, selectedIds)
                            onDismiss()
                        }
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

/**
 * Confirmation dialog before deleting a collection.
 */
@Composable
fun DeleteCollectionDialog(
    collectionName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text("Delete Collection?") },
        text = {
            Text(
                "Are you sure you want to delete '$collectionName'?\n\nPapers inside this collection will NOT be deleted from your saved library.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
