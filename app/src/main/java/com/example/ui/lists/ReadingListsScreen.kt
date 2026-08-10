package com.example.ui.lists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.HomeViewModel
import com.example.data.CitationFormatter
import com.example.data.CitationStyle
import com.example.data.SavedPaper
import com.example.data.formatTimeAgo
import com.example.ui.components.EmptyState
import com.example.ui.components.ListRowSkeleton
import com.example.ui.components.RefreshableBox

/**
 * Everything bookmarked from the feed.
 *
 * This used to render three hardcoded folders with invented paper counts. It now reflects
 * real state: the bookmark action on a post is what puts an entry here.
 */
@Composable
fun ReadingListsScreen(viewModel: HomeViewModel, navController: NavController) {
    val saved by viewModel.bookmarks.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    RefreshableBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Saved",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!saved.isLoading) {
                    Text(
                        "${saved.items.size} ${if (saved.items.size == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                saved.isLoading -> Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { ListRowSkeleton() }
                }

                saved.isEmpty -> EmptyState(
                    title = "Nothing saved yet",
                    message = "Bookmark an entry in your feed and it will appear here.",
                    icon = Icons.Outlined.BookmarkBorder
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(saved.items, key = { it.id }) { paper ->
                        SavedEntryCard(
                            paper = paper,
                            onOpen = { navController.navigate("post/${paper.id}") },
                            onRemove = { viewModel.toggleBookmark(paper.id, paper.isBookmarked) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedEntryCard(paper: SavedPaper, onOpen: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        paper.title.ifBlank { paper.content },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${paper.authorName} · ${formatTimeAgo(paper.publishedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = "Remove from saved",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (CitationFormatter.isStyleable(paper) || paper.citationOverride.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    CitationFormatter.format(paper, CitationStyle.DEFAULT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                )
            }
        }
    }
}
