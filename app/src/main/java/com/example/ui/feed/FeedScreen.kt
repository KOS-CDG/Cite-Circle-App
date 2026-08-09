package com.example.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.ui.components.EmptyState
import com.example.ui.components.PostCard
import com.example.ui.theme.Spacing

@Composable
fun FeedScreen(viewModel: HomeViewModel) {
    val papers by viewModel.savedPapers.collectAsStateWithLifecycle()

    if (papers.isEmpty()) {
        // The feed previously had no empty state at all -- it rendered a blank column.
        EmptyState(
            title = "Nothing here yet",
            message = "Posts from researchers you follow will appear here.",
            icon = Icons.AutoMirrored.Outlined.Article,
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            horizontal = Spacing.screenHorizontal,
            vertical = Spacing.sm,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.feedGutter),
    ) {
        items(papers, key = { it.id }) { paper ->
            PostCard(
                paper = paper,
                onToggleEndorse = viewModel::toggleEndorsement,
            )
        }
    }
}
