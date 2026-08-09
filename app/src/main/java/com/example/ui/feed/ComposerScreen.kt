package com.example.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.HomeViewModel
import com.example.ui.theme.AcademicField
import com.example.ui.theme.Spacing
import com.example.ui.theme.accent
import com.example.ui.theme.eyebrow

/**
 * A full screen rather than a bottom sheet. Sheets and soft keyboards fight each other, and a
 * full screen leaves room for the field picker and a citation field without the whole thing
 * collapsing when the IME opens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(
    viewModel: HomeViewModel,
    onClose: () -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var citation by remember { mutableStateOf("") }
    var field by remember { mutableStateOf(AcademicField.PHYSICS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New post") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.createPost(content, citation, field.key)
                            onClose()
                        },
                        enabled = content.isNotBlank(),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.padding(end = Spacing.sm),
                    ) {
                        Text("Post", style = MaterialTheme.typography.labelLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = Spacing.screenHorizontal)
                .imePadding(),
        ) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                "FIELD",
                style = MaterialTheme.typography.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AcademicField.entries.forEach { candidate ->
                    val accent = candidate.accent()
                    FilterChip(
                        selected = field == candidate,
                        onClick = { field = candidate },
                        label = { Text(candidate.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.container,
                            selectedLabelColor = accent.onContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.base))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("Share a finding, Jane?") },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            )

            Spacer(modifier = Modifier.height(Spacing.md))
            OutlinedTextField(
                value = citation,
                onValueChange = { citation = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Citation (optional)") },
                maxLines = 3,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
            )
            Spacer(modifier = Modifier.height(Spacing.base))
        }
    }
}
