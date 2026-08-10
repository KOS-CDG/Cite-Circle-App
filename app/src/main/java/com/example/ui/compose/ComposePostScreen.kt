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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.HomeViewModel
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
fun ComposePostScreen(viewModel: HomeViewModel, onDone: () -> Unit) {
    val identity = remember { AuthorIdentity.current() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var commentary by rememberSaveable { mutableStateOf("") }
    var imagePath by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var authors by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var venue by rememberSaveable { mutableStateOf("") }
    var doi by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var affiliation by rememberSaveable { mutableStateOf(identity.affiliation) }
    var previewStyle by rememberSaveable { mutableStateOf(CitationStyle.DEFAULT) }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val titleError = title.isBlank()
    // A four-digit year is the only thing worth rejecting outright; everything else is
    // legitimately optional on a preprint.
    val yearError = year.isNotBlank() && !Regex("^\\d{4}$").matches(year.trim())
    val canPublish = !titleError && !yearError

    val draft = SavedPaper(
        id = "",
        authorInitials = identity.initials,
        authorName = identity.name,
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
                if (stored != null) {
                    imagePath = stored
                    // Replacing an attachment should not orphan the one it replaced.
                    if (previous.isNotBlank()) ImageStore.delete(context, previous)
                }
            }
        }
    }

    /** Discarding the draft must not leave its copied image behind. */
    fun discard() {
        val pending = imagePath
        imagePath = ""
        if (pending.isNotBlank()) scope.launch { ImageStore.delete(context, pending) }
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
                "New entry",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { discard() }) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Discard draft",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        SectionLabel("Commentary")
        EditorialTextField(
            value = commentary,
            onValueChange = { commentary = it },
            placeholder = "What should your circle know about this paper?",
            minLines = 4
        )

        Spacer(Modifier.height(20.dp))
        SectionLabel("Attachment")
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
                    "Add a figure",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = "Attached figure",
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
                        contentDescription = "Remove figure",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(28.dp))

        SectionLabel("Paper details")
        EditorialTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = "Title (required)",
            isError = showErrors && titleError
        )
        if (showErrors && titleError) {
            FieldError("A title is required — citations cannot be generated without one.")
        }

        Spacer(Modifier.height(12.dp))
        EditorialTextField(
            value = authors,
            onValueChange = { authors = it },
            placeholder = "Doe, Jane; Smith, John"
        )
        FieldHint("Separate authors with a semicolon, each written “Family, Given”.")

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                EditorialTextField(
                    value = year,
                    onValueChange = { year = it },
                    placeholder = "Year",
                    keyboardType = KeyboardType.Number,
                    isError = showErrors && yearError
                )
            }
            Column(modifier = Modifier.weight(2f)) {
                EditorialTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    placeholder = "Journal or venue"
                )
            }
        }
        if (showErrors && yearError) FieldError("Enter a four-digit year, or leave it blank.")

        Spacer(Modifier.height(12.dp))
        EditorialTextField(
            value = doi,
            onValueChange = { doi = it },
            placeholder = "DOI (10.1000/example)",
            keyboardType = KeyboardType.Uri
        )

        Spacer(Modifier.height(12.dp))
        EditorialTextField(
            value = url,
            onValueChange = { url = it },
            placeholder = "URL",
            keyboardType = KeyboardType.Uri
        )

        Spacer(Modifier.height(12.dp))
        EditorialTextField(
            value = affiliation,
            onValueChange = { affiliation = it },
            placeholder = "Affiliation"
        )

        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(28.dp))

        SectionLabel("Citation preview")
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
                    viewModel.savePaper(
                        draft.copy(
                            id = UUID.randomUUID().toString(),
                            publishedAt = System.currentTimeMillis()
                        )
                    )
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
                "Post",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
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
                    "Preview",
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
                    "Add a title to see the citation build itself.",
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
