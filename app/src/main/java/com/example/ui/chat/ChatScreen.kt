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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Toolbar for model selection
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = viewModel.currentModel == "gemini-3.5-flash",
                onClick = { viewModel.currentModel = "gemini-3.5-flash" },
                label = { Text("Fast (3.5-flash)") }
            )
            FilterChip(
                selected = viewModel.currentModel == "gemini-3.1-pro-preview",
                onClick = { viewModel.currentModel = "gemini-3.1-pro-preview" },
                label = { Text("Pro (3.1-pro)") }
            )
            FilterChip(
                selected = viewModel.currentModel == "gemini-3.1-flash-lite-preview",
                onClick = { viewModel.currentModel = "gemini-3.1-flash-lite-preview" },
                label = { Text("Low Latency (Lite)") }
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = viewModel.useSearchGrounding,
                onCheckedChange = { viewModel.useSearchGrounding = it }
            )
            Text("Enable Search Grounding", style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f).padding(8.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Image Preview
        selectedImageUri?.let { uri ->
            @Suppress("DEPRECATION")
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            
            Box(modifier = Modifier.padding(8.dp)) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Selected image",
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp))
                )
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove image",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { imagePicker.launch("image/*") }) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Photo")
            }
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 3
            )
            @Suppress("DEPRECATION")
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() || selectedImageUri != null) {
                        @Suppress("DEPRECATION")
                        var bitmap: Bitmap? = null
                        selectedImageUri?.let { uri ->
                            bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                            } else {
                                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                        }
                        viewModel.sendMessage(inputText, bitmap)
                        inputText = ""
                        selectedImageUri = null
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    // The sender's own messages carry the brand colour, as in every messaging surface.
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .background(bgColor)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            if (message.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                message.imageUrl?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Uploaded image",
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).padding(bottom = 4.dp)
                    )
                }
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = if (message.isError) MaterialTheme.colorScheme.error else textColor
                    )
                }
            }
        }
    }
}
