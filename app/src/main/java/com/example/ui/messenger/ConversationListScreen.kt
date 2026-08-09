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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.Avatar
import com.example.ui.components.EmptyState
import com.example.ui.theme.Spacing
import com.example.ui.theme.eyebrow
import com.example.ui.theme.meta

@Composable
fun ConversationListScreen(
    viewModel: ConversationListViewModel,
    onOpenThread: (String) -> Unit,
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeNow by viewModel.activeNow.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val filtered = conversations.filter {
        it.title.contains(query, ignoreCase = true) ||
            it.preview.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SearchPill(
            query = query,
            onQueryChange = { query = it },
            modifier = Modifier.padding(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.sm,
            ),
        )

        if (activeNow.isNotEmpty()) {
            Text(
                "ACTIVE NOW",
                style = MaterialTheme.typography.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.xs,
                ),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(activeNow, key = { it.id }) { user ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Avatar(
                            initials = user.initials,
                            seed = user.id,
                            size = Spacing.avatarLg,
                            showPresence = true,
                            isOnline = true,
                            ring = true,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            user.name.substringAfter(' ').substringBefore(' '),
                            style = MaterialTheme.typography.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        if (filtered.isEmpty()) {
            EmptyState(
                title = "No conversations",
                message = if (query.isBlank()) {
                    "Start a conversation from a researcher's profile."
                } else {
                    "Nothing matches \"$query\"."
                },
                icon = Icons.Outlined.Forum,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = Spacing.xxl),
            ) {
                items(filtered, key = { it.id }) { row ->
                    ConversationRow(row = row, onClick = { onOpenThread(row.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search messages") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    )
}

@Composable
private fun ConversationRow(
    row: ConversationRowUi,
    onClick: () -> Unit,
) {
    val unread = row.unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // The single clearest unread signal: a tinted row, not just a bolder font.
            .background(
                if (unread) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.background
                },
            )
            .padding(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            initials = row.initials,
            seed = row.id,
            size = 56.dp,
            showPresence = true,
            isOnline = row.isOnline,
        )
        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                row.preview.ifBlank { "No messages yet" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                color = if (unread) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                row.timeLabel,
                style = MaterialTheme.typography.meta,
                color = if (unread) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            if (unread) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text("${row.unreadCount}")
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape),
                )
            }
        }
    }
}
