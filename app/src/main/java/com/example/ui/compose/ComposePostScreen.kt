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
import com.example.data.SavedPaper
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

    // The image the post already had. Only a *newly* picked file should be cleaned up on
    // discard — deleting this one would strip the figure off the saved post.
    val originalImage = remember { existing?.imageUri.orEmpty() }

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
        imageUri = imagePath
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

    /** Discarding must not leave a newly copied image behind, nor delete the saved one. */
    fun discard() {
        val pending = imagePath
        imagePath = ""
        if (pending.isNotBlank() && pending != originalImage) {
            scope.launch { ImageStore.delete(context, pending) }
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
        EditorialTextField(
            value = doi,
            onValueChange = { doi = it },
            placeholder = stringResource(R.string.field_doi),
            keyboardType = KeyboardType.Uri
        )

        Spacer(Modifier.height(12.dp))
        EditorialTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = stringResource(R.string.field_url),
            keyboardType = KeyboardType.Uri
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
                                quotedContent = existing.quotedContent
                            )
                        } else {
                            draft.copy(
                                id = UUID.randomUUID().toString(),
                                publishedAt = System.currentTimeMillis()
                            )
                        }
                    )
                    // A replaced figure is only safe to delete once the change is committed.
                    if (existing != null && originalImage.isNotBlank() &&
                        originalImage != imagePath
                    ) {
                        viewModel.forgetPaper(originalImage)
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
