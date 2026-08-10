package com.example.ui.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.HomeViewModel
import com.example.data.CitationFormatter
import com.example.data.CitationStyle
import com.example.data.SavedPaper
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.CiteCircleTheme
import com.example.ui.theme.TextPrimaryLight
import kotlinx.coroutines.launch

/** Holds the hosted [ComposeView] across recompositions without being observable state. */
private class CardViewHolder {
    var view: ComposeView? = null
}

/**
 * Draws a laid-out [View] into a software [Bitmap].
 *
 * The card is hosted in a [ComposeView] specifically so there is a real View to capture:
 * `View.draw` renders the whole subtree into any Canvas, which is stable across every API
 * level this app supports.
 */
fun captureView(view: View, backgroundArgb: Int): Bitmap? {
    if (view.width <= 0 || view.height <= 0) return null
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    // ComposeView is transparent by default; paint the page colour so the PNG is opaque.
    canvas.drawColor(backgroundArgb)
    view.draw(canvas)
    return bitmap
}

/**
 * The social card itself: a 4:5 portrait crop that reads well in a timeline.
 *
 * Deliberately pinned to the light palette regardless of the app's theme — a shared image
 * outlives the setting that produced it, so every card that leaves the app looks the same.
 */
@Composable
fun ShareCardContent(paper: SavedPaper, style: CitationStyle) {
    val citation = CitationFormatter.format(paper, style)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .padding(36.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cite Circle",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = BrandBlue
            )
            Text(
                style.label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimaryLight.copy(alpha = 0.45f)
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = BrandBlue.copy(alpha = 0.25f), thickness = 1.dp)
        Spacer(Modifier.height(28.dp))

        Text(
            paper.title.ifBlank { paper.content },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Normal,
                lineHeight = 36.sp
            ),
            color = BrandBlue,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        if (paper.authors.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                paper.authors.split(';').joinToString(" · ") { it.trim() },
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimaryLight.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (paper.venue.isNotBlank() || paper.year.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                listOf(paper.venue, paper.year).filter { it.isNotBlank() }.joinToString(" • "),
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimaryLight.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(BrandBlue)
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Citation",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = SurfaceWhite.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    citation,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
                    color = SurfaceWhite,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(BrandBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    paper.authorInitials,
                    color = SurfaceWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    paper.authorName,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimaryLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    paper.affiliation,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextPrimaryLight.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Preview-and-share screen: shows exactly the image that will be posted, lets the citation
 * style be switched, then exports it as a PNG or as plain text.
 */
@Composable
fun SharePreviewScreen(paperId: String, viewModel: HomeViewModel, navController: NavController) {
    val papers by viewModel.savedPapers.collectAsStateWithLifecycle()
    val paper = papers.firstOrNull { it.id == paperId }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var style by rememberSaveable { mutableStateOf(CitationStyle.DEFAULT) }
    // A plain holder rather than a MutableState: the view is only ever read from a click
    // handler, and writing state from AndroidView's factory would schedule a recomposition.
    val cardHolder = remember { CardViewHolder() }
    val backgroundArgb = SurfaceWhite.toArgb()

    if (paper == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "This post is no longer available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
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
                    "Share",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CitationStyle.entries.forEach { option ->
                    val selected = option == style
                    Text(
                        option.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                                MaterialTheme.shapes.extraLarge
                            )
                            .clickable { style = option }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Hosted in a ComposeView so the rendered card can be captured to a Bitmap.
            AndroidView(
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f),
                factory = { ctx -> ComposeView(ctx).also { cardHolder.view = it } },
                update = { view ->
                    view.setContent {
                        CiteCircleTheme(darkTheme = false) {
                            ShareCardContent(paper, style)
                        }
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val bitmap = cardHolder.view?.let { captureView(it, backgroundArgb) }
                        if (bitmap == null) {
                            snackbarHostState.showSnackbar("Card is not ready yet.")
                        } else {
                            ShareUtils.shareCardImage(
                                context = context,
                                bitmap = bitmap,
                                paper = paper,
                                caption = CitationFormatter.format(paper, style)
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Share as image",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { ShareUtils.sharePostText(context, paper, style) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(
                    Icons.Outlined.Notes,
                    contentDescription = null,
                    Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Share as text",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
