package com.example.ui.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.HomeViewModel
import com.example.R
import com.example.data.AuthorIdentity
import com.example.data.CitationFormatter
import com.example.data.CitationStyle
import com.example.data.ImageStore
import com.example.data.PdfStore
import com.example.data.SavedPaper
import com.example.network.AcademicPaperResolver
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * The post composer.
 *
 * Collects the commentary plus the structured paper metadata that [CitationFormatter]
 * needs, and previews the rendered citation live so mistakes are visible before publishing.
 * This is the screen that finally calls [HomeViewModel.savePaper], which existed unused
 * until now.
 */
@Composable
fun ComposePostScreen(
    viewModel: HomeViewModel,
    onDone: () -> Unit,
    existing: SavedPaper? = null
) {
    val context = LocalContext.current
    val identity = remember(context) { AuthorIdentity.current(context) }
    val scope = rememberCoroutineScope()
    val isEdit = existing != null

    var commentary by rememberSaveable { mutableStateOf(existing?.content.orEmpty()) }
    var imagePath by rememberSaveable { mutableStateOf(existing?.imageUri.orEmpty()) }
    var pdfLocalPath by rememberSaveable { mutableStateOf(existing?.pdfLocalPath.orEmpty()) }
    var pdfUrl by rememberSaveable { mutableStateOf(existing?.pdfUrl.orEmpty()) }
    var abstractText by rememberSaveable { mutableStateOf(existing?.abstractText.orEmpty()) }
    var openAccess by rememberSaveable { mutableStateOf(existing?.openAccess ?: false) }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var authors by rememberSaveable { mutableStateOf(existing?.authors.orEmpty()) }
    var year by rememberSaveable { mutableStateOf(existing?.year.orEmpty()) }
    var venue by rememberSaveable { mutableStateOf(existing?.venue.orEmpty()) }
    var doi by rememberSaveable { mutableStateOf(existing?.doi.orEmpty()) }
    var url by rememberSaveable { mutableStateOf(existing?.url.orEmpty()) }
    var affiliation by rememberSaveable {
        mutableStateOf(existing?.affiliation ?: identity.affiliation)
    }
    var previewStyle by rememberSaveable { mutableStateOf(CitationStyle.DEFAULT) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var isResolvingDoi by remember { mutableStateOf(false) }
    var doiStatusMessage by remember { mutableStateOf("") }

    // The image and pdf the post already had. Only *newly* picked files should be cleaned up on
    // discard — deleting these would strip attachments off the saved post.
    val originalImage = remember { existing?.imageUri.orEmpty() }
    val originalPdf = remember { existing?.pdfLocalPath.orEmpty() }

    // Read here because the picker callback below is a plain lambda, not a composable.
    val imageUnreadable = stringResource(R.string.image_unreadable)

    val titleError = title.isBlank()
    // A four-digit year is the only thing worth rejecting outright; everything else is
    // legitimately optional on a preprint.
    val yearError = year.isNotBlank() && !Regex("^\\d{4}$").matches(year.trim())
    val canPublish = !titleError && !yearError

    val draft = SavedPaper(
        id = existing?.id.orEmpty(),
        authorInitials = existing?.authorInitials ?: identity.initials,
        authorName = existing?.authorName ?: identity.name,
        affiliation = affiliation.trim(),
        content = commentary.trim(),
        title = title.trim(),
        authors = authors.trim(),
        year = year.trim(),
        venue = venue.trim(),
        doi = doi.trim(),
        url = url.trim(),
        imageUri = imagePath,
        pdfUrl = pdfUrl.trim(),
        pdfLocalPath = pdfLocalPath.trim(),
        abstractText = abstractText.trim(),
        openAccess = openAccess || pdfUrl.isNotBlank()
    )

    // The picker's URI grant is transient, so ImageStore copies the bytes into app storage
    // immediately and the post stores that path instead.
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val previous = imagePath
                val stored = ImageStore.persist(context, uri)
                if (stored == null) {
                    // Used to fail silently, leaving the button looking simply unresponsive.
                    viewModel.report(imageUnreadable)
                } else {
                    imagePath = stored
                    // Replacing an attachment should not orphan the one it replaced, but the
                    // post's existing figure stays until the edit is actually saved.
                    if (previous.isNotBlank() && previous != originalImage) {
                        ImageStore.delete(context, previous)
                    }
                }
            }
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val previous = pdfLocalPath
                val stored = PdfStore.persist(context, uri)
                if (stored == null) {
                    viewModel.report("Unable to process selected PDF file")
                } else {
                    pdfLocalPath = stored
                    if (previous.isNotBlank() && previous != originalPdf) {
                        PdfStore.delete(context, previous)
                    }
                }
            }
        }
    }

    /** Discarding must not leave a newly copied image or PDF behind, nor delete the saved ones. */
    fun discard() {
        val pendingImage = imagePath
        imagePath = ""
        if (pendingImage.isNotBlank() && pendingImage != originalImage) {
            scope.launch { ImageStore.delete(context, pendingImage) }
        }

        val pendingPdf = pdfLocalPath
        pdfLocalPath = ""
        if (pendingPdf.isNotBlank() && pendingPdf != originalPdf) {
            scope.launch { PdfStore.delete(context, pendingPdf) }
        }
        onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(
                    if (isEdit) R.string.compose_title_edit else R.string.compose_title_new
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { discard() }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_discard_draft),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionLabel(stringResource(R.string.section_commentary))
        EditorialTextField(
            value = commentary,
            onValueChange = { commentary = it },
            placeholder = stringResource(R.string.compose_commentary_placeholder),
            minLines = 4
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.section_attachment))
        if (imagePath.isBlank()) {
            OutlinedButton(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    Icons.Outlined.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.action_add_figure),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = stringResource(R.string.cd_attached_figure),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(MaterialTheme.shapes.small)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.small
                        )
                )
                IconButton(
                    onClick = {
                        val pending = imagePath
                        imagePath = ""
                        scope.launch { ImageStore.delete(context, pending) }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_remove_figure),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        if (pdfLocalPath.isBlank()) {
            OutlinedButton(
                onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.extraLarge,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Icon(
                    Icons.Outlined.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Attach Research Paper PDF",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Research Paper PDF Attached",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                PdfStore.getFormattedSize(pdfLocalPath),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            val pending = pdfLocalPath
                            pdfLocalPath = ""
                            scope.launch { PdfStore.delete(context, pending) }
                        }
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove PDF",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(28.dp))

        SectionLabel(stringResource(R.string.section_paper_details))
        EditorialTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = stringResource(R.string.field_title),
            isError = showErrors && titleError
        )
        if (showErrors && titleError) {
            FieldError(stringResource(R.string.error_title_required))
        }

        Spacer(Modifier.height(12.dp))
        EditorialTextField(
            value = authors,
            onValueChange = { authors = it },
            placeholder = stringResource(R.string.field_authors)
        )
        FieldHint(stringResource(R.string.hint_authors))

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                EditorialTextField(
                    value = year,
                    onValueChange = { year = it },
                    placeholder = stringResource(R.string.field_year),
                    keyboardType = KeyboardType.Number,
                    isError = showErrors && yearError
                )
            }
            Column(modifier = Modifier.weight(2f)) {
                EditorialTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    placeholder = stringResource(R.string.field_venue)
                )
            }
        }
        if (showErrors && yearError) FieldError(stringResource(R.string.error_year))

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                EditorialTextField(
                    value = doi,
                    onValueChange = {
                        doi = it
                        doiStatusMessage = ""
                    },
                    placeholder = stringResource(R.string.field_doi),
                    keyboardType = KeyboardType.Uri
                )
            }
            Button(
                onClick = {
                    if (doi.isNotBlank()) {
                        isResolvingDoi = true
                        doiStatusMessage = ""
                        scope.launch {
                            val resolved = AcademicPaperResolver.resolveDoi(doi)
                            isResolvingDoi = false
                            if (resolved != null) {
                                if (title.isBlank() || title == existing?.title) title = resolved.title
                                if (authors.isBlank() || authors == existing?.authors) authors = resolved.authors
                                if (year.isBlank() || year == existing?.year) year = resolved.year
                                if (venue.isBlank() || venue == existing?.venue) venue = resolved.venue
                                if (url.isBlank() || url == existing?.url) url = resolved.url
                                if (abstractText.isBlank() || abstractText == existing?.abstractText) abstractText = resolved.abstractText
                                if (pdfUrl.isBlank() || pdfUrl == existing?.pdfUrl) pdfUrl = resolved.pdfUrl
                                if (resolved.isOpenAccess) openAccess = true
                                doiStatusMessage = if (resolved.pdfUrl.isNotBlank()) "Metadata & Open-Access PDF imported!" else "Metadata imported from catalog!"
                            } else {
                                doiStatusMessage = "Could not resolve DOI. Enter details manually."
                            }
                        }
                    }
                },
                enabled = doi.isNotBlank() && !isResolvingDoi,
                shape = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (isResolvingDoi) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Auto-fill", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (doiStatusMessage.isNotBlank()) {
            Text(
                doiStatusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (doiStatusMessage.startsWith("Metadata")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))
        EditorialTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = stringResource(R.string.field_url),
            keyboardType = KeyboardType.Uri
        )

        Spacer(Modifier.height(12.dp))
        EditorialTextField(
            value = pdfUrl,
            onValueChange = { pdfUrl = it },
            placeholder = "Open-Access PDF Link (e.g. arXiv / preprint link)",
            keyboardType = KeyboardType.Uri
        )

        Spacer(Modifier.height(12.dp))
        EditorialTextField(
            value = abstractText,
            onValueChange = { abstractText = it },
            placeholder = "Abstract (auto-populated from DOI or enter manually)",
            minLines = 3
        )

        Spacer(Modifier.height(12.dp))
        EditorialTextField(
            value = affiliation,
            onValueChange = { affiliation = it },
            placeholder = stringResource(R.string.field_affiliation)
        )

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(28.dp))

        SectionLabel(stringResource(R.string.section_citation_preview))
        CitationPreview(
            draft = draft,
            style = previewStyle,
            onStyleChange = { previewStyle = it }
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                if (!canPublish) {
                    showErrors = true
                } else {
                    // Editing keeps the original id and publish time, so the entry updates in
                    // place rather than appearing twice at the top of the feed.
                    viewModel.savePaper(
                        if (existing != null) {
                            draft.copy(
                                id = existing.id,
                                publishedAt = existing.publishedAt,
                                isEndorsed = existing.isEndorsed,
                                endorsementCount = existing.endorsementCount,
                                commentCount = existing.commentCount,
                                repostCount = existing.repostCount,
                                isBookmarked = existing.isBookmarked,
                                quotedId = existing.quotedId,
                                quotedAuthorName = existing.quotedAuthorName,
                                quotedTitle = existing.quotedTitle,
                                quotedContent = existing.quotedContent,
                                imageUri = imagePath,
                                pdfLocalPath = pdfLocalPath,
                                pdfUrl = pdfUrl,
                                abstractText = abstractText,
                                openAccess = openAccess || pdfUrl.isNotBlank()
                            )
                        } else {
                            draft.copy(
                                id = UUID.randomUUID().toString(),
                                publishedAt = System.currentTimeMillis()
                            )
                        }
                    )
                    // A replaced figure or PDF is only safe to delete once the change is committed.
                    if (existing != null && originalImage.isNotBlank() &&
                        originalImage != imagePath
                    ) {
                        viewModel.forgetPaper(originalImage)
                    }
                    if (existing != null && originalPdf.isNotBlank() &&
                        originalPdf != pdfLocalPath
                    ) {
                        viewModel.forgetPaper("", originalPdf)
                    }
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                stringResource(if (isEdit) R.string.action_save_changes else R.string.action_post),
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun FieldHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun FieldError(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorialTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        minLines = minLines,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            errorContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

/** The dark citation slab, rendering the draft live as the form is filled in. */
@Composable
private fun CitationPreview(
    draft: SavedPaper,
    style: CitationStyle,
    onStyleChange: (CitationStyle) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
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
                    stringResource(R.string.preview_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CitationStyle.entries.forEach { option ->
                        val selected = option == style
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold
                                else FontWeight.Medium
                            ),
                            color = if (selected) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                                    } else {
                                        androidx.compose.ui.graphics.Color.Transparent
                                    }
                                )
                                .clickable { onStyleChange(option) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (draft.title.isBlank()) {
                Text(
                    stringResource(R.string.preview_empty),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                )
            } else {
                Text(
                    CitationFormatter.format(draft, style),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
