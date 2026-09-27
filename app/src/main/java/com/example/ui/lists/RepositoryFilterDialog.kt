package com.example.ui.lists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.HomeViewModel
import com.example.data.SavedPaper

enum class RepositorySortOrder(val label: String) {
    NEWEST("Newest Published"),
    OLDEST("Oldest Published"),
    TITLE_AZ("Title (A-Z)"),
    AUTHOR_AZ("Author (A-Z)")
}

enum class ReadingStatusFilter(val label: String) {
    ALL("All"),
    TO_READ("To Read"),
    READING("Reading"),
    COMPLETED("Read")
}

enum class DocumentTypeFilter(val label: String) {
    ALL("All"),
    PDF_ONLY("Has PDF"),
    OPEN_ACCESS("Open Access")
}

data class RepositoryFilterCriteria(
    val readingStatus: ReadingStatusFilter = ReadingStatusFilter.ALL,
    val documentType: DocumentTypeFilter = DocumentTypeFilter.ALL,
    val sortOrder: RepositorySortOrder = RepositorySortOrder.NEWEST
) {
    val activeFilterCount: Int
        get() = (if (readingStatus != ReadingStatusFilter.ALL) 1 else 0) +
            (if (documentType != DocumentTypeFilter.ALL) 1 else 0) +
            (if (sortOrder != RepositorySortOrder.NEWEST) 1 else 0)
}

/**
 * Filter & Sort Modal Dialog for the academic repository.
 */
@Composable
fun RepositoryFilterDialog(
    currentCriteria: RepositoryFilterCriteria,
    onDismiss: () -> Unit,
    onApply: (RepositoryFilterCriteria) -> Unit
) {
    var statusFilter by remember { mutableStateOf(currentCriteria.readingStatus) }
    var docFilter by remember { mutableStateOf(currentCriteria.documentType) }
    var sortOrder by remember { mutableStateOf(currentCriteria.sortOrder) }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Filter & Sort Papers",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // 1. Reading Status Filter
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Reading Status",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReadingStatusFilter.entries.forEach { option ->
                            val isSelected = statusFilter == option
                            FilterChip(
                                selected = isSelected,
                                onClick = { statusFilter = option },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 2. Document & Access Filter
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Document Attachment",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DocumentTypeFilter.entries.forEach { option ->
                            val isSelected = docFilter == option
                            FilterChip(
                                selected = isSelected,
                                onClick = { docFilter = option },
                                label = { Text(option.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Sort Order
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Sort By",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        RepositorySortOrder.entries.forEach { option ->
                            val isSelected = sortOrder == option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { sortOrder = option }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { sortOrder = option }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            statusFilter = ReadingStatusFilter.ALL
                            docFilter = DocumentTypeFilter.ALL
                            sortOrder = RepositorySortOrder.NEWEST
                        }
                    ) {
                        Text("Reset All")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onApply(
                                    RepositoryFilterCriteria(
                                        readingStatus = statusFilter,
                                        documentType = docFilter,
                                        sortOrder = sortOrder
                                    )
                                )
                                onDismiss()
                            }
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact interactive reading status badge ("To Read", "Reading", "Read").
 */
@Composable
fun ReadingStatusBadge(
    currentStatus: String,
    onStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val (label, color, icon) = when (currentStatus.uppercase()) {
        "READING" -> Triple("Reading", Color(0xFFE37400), Icons.Outlined.AutoStories)
        "COMPLETED", "READ" -> Triple("Read", Color(0xFF0D904F), Icons.Outlined.CheckCircle)
        else -> Triple("To Read", Color(0xFF1A73E8), Icons.Outlined.BookmarkBorder)
    }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.12f),
            border = BorderStroke(0.5.dp, color.copy(alpha = 0.35f)),
            modifier = Modifier.clickable { showMenu = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("To Read") },
                onClick = {
                    showMenu = false
                    onStatusSelected("TO_READ")
                },
                leadingIcon = {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = Color(0xFF1A73E8))
                }
            )
            DropdownMenuItem(
                text = { Text("Currently Reading") },
                onClick = {
                    showMenu = false
                    onStatusSelected("READING")
                },
                leadingIcon = {
                    Icon(Icons.Outlined.AutoStories, contentDescription = null, tint = Color(0xFFE37400))
                }
            )
            DropdownMenuItem(
                text = { Text("Completed (Read)") },
                onClick = {
                    showMenu = false
                    onStatusSelected("COMPLETED")
                },
                leadingIcon = {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF0D904F))
                }
            )
        }
    }
}

/**
 * Dialog for viewing and composing personal research notes on a paper.
 */
@Composable
fun ResearchNotesDialog(
    paper: SavedPaper,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf(paper.researchNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Research Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    paper.title.ifBlank { paper.content },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = {
                        Text(
                            "Add literature notes, experimental takeaways, critique, or questions for this paper...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveResearchNotes(paper.id, notes.trim())
                    onDismiss()
                }
            ) {
                Text("Save Notes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
