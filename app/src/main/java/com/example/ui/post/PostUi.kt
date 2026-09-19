package com.example.ui.post

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.Avatar
import com.example.HomeViewModel
import com.example.R
import com.example.data.CitationFormatter
import com.example.data.CitationStyle
import com.example.data.ExportFormat
import com.example.data.PdfStore
import com.example.data.SavedPaper
import com.example.data.formatTimeAgo
import com.example.data.isQuote
import com.example.data.security.DocumentFormat
import com.example.ui.share.ShareUtils
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerLight
import com.example.ui.theme.SurfaceInset
import kotlinx.coroutines.launch
import java.io.File

/**
 * A post in the feed: a flat full-bleed surface on the neutral page.
 *
 * No Card wrapper, no border, no shadow. Separation comes from the page colour showing through
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .then(onClick?.let { action -> Modifier.clickable { action() } } ?: Modifier)
    ) {
        if (paper.isQuote) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            ) {
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
        }

        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            PostHeader(paper, viewModel, navController)
        }

        if (paper.content.isNotBlank()) {
            Text(
                paper.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (paper.imageUri.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            PostImage(paper.imageUri) {
                navController.navigate("image/" + Uri.encode(paper.imageUri))
            }
            Spacer(Modifier.height(8.dp))
        }

        if (paper.isQuote) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                QuotedCard(
                    paper = paper,
                    onClick = if (paper.quotedId.isNotBlank()) {
                        { navController.navigate("post/${paper.quotedId}") }
                    } else null
                )
            }
        }

        if (paper.abstractText.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PaperAbstractSection(paper.abstractText)
            }
        }

        if (paper.pdfLocalPath.isNotBlank() || paper.pdfUrl.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PaperPdfBadge(
                    paper = paper,
                    onReadPdf = {
                        val encPath = if (paper.pdfLocalPath.isNotBlank()) Uri.encode(paper.pdfLocalPath) else ""
                        val encUrl = if (paper.pdfUrl.isNotBlank()) Uri.encode(paper.pdfUrl) else ""
                        val encTitle = Uri.encode(paper.title.ifBlank { "Research Paper" })
                        navController.navigate("pdf_viewer?path=$encPath&url=$encUrl&title=$encTitle")
                    }
                )
            }
        }

        if (CitationFormatter.isStyleable(paper) || paper.citationOverride.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                CitationBlock(paper, viewModel)
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            SocialProofBar(
                paper = paper,
                onClickComments = {
                    if (onClick != null) onClick() else navController.navigate("post/${paper.id}")
                }
            )
        }

        Spacer(Modifier.height(4.dp))
        // On the detail screen the thread is already below, so the comment count is
        // informational there rather than a link back to the screen we are on.
        PostActionBar(paper, viewModel, navController, commentOpensThread = onClick != null)
    }
}

@Composable
fun PostHeader(
    paper: SavedPaper,
    viewModel: HomeViewModel? = null,
    navController: NavController? = null
) {
    var showOverflow by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(
            initials = paper.authorInitials,
            size = 42.dp,
            modifier = if (navController != null) {
                Modifier.clickable { navController.navigate("profile") }
            } else Modifier
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (navController != null) {
                        Modifier.clickable { navController.navigate("profile") }
                    } else Modifier
                )
        ) {
            Text(
                paper.authorName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                paper.affiliation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${formatTimeAgo(paper.publishedAt)} · ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Filled.Public,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box {
            IconButton(onClick = { showOverflow = true }) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                DropdownMenuItem(
                    text = { Text(if (paper.isBookmarked) "Remove from Vault" else "Save to Vault") },
                    onClick = {
                        showOverflow = false
                        viewModel?.toggleBookmark(paper.id, paper.isBookmarked)
                    },
                    leadingIcon = { Icon(Icons.Filled.Bookmark, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Copy BibTeX Citation") },
                    onClick = {
                        showOverflow = false
                        val bib = CitationFormatter.export(paper, ExportFormat.BIBTEX)
                        clipboardManager.setText(AnnotatedString(bib))
                        viewModel?.report("BibTeX citation copied to clipboard")
                    },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Hide Post") },
                    onClick = {
                        showOverflow = false
                        viewModel?.report("Post hidden from feed")
                    },
                    leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Unfollow Researcher") },
                    onClick = {
                        showOverflow = false
                        viewModel?.report("Unfollowed ${paper.authorName}")
                    },
                    leadingIcon = { Icon(Icons.Outlined.PersonRemove, contentDescription = null) }
                )
            }
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
            .then(onClick?.let { action -> Modifier.clickable { action() } } ?: Modifier)
    )
}

/** The snapshot of the post being quoted, rendered as an inset panel. */
@Composable
fun QuotedCard(paper: SavedPaper, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceInset)
            .border(0.5.dp, DividerLight, MaterialTheme.shapes.small)
            .then(onClick?.let { action -> Modifier.clickable { action() } } ?: Modifier)
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

