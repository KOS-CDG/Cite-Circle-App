package com.example.ui.lists

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.HomeViewModel
import com.example.R
import com.example.data.CitationFormatter
import com.example.data.CitationStyle
import com.example.data.PdfStore
import com.example.data.SavedPaper
import com.example.data.formatTimeAgo
import com.example.ui.components.EmptyState
import com.example.ui.components.ListRowSkeleton
import com.example.ui.components.RefreshableBox

/**
 * Academic Library & Research Paper Vault.
 *
 * Provides instant access to bookmarked citations and offline research papers (PDFs).
 */
@Composable
fun ReadingListsScreen(viewModel: HomeViewModel, navController: NavController) {
    val saved by viewModel.bookmarks.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = All Saved, 1 = PDF Vault
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(saved.items, selectedTab, searchQuery) {
        val base = if (selectedTab == 1) {
            saved.items.filter { it.pdfLocalPath.isNotBlank() || it.pdfUrl.isNotBlank() }
        } else {
            saved.items
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            val q = searchQuery.trim().lowercase()
            base.filter {
                it.title.lowercase().contains(q) ||
                it.authors.lowercase().contains(q) ||
                it.venue.lowercase().contains(q) ||
                it.content.lowercase().contains(q)
            }
        }
    }

    val pdfCount = remember(saved.items) {
        saved.items.count { it.pdfLocalPath.isNotBlank() || it.pdfUrl.isNotBlank() }
    }

    RefreshableBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.saved_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!saved.isLoading) {
                    Text(
                        pluralStringResource(
                            R.plurals.entry_count,
                            filteredItems.size,
                            filteredItems.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Filter Chips: All Saved vs PDF Vault
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("All Saved (${saved.items.size})") }
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Paper Vault ($pdfCount)") }
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search title, author, or venue...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.small
            )

            when {
                saved.isLoading -> Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { ListRowSkeleton() }
                }

                filteredItems.isEmpty() -> EmptyState(
                    title = if (selectedTab == 1) "No Papers in PDF Vault" else stringResource(R.string.saved_empty_title),
                    message = if (selectedTab == 1) "Save research papers with PDFs attached or open-access links to view them offline in your vault." else stringResource(R.string.saved_empty_message),
                    icon = if (selectedTab == 1) Icons.Outlined.PictureAsPdf else Icons.Outlined.BookmarkBorder
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { paper ->
                        SavedEntryCard(
                            paper = paper,
                            onOpen = { navController.navigate("post/${paper.id}") },
                            onReadPdf = {
                                val encPath = if (paper.pdfLocalPath.isNotBlank()) Uri.encode(paper.pdfLocalPath) else ""
                                val encUrl = if (paper.pdfUrl.isNotBlank()) Uri.encode(paper.pdfUrl) else ""
                                val encTitle = Uri.encode(paper.title.ifBlank { "Research Paper" })
                                navController.navigate("pdf_viewer?path=$encPath&url=$encUrl&title=$encTitle")
                            },
                            onDownloadPdf = {
                                if (paper.pdfUrl.isNotBlank()) {
                                    viewModel.cacheRemotePdf(paper.id, paper.pdfUrl)
                                }
                            },
                            onRemove = { viewModel.toggleBookmark(paper.id, paper.isBookmarked) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedEntryCard(
    paper: SavedPaper,
    onOpen: () -> Unit,
    onReadPdf: () -> Unit,
    onDownloadPdf: () -> Unit,
    onRemove: () -> Unit
) {
    val hasPdf = paper.pdfLocalPath.isNotBlank() || paper.pdfUrl.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        stringResource(
                            R.string.saved_byline,
                            paper.authorName,
                            formatTimeAgo(paper.publishedAt)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = stringResource(R.string.cd_remove_from_saved),
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                )
            }

            // PDF Vault actions
            if (hasPdf) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (paper.pdfLocalPath.isNotBlank()) {
                                "Offline (${PdfStore.getFormattedSize(paper.pdfLocalPath)})"
                            } else {
                                "Open Access Cloud PDF"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (paper.pdfLocalPath.isBlank() && paper.pdfUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = onDownloadPdf,
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cache Offline", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Button(
                            onClick = onReadPdf,
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Text("Read PDF", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
