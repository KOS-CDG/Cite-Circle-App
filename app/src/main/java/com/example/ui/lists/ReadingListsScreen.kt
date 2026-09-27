package com.example.ui.lists

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.HomeViewModel
import com.example.ListState
import com.example.R
import com.example.navigateToPdf
import com.example.data.*
import com.example.data.security.DocumentFormat
import com.example.ui.components.EmptyState
import com.example.ui.components.ListRowSkeleton
import com.example.ui.components.RefreshableBox
import com.example.ui.share.ShareUtils
import com.example.ui.theme.DividerLight

sealed interface LibraryFilter {
    data object AllSaved : LibraryFilter
    data object Vault : LibraryFilter
    data class Custom(val collectionId: String) : LibraryFilter
}

private fun CollectionWithCount.toEntity(): CollectionEntity = CollectionEntity(
    id = id,
    name = name,
    description = description,
    colorHex = colorHex,
    iconName = iconName,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Academic Library & Repository Collections Hub.
 *
 * Provides research paper organization into custom folders/collections,
 * multi-faceted filtering & sorting, personal study notes, reading status tracking,
 * offline PDF vault access, and batch BibTeX citation exporting.
 */
@Composable
fun ReadingListsScreen(viewModel: HomeViewModel, navController: NavController) {
    val context = LocalContext.current
    val saved by viewModel.bookmarks.collectAsStateWithLifecycle()
    val collectionsState by viewModel.collections.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isSyncingLibrary by viewModel.isSyncingLibrary.collectAsStateWithLifecycle()
    val lastSyncedTimestamp by viewModel.lastSyncedTimestamp.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf<LibraryFilter>(LibraryFilter.AllSaved) }
    var searchQuery by remember { mutableStateOf("") }
    var filterCriteria by remember { mutableStateOf(RepositoryFilterCriteria()) }

    // Dialog states
    var showCreateDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var collectionToEdit by remember { mutableStateOf<CollectionEntity?>(null) }
    var collectionToDelete by remember { mutableStateOf<CollectionWithCount?>(null) }
    var paperToOrganize by remember { mutableStateOf<SavedPaper?>(null) }
    var paperForNotes by remember { mutableStateOf<SavedPaper?>(null) }

    // Dialog for creating a new collection
    if (showCreateDialog) {
        CreateEditCollectionDialog(
            initialCollection = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc, colorHex, iconName ->
                viewModel.createCollection(name, desc, colorHex, iconName)
            }
        )
    }

    // Dialog for editing an existing collection
    collectionToEdit?.let { col ->
        CreateEditCollectionDialog(
            initialCollection = col,
            onDismiss = { collectionToEdit = null },
            onConfirm = { name, desc, colorHex, iconName ->
                viewModel.updateCollection(
                    col.copy(
                        name = name,
                        description = desc,
                        colorHex = colorHex,
                        iconName = iconName
                    )
                )
                collectionToEdit = null
            }
        )
    }

    // Confirmation dialog for deleting a collection
    collectionToDelete?.let { col ->
        DeleteCollectionDialog(
            collectionName = col.name,
            onDismiss = { collectionToDelete = null },
            onConfirm = {
                viewModel.deleteCollection(col.id, col.name)
                if (selectedFilter is LibraryFilter.Custom && (selectedFilter as LibraryFilter.Custom).collectionId == col.id) {
                    selectedFilter = LibraryFilter.AllSaved
                }
                collectionToDelete = null
            }
        )
    }

    // Dialog to organize paper into collections
    paperToOrganize?.let { paper ->
        OrganizePaperDialog(
            paper = paper,
            viewModel = viewModel,
            onDismiss = { paperToOrganize = null }
        )
    }

    // Dialog to view/edit personal research notes
    paperForNotes?.let { paper ->
        ResearchNotesDialog(
            paper = paper,
            viewModel = viewModel,
            onDismiss = { paperForNotes = null }
        )
    }

    // Filter & Sort Dialog
    if (showFilterDialog) {
        RepositoryFilterDialog(
            currentCriteria = filterCriteria,
            onDismiss = { showFilterDialog = false },
            onApply = { newCriteria -> filterCriteria = newCriteria }
        )
    }

    // Observe papers for active custom collection if one is selected
    val activeCustomId = (selectedFilter as? LibraryFilter.Custom)?.collectionId
    val customCollectionPapers by produceState(initialValue = ListState<SavedPaper>(), key1 = activeCustomId) {
        if (activeCustomId != null) {
            viewModel.papersInCollection(activeCustomId).collect { value = it }
        } else {
            value = ListState()
        }
    }

    val activeCollection = remember(collectionsState.items, activeCustomId) {
        collectionsState.items.find { it.id == activeCustomId }
    }

    // Base paper list for the current selection
    val basePapers = when (selectedFilter) {
        is LibraryFilter.AllSaved -> saved.items
        is LibraryFilter.Vault -> saved.items.filter { it.pdfLocalPath.isNotBlank() || it.pdfUrl.isNotBlank() }
        is LibraryFilter.Custom -> customCollectionPapers.items
    }

    val isLoading = when (selectedFilter) {
        is LibraryFilter.Custom -> customCollectionPapers.isLoading
        else -> saved.isLoading
    }

    // Multi-faceted filtering & sorting
    val filteredItems = remember(basePapers, searchQuery, filterCriteria) {
        var list = basePapers

        // 1. Text search
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.authors.lowercase().contains(q) ||
                it.venue.lowercase().contains(q) ||
                it.content.lowercase().contains(q) ||
                it.researchNotes.lowercase().contains(q)
            }
        }

        // 2. Reading Status
        when (filterCriteria.readingStatus) {
            ReadingStatusFilter.TO_READ -> list = list.filter { it.readingStatus.equals("TO_READ", ignoreCase = true) }
            ReadingStatusFilter.READING -> list = list.filter { it.readingStatus.equals("READING", ignoreCase = true) }
            ReadingStatusFilter.COMPLETED -> list = list.filter {
                it.readingStatus.equals("COMPLETED", ignoreCase = true) || it.readingStatus.equals("READ", ignoreCase = true)
            }
            ReadingStatusFilter.ALL -> {}
        }

        // 3. Document Attachment
        when (filterCriteria.documentType) {
            DocumentTypeFilter.PDF_ONLY -> list = list.filter { it.pdfLocalPath.isNotBlank() || it.pdfUrl.isNotBlank() }
            DocumentTypeFilter.OPEN_ACCESS -> list = list.filter { it.openAccess }
            DocumentTypeFilter.ALL -> {}
        }

        // 4. Sort Order
        when (filterCriteria.sortOrder) {
            RepositorySortOrder.NEWEST -> list.sortedByDescending { it.publishedAt }
            RepositorySortOrder.OLDEST -> list.sortedBy { it.publishedAt }
            RepositorySortOrder.TITLE_AZ -> list.sortedBy { it.title.lowercase() }
            RepositorySortOrder.AUTHOR_AZ -> list.sortedBy { it.authors.lowercase() }
        }
    }

    val pdfCount = remember(saved.items) {
        saved.items.count { it.pdfLocalPath.isNotBlank() || it.pdfUrl.isNotBlank() }
    }

    fun handleBatchExport() {
        if (filteredItems.isEmpty()) {
            viewModel.report("No papers available to export")
            return
        }
        val bibText = CitationFormatter.exportBatch(filteredItems, ExportFormat.BIBTEX)
        val title = when (selectedFilter) {
            is LibraryFilter.Custom -> "${activeCollection?.name ?: "Collection"} - Citations.bib"
            is LibraryFilter.Vault -> "Paper Vault - Citations.bib"
            else -> "Saved Library - Citations.bib"
        }
        ShareUtils.shareText(context, bibText, title)
        viewModel.report("Exported ${filteredItems.size} papers as BibTeX")
    }

    RefreshableBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.saved_title),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isLoading) {
                                Text(
                                    pluralStringResource(
                                        R.plurals.entry_count,
                                        filteredItems.size,
                                        filteredItems.size
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            // Cloud Sync Button
                            IconButton(
                                onClick = { viewModel.syncFullLibraryNow(showFeedback = true) },
                                enabled = !isSyncingLibrary
                            ) {
                                if (isSyncingLibrary) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.Sync,
                                        contentDescription = "Sync Library with Cloud",
                                        tint = if (lastSyncedTimestamp > 0L) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            // Batch BibTeX Export Button
                            IconButton(
                                onClick = { handleBatchExport() },
                                enabled = filteredItems.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Outlined.IosShare,
                                    contentDescription = "Export BibTeX",
                                    tint = if (filteredItems.isNotEmpty()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Collections & Folders Horizontal Ribbon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All Saved Chip
                        FilterChip(
                            selected = selectedFilter is LibraryFilter.AllSaved,
                            onClick = { selectedFilter = LibraryFilter.AllSaved },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("All Saved (${saved.items.size})") }
                        )

                        // Paper Vault Chip
                        FilterChip(
                            selected = selectedFilter is LibraryFilter.Vault,
                            onClick = { selectedFilter = LibraryFilter.Vault },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Paper Vault ($pdfCount)") }
                        )

                        // Custom Collection Chips
                        collectionsState.items.forEach { col ->
                            val isSelected = (selectedFilter as? LibraryFilter.Custom)?.collectionId == col.id
                            val colColor = parseCollectionColor(col.colorHex)
                            val icon = getCollectionIcon(col.iconName)

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = LibraryFilter.Custom(col.id) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else colColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text("${col.name} (${col.paperCount})") }
                            )
                        }

                        // "+ New Collection" Action Chip
                        ElevatedAssistChip(
                            onClick = { showCreateDialog = true },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            label = {
                                Text(
                                    "+ New Collection",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
                }
            }

            // Collection Header Banner (shown when a custom collection is selected)
            if (activeCollection != null) {
                CollectionHeaderCard(
                    collection = activeCollection,
                    onEdit = { collectionToEdit = activeCollection.toEntity() },
                    onDelete = { collectionToDelete = activeCollection },
                    onExportBibtex = { handleBatchExport() }
                )
            }

            // Search Bar & Filter Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            if (activeCollection != null) "Search in ${activeCollection.name}..."
                            else "Search title, author, venue, or notes...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small
                )

                Spacer(Modifier.width(8.dp))

                // Multi-faceted Filter & Sort Button with Active Count Badge
                BadgedBox(
                    badge = {
                        if (filterCriteria.activeFilterCount > 0) {
                            Badge {
                                Text("${filterCriteria.activeFilterCount}")
                            }
                        }
                    }
                ) {
                    FilledTonalIconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.size(48.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Filter & Sort",
                            tint = if (filterCriteria.activeFilterCount > 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when {
                isLoading -> Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { ListRowSkeleton() }
                }

                filteredItems.isEmpty() -> EmptyState(
                    title = when {
                        searchQuery.isNotBlank() || filterCriteria.activeFilterCount > 0 -> "No matching papers found"
                        activeCollection != null -> "No Papers in '${activeCollection.name}'"
                        selectedFilter is LibraryFilter.Vault -> "No Papers in PDF Vault"
                        else -> stringResource(R.string.saved_empty_title)
                    },
                    message = when {
                        searchQuery.isNotBlank() || filterCriteria.activeFilterCount > 0 ->
                            "No papers match your search keywords or filter criteria. Try resetting filters."
                        activeCollection != null ->
                            "Organize research papers into '${activeCollection.name}' using the folder icon on any paper."
                        selectedFilter is LibraryFilter.Vault ->
                            "Save research papers with PDFs attached or open-access links to view them offline in your vault."
                        else -> stringResource(R.string.saved_empty_message)
                    },
                    icon = when {
                        activeCollection != null -> getCollectionIcon(activeCollection.iconName)
                        selectedFilter is LibraryFilter.Vault -> Icons.Outlined.PictureAsPdf
                        else -> Icons.Outlined.BookmarkBorder
                    },
                    actionLabel = if (searchQuery.isNotBlank() || filterCriteria.activeFilterCount > 0) "Reset Filters" else "Explore Papers",
                    onAction = {
                        if (searchQuery.isNotBlank() || filterCriteria.activeFilterCount > 0) {
                            searchQuery = ""
                            filterCriteria = RepositoryFilterCriteria()
                        } else {
                            navController.navigate("fields")
                        }
                    }
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredItems, key = { it.id }) { paper ->
                        SavedEntryCard(
                            paper = paper,
                            viewModel = viewModel,
                            onOpen = { navController.navigate("post/${paper.id}") },
                            onOrganize = { paperToOrganize = paper },
                            onOpenNotes = { paperForNotes = paper },
                            onReadPdf = {
                                navController.navigateToPdf(
                                    paperId = paper.id,
                                    path = paper.pdfLocalPath,
                                    url = paper.pdfUrl,
                                    title = paper.title.ifBlank { "Research Paper" }
                                )
                            },
                            onDownloadPdf = {
                                if (paper.pdfUrl.isNotBlank()) {
                                    viewModel.cacheRemotePdf(paper.id, paper.pdfUrl)
                                }
                            },
                            onRemove = { viewModel.toggleBookmark(paper.id, paper.isBookmarked) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top header banner displayed when inspecting a specific research collection.
 */
@Composable
private fun CollectionHeaderCard(
    collection: CollectionWithCount,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExportBibtex: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val colColor = parseCollectionColor(collection.colorHex)
    val icon = getCollectionIcon(collection.iconName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, colColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (collection.description.isNotBlank()) {
                    Text(
                        collection.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "${collection.paperCount} ${if (collection.paperCount == 1) "paper" else "papers"} in collection",
                    style = MaterialTheme.typography.labelSmall,
                    color = colColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Collection Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Export Collection (.bib)") },
                        onClick = {
                            showMenu = false
                            onExportBibtex()
                        },
                        leadingIcon = { Icon(Icons.Outlined.IosShare, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Collection") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Collection", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedEntryCard(
    paper: SavedPaper,
    viewModel: HomeViewModel,
    onOpen: () -> Unit,
    onOrganize: () -> Unit,
    onOpenNotes: () -> Unit,
    onReadPdf: () -> Unit,
    onDownloadPdf: () -> Unit,
    onRemove: () -> Unit
) {
    val hasPdf = paper.pdfLocalPath.isNotBlank() || paper.pdfUrl.isNotBlank()
    val collectionsForPaper by viewModel.getCollectionsForPaper(paper.id).collectAsStateWithLifecycle(emptyList())

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(0.5.dp, DividerLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        paper.title.ifBlank { paper.content },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(
                            R.string.saved_byline,
                            paper.authorName,
                            formatTimeAgo(paper.publishedAt)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Research Notes button
                    IconButton(onClick = onOpenNotes) {
                        Icon(
                            Icons.Outlined.EditNote,
                            contentDescription = "Research Notes",
                            modifier = Modifier.size(22.dp),
                            tint = if (paper.researchNotes.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Organize in collections button
                    IconButton(onClick = onOrganize) {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = "Organize in collections",
                            modifier = Modifier.size(20.dp),
                            tint = if (collectionsForPaper.isNotEmpty()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Remove bookmark button
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Filled.Bookmark,
                            contentDescription = stringResource(R.string.cd_remove_from_saved),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Reading Status Pill & Collection Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Reading Status Badge
                ReadingStatusBadge(
                    currentStatus = paper.readingStatus,
                    onStatusSelected = { newStatus ->
                        viewModel.setReadingStatus(paper.id, newStatus)
                    }
                )

                // Reading Progress Badge if reader has made progress
                if (paper.lastReadPage > 1 || paper.totalPageCount > 1) {
                    val pageText = if (paper.totalPageCount > 0) {
                        val pct = ((paper.lastReadPage.toFloat() / paper.totalPageCount) * 100).toInt()
                        "Page ${paper.lastReadPage}/${paper.totalPageCount} ($pct%)"
                    } else {
                        "Page ${paper.lastReadPage}"
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = pageText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Collection Badges
                collectionsForPaper.forEach { col ->
                    val colColor = parseCollectionColor(col.colorHex)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colColor.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, colColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colColor)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = col.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Research Notes preview snippet (if notes exist)
            if (paper.researchNotes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onOpenNotes)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.NoteAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        paper.researchNotes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (CitationFormatter.isStyleable(paper) || paper.citationOverride.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    CitationFormatter.format(paper, CitationStyle.DEFAULT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                )
            }

            // PDF Vault actions
            if (hasPdf) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val format = if (paper.pdfLocalPath.isNotBlank()) PdfStore.getDocumentFormat(paper.pdfLocalPath) else DocumentFormat.PDF
                    val isPdf = format == DocumentFormat.PDF

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isPdf) Icons.Outlined.PictureAsPdf else Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (paper.pdfLocalPath.isNotBlank()) {
                                "${format.label} (${PdfStore.getFormattedSize(paper.pdfLocalPath)})"
                            } else {
                                "Open Access Cloud PDF"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (paper.pdfLocalPath.isBlank() && paper.pdfUrl.isNotBlank()) {
                            OutlinedButton(
                                onClick = onDownloadPdf,
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = MaterialTheme.shapes.extraLarge
                            ) {
                                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cache Offline", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Button(
                            onClick = onReadPdf,
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Text(
                                if (isPdf) "Read PDF" else "Open ${format.label}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
