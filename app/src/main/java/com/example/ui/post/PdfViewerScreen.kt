package com.example.ui.post

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.PageNeutral
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.data.PdfStore
import com.example.data.security.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * In-App Research Paper PDF Viewer & Excerpt Highlighter.
 *
 * Renders PDF pages with on-demand bitmap rasterization, LRU memory caching,
 * pan-and-zoom controls, night reading mode, page bookmarking, reading progress tracking,
 * excerpt highlighter dialog, and in-reader research notes sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    paperId: String = "",
    initialLocalPath: String = "",
    remoteUrl: String = "",
    paperTitle: String = "Research Paper",
    viewModel: HomeViewModel? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeLocalPath by remember { mutableStateOf(initialLocalPath) }
    var isDownloading by remember { mutableStateOf(initialLocalPath.isBlank() && remoteUrl.isNotBlank()) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var nightMode by remember { mutableStateOf(false) }

    // Observe paper data from Room if paperId is provided
    val paperState = if (paperId.isNotBlank() && viewModel != null) {
        viewModel.paper(paperId).collectAsStateWithLifecycle(initialValue = null).value
    } else null

    // Track active page currently in view
    var activePage by remember { mutableIntStateOf(paperState?.lastReadPage ?: 1) }
    var totalPages by remember { mutableIntStateOf(paperState?.totalPageCount ?: 0) }

    // Parse bookmarked pages
    val bookmarkedPages = remember(paperState?.pageBookmarks) {
        paperState?.pageBookmarks
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    // Modal dialog and bottom sheet states
    var showExcerptDialog by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showAiCoPilotSheet by remember { mutableStateOf(false) }
    var coPilotInitialPrompt by remember { mutableStateOf("") }
    var showBookmarksMenu by remember { mutableStateOf(false) }
    var jumpToPageTarget by remember { mutableStateOf<Int?>(null) }

    // Download remote PDF if no local path exists
    LaunchedEffect(remoteUrl, initialLocalPath) {
        if (activeLocalPath.isBlank() && remoteUrl.isNotBlank()) {
            isDownloading = true
            downloadError = null
            val downloadedPath = withContext(Dispatchers.IO) {
                PdfStore.downloadPdf(context, remoteUrl)
            }
            isDownloading = false
            if (downloadedPath != null) {
                activeLocalPath = downloadedPath
            } else {
                downloadError = "Unable to download research paper PDF. Please check connection."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            paperTitle.ifBlank { "Research Paper" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (activeLocalPath.isNotBlank()) {
                            Text(
                                if (totalPages > 0) "Page $activePage of $totalPages • ${PdfStore.getFormattedSize(activeLocalPath)}"
                                else PdfStore.getFormattedSize(activeLocalPath),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Page bookmark toggle
                    if (paperId.isNotBlank() && viewModel != null && totalPages > 0) {
                        val isBookmarked = bookmarkedPages.contains(activePage)
                        IconButton(onClick = { viewModel.togglePageBookmark(paperId, activePage) }) {
                            Icon(
                                if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove Bookmark" else "Bookmark Page",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Bookmarks list menu
                        if (bookmarkedPages.isNotEmpty()) {
                            Box {
                                IconButton(onClick = { showBookmarksMenu = true }) {
                                    Icon(
                                        Icons.Outlined.Bookmarks,
                                        contentDescription = "Bookmarked Pages",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showBookmarksMenu,
                                    onDismissRequest = { showBookmarksMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Bookmarks (${bookmarkedPages.size})",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        },
                                        onClick = { },
                                        enabled = false
                                    )
                                    bookmarkedPages.sorted().forEach { page ->
                                        DropdownMenuItem(
                                            text = { Text("Jump to Page $page") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Bookmark,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                showBookmarksMenu = false
                                                jumpToPageTarget = page
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Excerpt Highlighter action
                        IconButton(onClick = { showExcerptDialog = true }) {
                            Icon(
                                Icons.Outlined.FormatQuote,
                                contentDescription = "Highlight Excerpt",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Research Notes sheet action
                        IconButton(onClick = { showNotesSheet = true }) {
                            Icon(
                                Icons.Outlined.Notes,
                                contentDescription = "Research Notes",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // AI Research Co-Pilot action
                        IconButton(onClick = {
                            coPilotInitialPrompt = ""
                            showAiCoPilotSheet = true
                        }) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = "AI Research Co-Pilot",
                                tint = BrandBlue
                            )
                        }
                    }

                    // Night reading mode
                    IconButton(onClick = { nightMode = !nightMode }) {
                        Icon(
                            if (nightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (nightMode) "Normal Mode" else "Night Reading Mode"
                        )
                    }

                    // Share document
                    if (activeLocalPath.isNotBlank()) {
                        IconButton(onClick = {
                            sharePdf(context, activeLocalPath, paperTitle)
                        }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share PDF")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (nightMode) Color(0xFF121212) else Color(0xFFE8ECEF)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDownloading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Downloading paper into Vault...",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            remoteUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                downloadError != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            downloadError.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (remoteUrl.isNotBlank()) {
                                    scope.launch {
                                        isDownloading = true
                                        downloadError = null
                                        val path = PdfStore.downloadPdf(context, remoteUrl)
                                        isDownloading = false
                                        if (path != null) activeLocalPath = path
                                        else downloadError = "Retry failed. Check network or open in browser."
                                    }
                                }
                            }
                        ) {
                            Text("Retry Download")
                        }
                        if (remoteUrl.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(remoteUrl))
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Open in Browser")
                            }
                        }
                    }
                }

                activeLocalPath.isNotBlank() -> {
                    val format = PdfStore.getDocumentFormat(activeLocalPath)
                    when {
                        activeLocalPath.endsWith(".pdf", ignoreCase = true) -> {
                            PdfRendererContent(
                                filePath = activeLocalPath,
                                nightMode = nightMode,
                                paperId = paperId,
                                initialPage = paperState?.lastReadPage ?: 1,
                                readingStatus = paperState?.readingStatus.orEmpty(),
                                jumpToPageTarget = jumpToPageTarget,
                                onJumpCompleted = { jumpToPageTarget = null },
                                viewModel = viewModel,
                                onPageChanged = { page, count ->
                                    activePage = page
                                    totalPages = count
                                }
                            )
                        }
                        format == DocumentFormat.TXT ||
                        format == DocumentFormat.MD ||
                        format == DocumentFormat.TEX -> {
                            TextManuscriptContent(
                                filePath = activeLocalPath,
                                nightMode = nightMode
                            )
                        }
                        else -> {
                            WordDocumentContent(
                                filePath = activeLocalPath,
                                paperTitle = paperTitle,
                                format = format
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        "No PDF available for this paper.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Excerpt Highlighter Dialog
            if (showExcerptDialog && paperId.isNotBlank() && viewModel != null) {
                ExcerptHighlighterDialog(
                    pageNumber = activePage,
                    onDismiss = { showExcerptDialog = false },
                    onExplainWithAi = { excerpt ->
                        coPilotInitialPrompt = "Explain this excerpt from page $activePage in context of the paper's core contributions: \"$excerpt\""
                        showExcerptDialog = false
                        showAiCoPilotSheet = true
                    },
                    onSaveExcerpt = { tag, excerpt, commentary ->
                        viewModel.appendExcerptToNotes(
                            paperId = paperId,
                            pageNumber = activePage,
                            tag = tag,
                            excerpt = excerpt,
                            commentary = commentary
                        )
                        showExcerptDialog = false
                    }
                )
            }

            // In-Reader Research Notes Sheet
            if (showNotesSheet && paperId.isNotBlank() && viewModel != null) {
                InReaderResearchNotesSheet(
                    paperTitle = paperTitle,
                    initialNotes = paperState?.researchNotes.orEmpty(),
                    onDismiss = { showNotesSheet = false },
                    onSaveNotes = { notes ->
                        viewModel.saveResearchNotes(paperId, notes)
                    }
                )
            }

            // AI Research Co-Pilot Sheet
            if (showAiCoPilotSheet) {
                PaperAiCoPilotSheet(
                    paperId = paperId,
                    paperTitle = paperTitle,
                    paperAuthors = paperState?.authors.orEmpty(),
                    paperYear = paperState?.year.orEmpty(),
                    paperVenue = paperState?.venue.orEmpty(),
                    paperDoi = paperState?.doi.orEmpty(),
                    paperAbstract = paperState?.abstractText.orEmpty(),
                    initialPrompt = coPilotInitialPrompt,
                    onAppendToNotes = { insight ->
                        if (paperId.isNotBlank() && viewModel != null) {
                            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())
                            val noteEntry = "### 🤖 AI Co-Pilot Insight ($dateStr)\n$insight"
                            val currentNotes = paperState?.researchNotes?.trim().orEmpty()
                            val updatedNotes = if (currentNotes.isEmpty()) noteEntry else "$currentNotes\n\n---\n$noteEntry"
                            viewModel.saveResearchNotes(paperId, updatedNotes)
                        }
                    },
                    onDismiss = {
                        showAiCoPilotSheet = false
                        coPilotInitialPrompt = ""
                    }
                )
            }
        }
    }
}

/**
 * Renders the local PDF file using Android's native PdfRenderer.
 * Includes page-resuming, dynamic reading progress tracking, zoom/pan, and finish prompts.
 */
@Composable
private fun PdfRendererContent(
    filePath: String,
    nightMode: Boolean,
    paperId: String,
    initialPage: Int,
    readingStatus: String,
    jumpToPageTarget: Int?,
    onJumpCompleted: () -> Unit,
    viewModel: HomeViewModel?,
    onPageChanged: (page: Int, totalPages: Int) -> Unit
) {
    val file = remember(filePath) { File(filePath) }
    if (!file.exists() || !file.canRead()) {
        Text("PDF file not accessible on device.", color = MaterialTheme.colorScheme.error)
        return
    }

    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }

    // LRU memory cache for rendered page bitmaps (retains max 8 pages to prevent OOM)
    val bitmapCache = remember {
        object : LruCache<Int, Bitmap>(8) {
            override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
                if (evicted) oldValue?.recycle()
            }
        }
    }

    DisposableEffect(filePath) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRen = PdfRenderer(pfd)
            fileDescriptor = pfd
            renderer = pdfRen
            pageCount = pdfRen.pageCount
        } catch (e: Exception) {
            Log.e("PdfViewer", "Error opening PDF renderer for $filePath", e)
        }

        onDispose {
            try {
                renderer?.close()
                fileDescriptor?.close()
                bitmapCache.evictAll()
            } catch (e: Exception) {
                Log.w("PdfViewer", "Error closing renderer", e)
            }
        }
    }

    if (renderer == null || pageCount == 0) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Opening document pages...")
        }
        return
    }

    val listState = rememberLazyListState()
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Auto-resume to lastReadPage on first launch
    var hasRestoredInitialPage by remember { mutableStateOf(false) }
    LaunchedEffect(pageCount) {
        if (!hasRestoredInitialPage && pageCount > 0 && initialPage in 1..pageCount) {
            hasRestoredInitialPage = true
            listState.scrollToItem((initialPage - 1).coerceAtLeast(0))
        }
    }

    // React to external jump requests (e.g. from bookmarks menu)
    LaunchedEffect(jumpToPageTarget) {
        if (jumpToPageTarget != null && jumpToPageTarget in 1..pageCount) {
            listState.animateScrollToItem((jumpToPageTarget - 1).coerceAtLeast(0))
            onJumpCompleted()
        }
    }

    // Dynamic current visible page tracking
    val currentPage by remember {
        derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceIn(1, pageCount.coerceAtLeast(1)) }
    }

    LaunchedEffect(currentPage, pageCount) {
        if (pageCount > 0) {
            onPageChanged(currentPage, pageCount)
            if (paperId.isNotBlank() && viewModel != null) {
                viewModel.updateReadingProgress(paperId, currentPage, pageCount)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    zoomScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                    if (zoomScale > 1f) {
                        panOffsetX += pan.x
                        panOffsetY += pan.y
                    } else {
                        panOffsetX = 0f
                        panOffsetY = 0f
                    }
                }
            }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = zoomScale,
                    scaleY = zoomScale,
                    translationX = panOffsetX,
                    translationY = panOffsetY
                ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pageCount) { pageIndex ->
                PdfPageItem(
                    pageIndex = pageIndex,
                    renderer = renderer,
                    cache = bitmapCache,
                    nightMode = nightMode
                )
            }
        }

        // Reading Progress Indicator Pill
        val progress = if (pageCount > 0) currentPage.toFloat() / pageCount else 0f
        val percent = (progress * 100).toInt()

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shadowElevation = 6.dp,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Page $currentPage of $pageCount",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Prompt to mark as read when reaching the end of the manuscript
                    if (currentPage >= pageCount && pageCount > 1 &&
                        readingStatus != "READ" && readingStatus != "COMPLETED" &&
                        paperId.isNotBlank() && viewModel != null
                    ) {
                        Spacer(Modifier.width(4.dp))
                        FilledTonalButton(
                            onClick = { viewModel.setReadingStatus(paperId, "READ") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Mark Read", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(180.dp)
                        .height(3.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

/**
 * Individual rendered page bitmap with on-demand background rasterization.
 */
@Composable
private fun PdfPageItem(
    pageIndex: Int,
    renderer: PdfRenderer?,
    cache: LruCache<Int, Bitmap>,
    nightMode: Boolean
) {
    var pageBitmap by remember { mutableStateOf(cache.get(pageIndex)) }

    LaunchedEffect(pageIndex, renderer) {
        if (pageBitmap == null && renderer != null) {
            val bmp = withContext(Dispatchers.IO) {
                synchronized(renderer) {
                    try {
                        val page = renderer.openPage(pageIndex)
                        // Render at 2x page dimensions for crisp typography on mobile screens
                        val width = page.width * 2
                        val height = page.height * 2
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        cache.put(pageIndex, bitmap)
                        bitmap
                    } catch (e: Exception) {
                        Log.e("PdfPageItem", "Failed to render page $pageIndex", e)
                        null
                    }
                }
            }
            pageBitmap = bmp
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val currentBitmap = pageBitmap
        if (currentBitmap != null && !currentBitmap.isRecycled) {
            val displayBitmap = remember(currentBitmap, nightMode) {
                if (nightMode) applyInvertFilter(currentBitmap) else currentBitmap
            }
            Image(
                bitmap = displayBitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(480.dp)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Excerpt Highlighter Dialog.
 * Enables capturing quotes, tagging key categories, and appending directly to Research Notes.
 */
@Composable
fun ExcerptHighlighterDialog(
    pageNumber: Int,
    onDismiss: () -> Unit,
    onExplainWithAi: ((String) -> Unit)? = null,
    onSaveExcerpt: (tag: String, excerpt: String, commentary: String) -> Unit
) {
    var selectedTag by remember { mutableStateOf("Key Finding") }
    var excerptText by remember { mutableStateOf("") }
    var commentaryText by remember { mutableStateOf("") }
    val tagsRow1 = listOf("Key Finding", "Methodology", "Result")
    val tagsRow2 = listOf("Limitation", "Idea", "General")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Highlight Excerpt",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "Page $pageNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Category Tag",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tagsRow1.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tagsRow2.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = excerptText,
                    onValueChange = { excerptText = it },
                    label = { Text("Excerpt Quote") },
                    placeholder = { Text("Paste or type passage from this page...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 150.dp),
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = commentaryText,
                    onValueChange = { commentaryText = it },
                    label = { Text("Insight / Note (Optional)") },
                    placeholder = { Text("Add personal context or research connection...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 110.dp),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onExplainWithAi != null) {
                    OutlinedButton(
                        onClick = {
                            if (excerptText.isNotBlank()) {
                                onExplainWithAi(excerptText)
                            }
                        },
                        enabled = excerptText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Explain with AI", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Button(
                    onClick = {
                        if (excerptText.isNotBlank()) {
                            onSaveExcerpt(selectedTag, excerptText, commentaryText)
                        }
                    },
                    enabled = excerptText.isNotBlank()
                ) {
                    Text("Append to Notes")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * In-Reader Research Notes Bottom Sheet.
 * Allows viewing and editing paper notes without leaving the document reader.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InReaderResearchNotesSheet(
    paperTitle: String,
    initialNotes: String,
    onDismiss: () -> Unit,
    onSaveNotes: (String) -> Unit
) {
    var notesText by remember(initialNotes) { mutableStateOf(initialNotes) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Research Notes",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        paperTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = {
                        onSaveNotes(notesText)
                        onDismiss()
                    }
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
            }

            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                placeholder = { Text("Jot down summaries, ideas, methodologies, or saved excerpts...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f),
                shape = MaterialTheme.shapes.medium
            )
        }
    }
}

/**
 * Inverts the colors of a Bitmap for comfortable reading at night.
 */
private fun applyInvertFilter(source: Bitmap): Bitmap {
    val inverted = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(inverted)
    val paint = Paint()
    val matrix = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    paint.colorFilter = ColorMatrixColorFilter(matrix)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return inverted
}

private fun sharePdf(context: Context, localPath: String, title: String) {
    try {
        val file = File(localPath)
        val format = DocumentFormat.fromExtension(file.extension)
        val mime = format?.mimeType ?: "application/pdf"
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Research Manuscript"))
    } catch (e: Exception) {
        Log.e("PdfViewer", "Error sharing document", e)
    }
}

@Composable
private fun TextManuscriptContent(
    filePath: String,
    nightMode: Boolean
) {
    val content = remember(filePath) {
        try {
            File(filePath).readText().take(500_000)
        } catch (e: Exception) {
            "Unable to read manuscript text: ${e.message}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(
                if (nightMode) Color(0xFF1E1E1E) else Color.White,
                shape = MaterialTheme.shapes.small
            )
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = if (nightMode) Color(0xFFECEFF1) else Color(0xFF1A1A1A)
        )
    }
}

@Composable
private fun WordDocumentContent(
    filePath: String,
    paperTitle: String,
    format: DocumentFormat
) {
    val context = LocalContext.current
    val file = remember(filePath) { File(filePath) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Icon(
            Icons.Outlined.Description,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            paperTitle.ifBlank { file.name },
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${format.label} Manuscript • ${PdfStore.getFormattedSize(filePath)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, format.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open manuscript in..."))
                } catch (e: Exception) {
                    Log.e("PdfViewer", "Error launching external document viewer", e)
                }
            },
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("Open in Word / Office App")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { sharePdf(context, filePath, paperTitle) },
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Share Manuscript")
        }
    }
}
