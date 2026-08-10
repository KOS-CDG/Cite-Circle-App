package com.example.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.Avatar
import com.example.HomeViewModel
import com.example.ListState
import com.example.data.Comment
import com.example.data.formatTimeAgo
import com.example.ui.components.CommentSkeleton

/**
 * A single post with its comment thread.
 *
 * Delete is offered unconditionally: this is a local Room database, so every row in it
 * belongs to the person holding the phone. There is no server-side ownership to check yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(paperId: String, viewModel: HomeViewModel, navController: NavController) {
    val papers by viewModel.savedPapers.collectAsStateWithLifecycle()
    val paper = papers.firstOrNull { it.id == paperId }

    // Remembered per id: calling comments() on every recomposition would build a new Flow
    // and restart the query each time.
    val commentFlow = remember(paperId) { viewModel.comments(paperId) }
    val commentState by commentFlow.collectAsStateWithLifecycle(initialValue = ListState())
    val comments = commentState.items

    var draft by rememberSaveable { mutableStateOf("") }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (paper == null) {
        MissingPost(navController)
        return
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    "Withdraw entry?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    "This removes the post, its ${comments.size} comment" +
                        (if (comments.size == 1) "" else "s") +
                        " and any attached image from this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.removePaper(paper)
                    navController.popBackStack()
                }) {
                    Text(
                        "Withdraw",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    "Entry",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Withdraw entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // onClick null: already on the detail screen, so the card is not a link.
                PostCard(paper, viewModel, navController, onClick = null)
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (comments.isEmpty()) "Discussion" else "Discussion · ${comments.size}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when {
                commentState.isLoading -> items(2) { CommentSkeleton() }

                comments.isEmpty() -> item {
                    Text(
                        "No responses yet. Start the discussion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> items(comments, key = { it.id }) { comment ->
                    CommentRow(comment, onDelete = { viewModel.deleteComment(comment) })
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        CommentComposer(
            value = draft,
            onValueChange = { draft = it },
            onSend = {
                viewModel.addComment(paper.id, draft)
                draft = ""
            }
        )
    }
}

@Composable
private fun CommentRow(comment: Comment, onDelete: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Avatar(comment.authorInitials, 32.dp)
        Spacer(Modifier.width(12.dp))
        // The bubble is what separates a reply from the post body at a glance.
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    comment.authorName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatTimeAgo(comment.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                comment.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Delete comment",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentComposer(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Add to the discussion…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Spacer(Modifier.width(8.dp))
            val enabled = value.isNotBlank()
            IconButton(onClick = onSend, enabled = enabled) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Post comment",
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun MissingPost(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "This entry is no longer available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { navController.popBackStack() }) {
            Text(
                "Back to feed",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
