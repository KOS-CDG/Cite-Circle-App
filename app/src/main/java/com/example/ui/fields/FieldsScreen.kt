package com.example.ui.fields

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmptyState
import com.example.ui.theme.AcademicField
import com.example.ui.theme.Spacing
import com.example.ui.theme.accent
import com.example.ui.theme.numeric

/**
 * Takes a modifier because it is hosted inside DiscoverScreen's tab Column. Without one the
 * caller cannot pass `weight(1f)`, and a fillMaxSize root would claim the whole column height
 * and push itself past the tab row.
 */
@Composable
fun FieldsScreen(
    onOpenField: (AcademicField) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val fields = AcademicField.entries.filter {
        it.label.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.screenHorizontal),
            placeholder = { Text("Search academic fields...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            shape = MaterialTheme.shapes.medium,
        )

        if (fields.isEmpty()) {
            EmptyState(
                title = "No Fields Found",
                message = "Try adjusting your search criteria.",
                icon = Icons.Outlined.FolderOff,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(fields, key = { it.key }) { field ->
                    FieldRow(field, onClick = { onOpenField(field) })
                }
            }
        }
    }
}

@Composable
private fun FieldRow(field: AcademicField, onClick: () -> Unit) {
    val accent = field.accent()
    Card(
        // The row carries a right-chevron, which promises navigation. Until now tapping it did
        // nothing; it opens the People tab filtered to this discipline.
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The colored rail is where the per-field palette earns its keep: six disciplines,
            // six hues, immediately scannable.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .background(accent.base),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.base),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(accent.container),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            field.label.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = accent.onContainer,
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Column {
                        Text(
                            field.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            "${field.researcherCount} active researchers",
                            style = MaterialTheme.typography.numeric,
                            color = accent.base,
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Placeholder counts. These were previously computed as `(index + 1) * 120` inline in the list,
 * which made them shift as soon as the list was filtered -- searching changed the numbers.
 * Pinning them to the field keeps them stable until there is a real source.
 */
private val AcademicField.researcherCount: Int
    get() = when (this) {
        AcademicField.PHYSICS -> 1240
        AcademicField.BIOLOGY -> 2180
        AcademicField.HISTORY -> 640
        AcademicField.LINGUISTICS -> 890
        AcademicField.COGNITIVE -> 1510
        AcademicField.ECONOMICS -> 1120
    }
