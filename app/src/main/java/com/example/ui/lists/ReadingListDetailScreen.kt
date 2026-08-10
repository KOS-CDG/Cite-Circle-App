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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReadingListEntry
import com.example.data.SampleData
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorState
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionLabel

@Composable
fun ReadingListDetailScreen(
    listId: String?,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val folder = SampleData.readingListById(listId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(
            title = "READING LIST",
            onBack = onBack,
            actions = {
                if (folder != null) {
                    IconButton(onClick = { onEdit(folder.id) }) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit ${folder.title}",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            },
        )

        if (folder == null) {
            ErrorState(
                title = "List Not Found",
                message = "This reading list may have been deleted.",
                actionLabel = "GO BACK",
                onAction = onBack,
            )
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(CiteCircleDefaults.ScreenPadding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        folder.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (folder.isPrivate) Icons.Outlined.Lock else Icons.Outlined.Public,
                        contentDescription = if (folder.isPrivate) {
                            "Private list"
                        } else {
                            "Shared list"
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    folder.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${folder.paperCount} papers • ${if (folder.isPrivate) "PRIVATE" else "SHARED"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(32.dp))
                SectionLabel("IN THIS LIST")
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (folder.entries.isEmpty()) {
                item {
                    EmptyState(
                        title = "Nothing Filed Yet",
                        message = "Papers you add to this folder will be listed here.",
                        icon = Icons.Outlined.LibraryBooks,
                    )
                }
            } else {
                items(folder.entries, key = { it.id }) { entry ->
                    ReadingListEntryCard(entry)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ReadingListEntryCard(entry: ReadingListEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = CiteCircleDefaults.CardShape,
        border = CiteCircleDefaults.cardBorder(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                entry.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    entry.authors,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    entry.year,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                entry.note,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
            )
        }
    }
}
