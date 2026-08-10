package com.example.ui.chat

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.EmptyState
import com.example.ui.components.ScreenHeader

private data class ChatModelOption(val id: String, val label: String)

private val chatModels = listOf(
    ChatModelOption("gemini-3.5-flash", "FAST"),
    ChatModelOption("gemini-3.1-pro-preview", "PRO"),
    ChatModelOption("gemini-3.1-flash-lite-preview", "LITE"),
)

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel(),
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedImageUri = uri },
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(title = "RESEARCH ASSISTANT", onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CiteCircleDefaults.ScreenPadding,
                    vertical = 12.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            chatModels.forEach { option ->
                ModelChip(
                    label = option.label,
                    selected = viewModel.currentModel == option.id,
                    onClick = { viewModel.currentModel = option.id },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            GroundingToggle(
                enabled = viewModel.useSearchGrounding,
                onToggle = { viewModel.useSearchGrounding = !viewModel.useSearchGrounding },
            )
        }

        HorizontalDivider(color = CiteCircleDefaults.hairlineColor())

        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Ask Anything",
                    message = "Summarise a paper, draft a review, or work through an argument.",
                    icon = Icons.Outlined.AutoAwesome,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(CiteCircleDefaults.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
            }
        }

        selectedImageUri?.let { uri ->
            val bitmap = remember(uri) { context.decodeBitmap(uri) }
            if (bitmap != null) {
                Box(
                    modifier = Modifier.padding(
                        start = CiteCircleDefaults.ScreenPadding,
                        bottom = 8.dp,
                    ),
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CiteCircleDefaults.CardShape),
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Remove attached image",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = CiteCircleDefaults.hairlineColor())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { imagePicker.launch("image/*") }) {
                Icon(
                    Icons.Filled.AddPhotoAlternate,
                    contentDescription = "Attach an image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Type a message…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                maxLines = 3,
                shape = CiteCircleDefaults.CardShape,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = CiteCircleDefaults.hairlineColor(alpha = 0.2f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() || selectedImageUri != null) {
                        val bitmap = selectedImageUri?.let { context.decodeBitmap(it) }
                        viewModel.sendMessage(inputText, bitmap)
                        inputText = ""
                        selectedImageUri = null
                    }
                },
                enabled = inputText.isNotBlank() || selectedImageUri != null,
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (inputText.isNotBlank() || selectedImageUri != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                )
            }
        }
    }
}

@Composable
private fun ModelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
        ),
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            )
            .border(
                CiteCircleDefaults.cardBorder(alpha = if (selected) 0f else 0.2f),
                RoundedCornerShape(2.dp),
            )
            .clickable(onClickLabel = "Use the $label model", onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun GroundingToggle(enabled: Boolean, onToggle: () -> Unit) {
    Text(
        "SEARCH",
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold,
        ),
        color = if (enabled) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(
                CiteCircleDefaults.cardBorder(alpha = if (enabled) 0.4f else 0.2f),
                RoundedCornerShape(2.dp),
            )
            .clickable(
                onClickLabel = if (enabled) {
                    "Disable search grounding"
                } else {
                    "Enable search grounding"
                },
                onClick = onToggle,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(CiteCircleDefaults.CardShape)
                .background(
                    if (message.isUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                )
                .border(
                    CiteCircleDefaults.cardBorder(alpha = if (message.isUser) 0f else 0.1f),
                    CiteCircleDefaults.CardShape,
                )
                .padding(16.dp),
        ) {
            if (message.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 2.dp,
                )
            } else {
                message.imageUrl?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Attached image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp)),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = when {
                            message.isError -> MaterialTheme.colorScheme.error
                            message.isUser -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

/**
 * Decodes a picked image, tolerating the pre-P and post-P code paths.
 *
 * On P+ the decoder is forced to a software allocation: the default is a hardware bitmap, and
 * `ChatViewModel` compresses the result to base64, which throws for hardware bitmaps.
 */
private fun android.content.Context.decodeBitmap(uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(contentResolver, uri)
    }
}.getOrNull()
