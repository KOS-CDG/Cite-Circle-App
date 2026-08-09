package com.example.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.ui.components.Avatar
import com.example.ui.components.EmptyState
import com.example.ui.theme.Spacing
import com.example.ui.theme.eyebrow
import com.example.ui.theme.meta
import com.example.util.TimeFormat

/**
 * Comments live in a bottom sheet rather than a screen: the post stays visible behind it, which
 * is what makes a comment thread feel attached to what you were reading.
 *
 * Deliberately flat -- no nested replies. Deeper trees are unreadable at phone width, and the
 * data model has no parent pointer yet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentSheet(
    viewModel: HomeViewModel,
    postId: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val comments by viewModel.comments(postId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var draft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .imePadding(),
        ) {
            Text(
                "COMMENTS",
                style = MaterialTheme.typography.eyebrow,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.sm,
                ),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(modifier = Modifier.weight(1f)) {
                if (comments.isEmpty()) {
                    EmptyState(
                        title = "No comments yet",
                        message = "Be the first to respond to this finding.",
                        icon = Icons.Outlined.ChatBubbleOutline,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = Spacing.screenHorizontal,
                            vertical = Spacing.sm,
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.base),
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            Row(verticalAlignment = Alignment.Top) {
                                Avatar(
                                    initials = comment.authorInitials,
                                    seed = comment.authorId,
                                    size = Spacing.avatarSm,
                                )
                                Spacer(modifier = Modifier.width(Spacing.md))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            comment.authorName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.width(Spacing.sm))
                                        Text(
                                            TimeFormat.relative(comment.createdAt),
                                            style = MaterialTheme.typography.meta,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(Spacing.xxs))
                                    Text(
                                        comment.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(
                        horizontal = Spacing.screenHorizontal,
                        vertical = Spacing.sm,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a comment") },
                    maxLines = 4,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                IconButton(
                    onClick = {
                        viewModel.addComment(postId, draft)
                        draft = ""
                    },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post comment",
                        tint = if (draft.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                }
            }
        }
    }
}
