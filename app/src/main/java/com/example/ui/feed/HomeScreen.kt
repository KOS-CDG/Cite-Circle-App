package com.example.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.EmptyState
import com.example.ui.components.LoadingList
import com.example.ui.components.PaperCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onViewPaper: (String) -> Unit,
    onViewAuthor: (String) -> Unit,
    onComposePaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val papers by viewModel.savedPapers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var pendingDeletionId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            isLoading -> LoadingList()

            papers.isEmpty() -> EmptyState(
                title = "No Papers Yet",
                message = "Publish a preprint or endorse a colleague's work to start building " +
                    "your registry.",
                icon = Icons.Outlined.BookmarkBorder,
                actionLabel = "PUBLISH A PAPER",
                onAction = onComposePaper,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = CiteCircleDefaults.ScreenPadding,
                    end = CiteCircleDefaults.ScreenPadding,
                    top = 8.dp,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                items(papers, key = { it.id }) { paper ->
                    PaperCard(
                        paper = paper,
                        onEndorse = { viewModel.toggleEndorsement(paper.id, paper.isEndorsed) },
                        onViewContext = { onViewPaper(paper.id) },
                        onAuthorClick = { onViewAuthor(paper.id) },
                        onDelete = { pendingDeletionId = paper.id },
                    )
                }
            }
        }

        if (!isLoading) {
            FloatingActionButton(
                onClick = onComposePaper,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(CiteCircleDefaults.ScreenPadding),
                shape = CiteCircleDefaults.CardShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Publish a paper")
            }
        }
    }

    pendingDeletionId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeletionId = null },
            title = { Text("Remove this paper?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "It will be removed from your local registry. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removePaper(id)
                        pendingDeletionId = null
                    },
                ) {
                    Text("REMOVE", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletionId = null }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = CiteCircleDefaults.CardShape,
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}
