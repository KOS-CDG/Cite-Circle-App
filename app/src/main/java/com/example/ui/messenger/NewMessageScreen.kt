package com.example.ui.messenger

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.data.messenger.MessengerUser
import com.example.ui.components.Avatar
import com.example.ui.components.EmptyState
import com.example.ui.theme.Spacing
import com.example.ui.theme.meta

/**
 * Recipient picker. Multi-select, because picking more than one person is how a group thread gets
 * created -- InMemoryMessengerRepository.startConversation already handles that, including
 * returning the existing conversation when the same participant set is chosen again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    people: List<MessengerUser>,
    onBack: () -> Unit,
    onStart: (List<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<List<MessengerUser>>(emptyList()) }

    val filtered = people.filter { candidate ->
        candidate.name.contains(query, ignoreCase = true) ||
            candidate.affiliation.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New message") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { onStart(selected.map { it.id }) },
                        enabled = selected.isNotEmpty(),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.padding(end = Spacing.sm),
                    ) {
                        Text(
                            if (selected.size > 1) "Start group" else "Start",
                            style = MaterialTheme.typography.labelLarge,
                        )
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
                .background(MaterialTheme.colorScheme.background),
        ) {
            if (selected.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(
                        horizontal = Spacing.screenHorizontal,
                        vertical = Spacing.sm,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(selected, key = { it.id }) { person ->
                        InputChip(
                            selected = true,
                            onClick = { selected = selected - person },
                            label = { Text(person.name.substringAfter(' ')) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Remove ${person.name}",
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Spacing.screenHorizontal,
                        vertical = Spacing.sm,
                    ),
                placeholder = { Text("Search researchers") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )

            if (filtered.isEmpty()) {
                EmptyState(
                    title = "No matches",
                    message = "Nobody here matches \"$query\".",
                    icon = Icons.Default.Search,
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = Spacing.xxl)) {
                    items(filtered, key = { it.id }) { person ->
                        val isSelected = person in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (isSelected) {
                                        selected - person
                                    } else {
                                        selected + person
                                    }
                                }
                                .padding(
                                    horizontal = Spacing.screenHorizontal,
                                    vertical = Spacing.md,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Avatar(
                                initials = person.initials,
                                seed = person.id,
                                size = Spacing.avatarMd,
                                showPresence = true,
                                isOnline = person.isOnline,
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    person.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    person.affiliation,
                                    style = MaterialTheme.typography.meta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
