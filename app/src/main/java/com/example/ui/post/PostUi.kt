package com.example.ui.post

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.Avatar
import com.example.HomeViewModel
import com.example.R
import com.example.data.CitationFormatter
import com.example.data.CitationStyle
import com.example.data.ExportFormat
import com.example.data.SavedPaper
import com.example.data.formatTimeAgo
import com.example.data.isQuote
import com.example.ui.share.ShareUtils
import kotlinx.coroutines.launch
import java.io.File

/**
 * A post in the feed: a flat white card on the neutral page.
 *
 * Cards carry no border and no shadow. Separation comes from the page colour showing through
 * the gaps between them, which is how both reference apps build a feed — a hairline outline
 * on every card reads as a form, not a stream.
 *
 * Tapping anywhere that is not an action opens the detail screen. Pass a null [onClick] on
 * the detail screen itself, where the card is not a link.
 */
@Composable
fun PostCard(
    paper: SavedPaper,
    viewModel: HomeViewModel,
    navController: NavController,
    onClick: (() -> Unit)? = { navController.navigate("post/${paper.id}") }
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(onClick?.let { action -> Modifier.clickable { action() } } ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (paper.isQuote) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.post_cited_from, paper.quotedAuthorName),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            PostHeader(paper)

            if (paper.content.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    paper.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (paper.imageUri.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                PostImage(paper.imageUri) {
                    navController.navigate("image/" + Uri.encode(paper.imageUri))
                }
            }

            if (paper.isQuote) {
                Spacer(Modifier.height(12.dp))
                QuotedCard(paper)
            }

            Spacer(Modifier.height(12.dp))
            CitationBlock(paper, viewModel)

            Spacer(Modifier.height(4.dp))
            // On the detail screen the thread is already below, so the comment count is
            // informational there rather than a link back to the screen we are on.
            PostActionBar(paper, viewModel, navController, commentOpensThread = onClick != null)
        }
    }
}

@Composable
fun PostHeader(paper: SavedPaper) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(paper.authorInitials, 40.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                paper.authorName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                paper.affiliation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatTimeAgo(paper.publishedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** An image attached to a post, loaded from the copy ImageStore made in app storage. */
@Composable
fun PostImage(path: String, onClick: (() -> Unit)? = null) {
    AsyncImage(
        model = File(path),
        contentDescription = stringResource(R.string.cd_attached_figure_expandable),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .clip(MaterialTheme.shapes.small)
            .then(onClick?.let { action -> Modifier.clickable { action() } } ?: Modifier)
    )
}

/** The snapshot of the post being quoted, rendered as an inset panel. */
@Composable
fun QuotedCard(paper: SavedPaper) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                MaterialTheme.shapes.small
            )
            .padding(14.dp)
    ) {
        Text(
            paper.quotedAuthorName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (paper.quotedTitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                paper.quotedTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (paper.quotedContent.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                paper.quotedContent,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Endorse, comment, repost, bookmark, share — with live counts. */
@Composable
fun PostActionBar(
    paper: SavedPaper,
    viewModel: HomeViewModel,
    navController: NavController,
    commentOpensThread: Boolean = true
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PostAction(
                icon = if (paper.isEndorsed) Icons.Filled.Verified else Icons.Outlined.Verified,
                contentDescription = if (paper.isEndorsed) {
                    stringResource(R.string.cd_remove_endorsement)
                } else {
                    stringResource(R.string.cd_endorse)
                },
                count = paper.endorsementCount,
                active = paper.isEndorsed,
                activeColor = MaterialTheme.colorScheme.secondary,
                onClick = { viewModel.toggleEndorsement(paper.id, paper.isEndorsed) }
            )
            PostAction(
                icon = Icons.Outlined.ChatBubbleOutline,
                contentDescription = if (commentOpensThread) {
                    stringResource(R.string.cd_open_discussion)
                } else {
                    stringResource(R.string.cd_comments)
                },
                count = paper.commentCount,
                enabled = commentOpensThread,
                onClick = { navController.navigate("post/${paper.id}") }
            )
            PostAction(
                icon = Icons.Outlined.Repeat,
                contentDescription = stringResource(R.string.cd_cite_post),
                count = paper.repostCount,
                onClick = { navController.navigate("quote/${paper.id}") }
            )
            PostAction(
                icon = if (paper.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (paper.isBookmarked) {
                    stringResource(R.string.cd_remove_bookmark)
                } else {
                    stringResource(R.string.cd_save)
                },
                active = paper.isBookmarked,
                onClick = { viewModel.toggleBookmark(paper.id, paper.isBookmarked) }
            )
            PostAction(
                icon = Icons.Outlined.Share,
                contentDescription = stringResource(R.string.cd_share),
                onClick = { navController.navigate("share/${paper.id}") }
            )
        }
    }
}

@Composable
private fun PostAction(
    icon: ImageVector,
    contentDescription: String,
    count: Int = 0,
    active: Boolean = false,
    enabled: Boolean = true,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val tint = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            // Keeps the tap target at the 48dp minimum without inflating the visual row.
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp), tint = tint)
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
        }
    }
}

/**
 * The citation panel: the one deliberately saturated surface in the app, and the thing that
 * makes a post recognisable as coming from Cite Circle when it is screenshotted.
 */
@Composable
fun CitationBlock(paper: SavedPaper, viewModel: HomeViewModel) {
    var style by remember { mutableStateOf(CitationStyle.DEFAULT) }
    val styleable = CitationFormatter.isStyleable(paper)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.citation_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
            )
            if (styleable) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CitationStyle.entries.forEach { option ->
                        val selected = option == style
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (selected) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .clickable { style = option }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            } else {
                // Legacy rows carry a verbatim citation string that cannot honestly be
                // restyled, so no toggle is offered rather than one that does nothing.
                Text(
                    stringResource(R.string.citation_verbatim),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Text(
            CitationFormatter.format(paper, style),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            ExportFormat.entries.forEach { format ->
                // Resolved here rather than in the click handler: that lambda is a coroutine
                // body, not a composable, so it cannot read resources itself.
                val exportFailed = stringResource(R.string.export_failed, format.label)
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                ShareUtils.shareExport(context, paper, format)
                            } catch (e: Exception) {
                                // Writing to cache or resolving a chooser can both fail; the
                                // button used to just do nothing visible.
                                viewModel.report(exportFailed)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.45f)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(format.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
