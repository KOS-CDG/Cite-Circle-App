package com.example.ui.post

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.HomeViewModel
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
 * A post in the feed.
 *
 * Tapping anywhere that is not an action opens the detail screen, where the comment thread
 * lives. Pass a null [onClick] on the detail screen itself, where the card is not a link.
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
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(4.dp), spotColor = Color(0x0D1A1A1A))
            .then(onClick?.let { action -> Modifier.clickable { action() } } ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
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
                        "CITED FROM ${paper.quotedAuthorName.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            PostHeader(paper)

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            if (paper.content.isNotBlank()) {
                Text(
                    paper.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
            }

            if (paper.imageUri.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                PostImage(paper.imageUri)
            }

            if (paper.isQuote) {
                Spacer(Modifier.height(16.dp))
                QuotedCard(paper)
            }

            Spacer(Modifier.height(20.dp))
            CitationBlock(paper)

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
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                paper.authorInitials,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    paper.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "• ${formatTimeAgo(paper.publishedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                paper.affiliation,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** An image attached to a post, loaded from the copy [ImageStore] made in app storage. */
@Composable
fun PostImage(path: String) {
    AsyncImage(
        model = File(path),
        contentDescription = "Attached image",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                RoundedCornerShape(4.dp)
            )
    )
}

/** The snapshot of the post being quoted, rendered as an inset card. */
@Composable
fun QuotedCard(paper: SavedPaper) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                RoundedCornerShape(4.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            paper.quotedAuthorName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (paper.quotedTitle.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                paper.quotedTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (paper.quotedContent.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                paper.quotedContent,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
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
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PostAction(
                icon = if (paper.isEndorsed) Icons.Filled.Verified else Icons.Outlined.Verified,
                contentDescription = if (paper.isEndorsed) "Remove endorsement" else "Endorse",
                count = paper.endorsementCount,
                active = paper.isEndorsed,
                onClick = { viewModel.toggleEndorsement(paper.id, paper.isEndorsed) }
            )
            PostAction(
                icon = Icons.Outlined.ChatBubbleOutline,
                contentDescription = if (commentOpensThread) "Open discussion" else "Comments",
                count = paper.commentCount,
                enabled = commentOpensThread,
                onClick = { navController.navigate("post/${paper.id}") }
            )
            PostAction(
                icon = Icons.Outlined.Repeat,
                contentDescription = "Cite this post",
                count = paper.repostCount,
                onClick = { navController.navigate("quote/${paper.id}") }
            )
            PostAction(
                icon = if (paper.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (paper.isBookmarked) "Remove bookmark" else "Save to reading list",
                active = paper.isBookmarked,
                onClick = { viewModel.toggleBookmark(paper.id, paper.isBookmarked) }
            )
            PostAction(
                icon = Icons.Outlined.Share,
                contentDescription = "Share",
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
    onClick: () -> Unit
) {
    val tint = if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            // Keeps the tap target at the 48dp minimum without inflating the visual row.
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp), tint = tint)
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = tint
            )
        }
    }
}

/** The dark citation slab, with style switching and working .bib/.ris export. */
@Composable
fun CitationBlock(paper: SavedPaper) {
    var style by remember { mutableStateOf(CitationStyle.DEFAULT) }
    val styleable = CitationFormatter.isStyleable(paper)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CITATION",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
                if (styleable) {
                    Row {
                        CitationStyle.entries.forEach { option ->
                            val selected = option == style
                            Text(
                                option.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (selected) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .clickable { style = option }
                                    .border(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                        else Color.Transparent,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    // Legacy rows carry a verbatim citation string that cannot honestly be
                    // restyled, so no toggle is offered rather than one that does nothing.
                    Text(
                        "VERBATIM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Text(
                CitationFormatter.format(paper, style),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                ExportFormat.entries.forEach { format ->
                    OutlinedButton(
                        onClick = { scope.launch { ShareUtils.shareExport(context, paper, format) } },
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = "Export ${format.label}",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            format.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
