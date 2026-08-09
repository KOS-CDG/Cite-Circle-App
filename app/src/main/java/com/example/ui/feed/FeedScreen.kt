package com.example.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.ui.components.Avatar
import com.example.ui.components.EmptyState
import com.example.ui.components.PostCard
import com.example.ui.components.PostCardSkeleton
import com.example.ui.components.rememberShimmerBrush
import com.example.ui.theme.Spacing

@Composable
fun FeedScreen(
    viewModel: HomeViewModel,
    onOpenComposer: () -> Unit,
) {
    val papersState by viewModel.savedPapers.collectAsStateWithLifecycle()
    var openComments by remember { mutableStateOf<String?>(null) }
    val shimmer = rememberShimmerBrush()

    // Captured into a plain local because a delegated property has a custom getter and so cannot
    // be smart-cast: the null check below would not narrow the type of `papersState` itself.
    val papers = papersState

    openComments?.let { postId ->
        CommentSheet(
            viewModel = viewModel,
            postId = postId,
            onDismiss = { openComments = null },
        )
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
        item(key = "composer") {
            ComposerRow(onClick = onOpenComposer)
        }

        when {
            // Still waiting on Room. Placeholders rather than the empty state, which used to
            // flash for a frame on every cold start.
            papers == null -> {
                items(3, key = { "skeleton-$it" }) { index ->
                    PostCardSkeleton(brush = shimmer, withImage = index == 1)
                }
            }

            papers.isEmpty() -> {
                item(key = "empty") {
                    // The feed previously had no empty state at all -- it rendered a blank column.
                    EmptyState(
                        title = "Nothing here yet",
                        message = "Posts from researchers you follow will appear here.",
                        icon = Icons.AutoMirrored.Outlined.Article,
                    )
                }
            }

            else -> {
                items(papers, key = { it.id }) { paper ->
                    PostCard(
                        paper = paper,
                        onReact = { reaction ->
                            viewModel.setReaction(paper.id, reaction.key, paper.myReaction)
                        },
                        onComment = { openComments = paper.id },
                        // animateItem (not the removed animateItemPlacement) so a newly composed
                        // post slides its neighbours down rather than making them jump.
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/** Facebook's "What's on your mind?" row, adapted. Tapping it opens the full composer. */
@Composable
private fun ComposerRow(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(initials = "JD", seed = "u-me", size = Spacing.avatarMd)
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(
                "Share a finding, Jane?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = Spacing.base, vertical = Spacing.md),
            )
        }
    }
}
