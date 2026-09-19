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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.PdfStore
import com.example.data.security.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * In-App Research Paper PDF Viewer.
 *
 * Renders PDF pages with on-demand bitmap rasterization, LRU memory caching,
 * pan-and-zoom controls, night reading mode, and external sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    initialLocalPath: String = "",
    remoteUrl: String = "",
    paperTitle: String = "Research Paper",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeLocalPath by remember { mutableStateOf(initialLocalPath) }
    var isDownloading by remember { mutableStateOf(initialLocalPath.isBlank() && remoteUrl.isNotBlank()) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var nightMode by remember { mutableStateOf(false) }

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
                                PdfStore.getFormattedSize(activeLocalPath),
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
                    IconButton(onClick = { nightMode = !nightMode }) {
                        Icon(
                            if (nightMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = if (nightMode) "Normal Mode" else "Night Reading Mode"
                        )
                    }
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
                                nightMode = nightMode
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
        }
    }
}

/**
 * Renders the local PDF file using Android's native PdfRenderer.
 */
@Composable
private fun PdfRendererContent(
    filePath: String,
    nightMode: Boolean
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

        // Floating page pill
        val firstVisible = remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 6.dp
        ) {
            Text(
                text = "Page ${firstVisible.value} of $pageCount",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
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
