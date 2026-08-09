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
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AcademicField
import com.example.ui.theme.Spacing
import com.example.ui.theme.accent
import com.example.ui.theme.eyebrow
import com.example.ui.theme.eyebrowTight
import com.example.ui.theme.numeric

data class ReadingListFolder(
    val id: String,
    val title: String,
    val description: String,
    val paperCount: Int,
    val isPrivate: Boolean,
    val field: AcademicField,
)

val sampleLists = listOf(
    ReadingListFolder(
        "1",
        "LLM Epistemology",
        "Papers covering the semantic shifts in large models.",
        12,
        isPrivate = true,
        field = AcademicField.LINGUISTICS,
    ),
    ReadingListFolder(
        "2",
        "Thermodynamics in Archival Tech",
        "Foundational texts for my upcoming grant proposal.",
        4,
        isPrivate = false,
        field = AcademicField.PHYSICS,
    ),
    ReadingListFolder(
        "3",
        "Decentralized Science",
        "DAO structures and reputation mechanics.",
        28,
        isPrivate = false,
        field = AcademicField.ECONOMICS,
    ),
)

@Composable
fun ReadingListsScreen(modifier: Modifier = Modifier) {
    // modifier param so LibraryScreen can pass weight(1f); see FieldsScreen for why.
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
                onClick = { },
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
            items(sampleLists, key = { it.id }) { folder ->
                ReadingListCard(folder)
            }
        }
    }
}

@Composable
fun ReadingListCard(folder: ReadingListFolder) {
    val accent = folder.field.accent()
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        Icons.Outlined.LibraryBooks,
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
                Icon(
                    if (folder.isPrivate) Icons.Outlined.Lock else Icons.Outlined.Public,
                    contentDescription = if (folder.isPrivate) "Private" else "Shared",
                    tint = accent.onContainer,
                    modifier = Modifier.size(16.dp),
                )
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