/** Reaction + social counts summary bar above the action divider. */
@Composable
private fun SocialProofBar(paper: SavedPaper, onClickComments: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReactionChip(Icons.Filled.ThumbUp, BrandBlue)
            Spacer(Modifier.width((-4).dp))
            ReactionChip(Icons.Filled.Lightbulb, MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width((-4).dp))
            ReactionChip(Icons.Filled.School, MaterialTheme.colorScheme.secondary)
            if (paper.endorsementCount > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    paper.endorsementCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (paper.commentCount > 0 || paper.repostCount > 0) {
            Text(
                buildString {
                    if (paper.commentCount > 0) append("${paper.commentCount} comments")
                    if (paper.commentCount > 0 && paper.repostCount > 0) append(" · ")
                    if (paper.repostCount > 0) append("${paper.repostCount} citations")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.then(onClickComments?.let { Modifier.clickable { it() } } ?: Modifier)
            )
        }
    }
}

@Composable
private fun ReactionChip(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(tint),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
    }
}

/** Facebook-style 3-action bar: Endorse, Comment, Cite. */
@Composable
fun PostActionBar(
    paper: SavedPaper,
    viewModel: HomeViewModel,
    navController: NavController,
    commentOpensThread: Boolean = true
) {
    Column {
        HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FacebookPostAction(
                icon = if (paper.isEndorsed) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                label = "Endorse",
                active = paper.isEndorsed,
                activeColor = BrandBlue,
                onClick = { viewModel.toggleEndorsement(paper.id, paper.isEndorsed) },
                modifier = Modifier.weight(1f)
            )
            FacebookPostAction(
                icon = Icons.Outlined.ModeComment,
                label = "Comment",
                active = false,
                onClick = { if (commentOpensThread) navController.navigate("post/${paper.id}") },
                modifier = Modifier.weight(1f)
            )
            FacebookPostAction(
                icon = Icons.Outlined.Repeat,
                label = "Cite",
                active = false,
                onClick = { navController.navigate("quote/${paper.id}") },
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
    }
}

@Composable
private fun FacebookPostAction(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    activeColor: Color = BrandBlue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (active) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = tint)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
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

@Composable
fun PaperPdfBadge(
    paper: SavedPaper,
    onReadPdf: () -> Unit
) {
    val format = if (paper.pdfLocalPath.isNotBlank()) {
        PdfStore.getDocumentFormat(paper.pdfLocalPath)
    } else {
        DocumentFormat.PDF
    }
    val isPdf = format == DocumentFormat.PDF

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(SurfaceInset)
            .clickable(onClick = onReadPdf)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                if (isPdf) Icons.Outlined.PictureAsPdf else Icons.Outlined.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Full Manuscript Available (${format.label})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (paper.pdfLocalPath.isNotBlank()) "Downloaded in Vault (${PdfStore.getFormattedSize(paper.pdfLocalPath)}) · Tap to read"
                    else if (paper.openAccess) "Open Access PDF · Tap to stream & read"
                    else "Preprint Manuscript · Tap to read",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            if (isPdf) "Read PDF" else "Open ${format.label}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = BrandBlue,
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraSmall)
                .background(BrandBlue.copy(alpha = 0.10f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun PaperAbstractSection(abstractText: String) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(0.5.dp, DividerLight, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Abstract",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (expanded) "Collapse" else "Expand",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            abstractText,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
