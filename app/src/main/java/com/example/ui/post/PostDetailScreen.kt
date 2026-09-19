package com.example.ui.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.launch
import com.example.Avatar
import com.example.HomeViewModel
import com.example.ListState
import com.example.R
import com.example.data.Comment
import com.example.data.formatTimeAgo
import com.example.ui.components.CommentSkeleton
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerLight
import com.example.ui.theme.PageNeutral
import com.example.ui.theme.SurfaceInset
import androidx.compose.ui.graphics.Color

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
    var showSortMenu by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf("Most Relevant") }
    val displayedComments = remember(comments, sortMode) {
        if (sortMode == "Newest") comments.reversed() else comments
    }

    if (paper == null) {
        MissingPost(navController)
        return
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = {
                Text(
                    stringResource(R.string.withdraw_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.withdraw_dialog_message,
                        comments.size,
                        comments.size
                    ),
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
                        stringResource(R.string.action_withdraw),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(
                        stringResource(R.string.action_cancel),
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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    stringResource(R.string.post_detail_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row {
                IconButton(onClick = { navController.navigate("edit/${paper.id}") }) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.cd_edit_entry),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.cd_withdraw_entry),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                // onClick null: already on the detail screen, so the card is not a link.
                PostCard(paper, viewModel, navController, onClick = null)
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(PageNeutral)
                )
            }

            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = rememberCoroutineScope()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (comments.isEmpty()) {
                                stringResource(R.string.discussion)
                            } else {
                                stringResource(R.string.discussion_with_count, comments.size)
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(
                        onClick = {
                            val app = context.applicationContext as com.example.MyApplication
                            scope.launch {
                                val convId = app.chatRepository.startOrGetConversationForPaper(paper)
                                navController.navigate("chat_thread/$convId")
                            }
                        }
                    ) {
                        Text(
                            "Message Author",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceInset)
                                .clickable { showSortMenu = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                sortMode,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Sort comments",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            listOf("Most Relevant", "Newest", "All Comments").forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode) },
                                    onClick = {
                                        sortMode = mode
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            when {
                commentState.isLoading -> items(2) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        CommentSkeleton()
                    }
                }

                comments.isEmpty() -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Text(
                            stringResource(R.string.discussion_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> items(displayedComments, key = { it.id }) { comment ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        CommentRow(
                            comment = comment,
                            onDelete = { viewModel.deleteComment(comment) },
                            onReply = { author -> draft = "@$author " }
                        )
                    }
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
            },
            onAttachFigure = {
                draft = if (draft.isBlank()) "[Figure Reference]" else "$draft [Figure Reference]"
            },
            onQuotePaper = {
                val snippet = "\"${paper.title.ifBlank { paper.content.take(40) }}\" "
                draft = if (draft.isBlank()) snippet else "$draft $snippet"
            }
        )
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    onDelete: () -> Unit,
    onReply: (String) -> Unit = {}
) {
    var isEndorsed by rememberSaveable { mutableStateOf(false) }

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Avatar(comment.authorInitials, 32.dp)
            Spacer(Modifier.width(8.dp))
            // Facebook-style bubble: rounded 16dp corners, no border.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant) // surfaceVariant maps to SurfaceInset
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Row 1: Author name (titleSmall, FontWeight.Bold, onSurface)
                Text(
                    comment.authorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Row 2: Affiliation (bodySmall, onSurfaceVariant) - if not blank
                if (comment.affiliation.isNotBlank()) {
                    Text(
                        comment.affiliation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Row 3: Comment body
                Text(
                    comment.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_delete_comment),
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action footer — sits below the bubble, aligned with the bubble's left edge.
        Row(
            modifier = Modifier.padding(start = 40.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatTimeAgo(comment.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                if (isEndorsed) "Endorsed" else "Endorse",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isEndorsed) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isEndorsed) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { isEndorsed = !isEndorsed }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Reply",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onReply(comment.authorName) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachFigure: () -> Unit = {},
    onQuotePaper: () -> Unit = {}
) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAttachFigure, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = "Camera",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onQuotePaper, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Outlined.FormatQuote,
                    contentDescription = "Quote",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp, max = 120.dp)
                    .clip(RoundedCornerShape(20.dp)),
                placeholder = {
                    Text(
                        stringResource(R.string.comment_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceInset,
                    unfocusedContainerColor = SurfaceInset,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
            Spacer(Modifier.width(8.dp))
            val enabled = value.isNotBlank()
            IconButton(
                onClick = onSend,
                enabled = enabled,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = stringResource(R.string.cd_post_comment),
                    tint = if (enabled) BrandBlue
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
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
            stringResource(R.string.entry_missing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { navController.popBackStack() }) {
            Text(
                stringResource(R.string.action_back_to_feed),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
