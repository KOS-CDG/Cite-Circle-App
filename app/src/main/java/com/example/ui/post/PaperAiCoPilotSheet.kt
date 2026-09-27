package com.example.ui.post

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerLight
import com.example.ui.theme.PageNeutral
import com.example.ui.theme.SurfaceInset
import com.example.ui.theme.SurfaceWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AiCoPilotMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Slide-up AI Research Co-Pilot bottom sheet embedded inside the In-App PDF Reader.
 *
 * Grounded specifically in the current manuscript's metadata, abstract, and notes.
 * Offers 1-tap academic prompts (Methodology, Findings, Limitations, BibTeX synthesis)
 * and enables direct 1-tap appending of AI insights into the paper's personal research notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperAiCoPilotSheet(
    paperId: String,
    paperTitle: String,
    paperAuthors: String = "",
    paperYear: String = "",
    paperVenue: String = "",
    paperDoi: String = "",
    paperAbstract: String = "",
    initialPrompt: String = "",
    onAppendToNotes: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<AiCoPilotMessage>>(emptyList()) }
    var isThinking by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var appendedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var copiedId by remember { mutableStateOf<String?>(null) }

    // Quick action prompt chips
    val quickActions = remember {
        listOf(
            "🔬 Explain Methodology" to "Explain the core methodology, experimental framework, and datasets used in this paper in clear and rigorous terms.",
            "💡 Key Contributions" to "What are the 3 most significant contributions, novel algorithms, or empirical findings presented in this manuscript?",
            "⚠️ Limitations & Risks" to "What are the critical limitations, potential biases, and unaddressed questions in this paper?",
            "📝 Synthesize Review" to "Draft a 1-paragraph literature review synthesis and standard BibTeX citation for this paper."
        )
    }

    // Function to query Gemini
    fun executeQuery(prompt: String) {
        if (prompt.isBlank() || isThinking) return

        val userMsg = AiCoPilotMessage(text = prompt, isUser = true)
        messages = messages + userMsg
        isThinking = true

        scope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    delay(300)
                    messages = messages + AiCoPilotMessage(
                        text = "⚠️ Gemini API key is not configured. Please supply GEMINI_API_KEY in your gradle properties or .env to activate the AI Research Co-Pilot.",
                        isUser = false,
                        isError = true
                    )
                    isThinking = false
                    return@launch
                }

                val systemPrompt = """
                    You are Cite Circle's Academic Research Co-Pilot, an expert scientific collaborator embedded directly inside the PDF reader.
                    
                    MANUSCRIPT CONTEXT:
                    - Title: "$paperTitle"
                    - Authors: ${paperAuthors.ifBlank { "Not specified" }}
                    - Year: ${paperYear.ifBlank { "N/A" }}
                    - Venue: ${paperVenue.ifBlank { "Preprint" }}
                    - DOI: ${paperDoi.ifBlank { "N/A" }}
                    - Abstract: ${paperAbstract.ifBlank { "No abstract provided" }}
                    
                    INSTRUCTIONS:
                    1. Ground your answers specifically in the provided paper context.
                    2. Provide deep, accurate, and rigorous scientific reasoning.
                    3. Format complex insights with clean bullet points, bold headers, and mathematical clarity.
                    4. When discussing formulas, use standard LaTeX notation.
                    5. Keep responses concise, objective, and scholarly.
                """.trimIndent()

                val historyContents = messages.map { m ->
                    Content(
                        role = if (m.isUser) "user" else "model",
                        parts = listOf(Part(text = m.text))
                    )
                }

                val request = GenerateContentRequest(
                    contents = historyContents,
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent("gemini-2.5-flash", apiKey, request)
                }

                val aiText = response.candidates?.firstOrNull()?.content?.parts
                    ?.mapNotNull { it.text }
                    ?.joinToString("\n")
                    ?.trim()
                    ?: "No response generated."

                messages = messages + AiCoPilotMessage(text = aiText, isUser = false)
            } catch (e: Exception) {
                messages = messages + AiCoPilotMessage(
                    text = "Connection error: ${e.localizedMessage ?: "Failed to reach Gemini API"}",
                    isUser = false,
                    isError = true
                )
            } finally {
                isThinking = false
            }
        }
    }

    // Auto-execute initial prompt if provided (e.g. from Excerpt Highlighter)
    LaunchedEffect(initialPrompt) {
        if (initialPrompt.isNotBlank() && messages.isEmpty()) {
            executeQuery(initialPrompt)
        }
    }

    // Auto-scroll on new message
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.85f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // -------------------------------------------------------------
            // Header Bar
            // -------------------------------------------------------------
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BrandBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "AI Research Co-Pilot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BrandBlue.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    "Gemini 2.5 Flash",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            paperTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                messages = emptyList()
                                appendedIds = emptySet()
                            }
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // -------------------------------------------------------------
            // Quick Action Chips Row
            // -------------------------------------------------------------
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PageNeutral)
            ) {
                items(quickActions) { (label, prompt) ->
                    FilterChip(
                        selected = false,
                        onClick = { executeQuery(prompt) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = DividerLight
                        )
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = DividerLight)

            // -------------------------------------------------------------
            // Chat Message Stream
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(PageNeutral)
            ) {
                if (messages.isEmpty() && !isThinking) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(BrandBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Ask Anything About This Manuscript",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap an action chip above or type a specific question about experimental design, math equations, or findings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            CoPilotMessageCard(
                                message = msg,
                                isAppended = appendedIds.contains(msg.id),
                                isCopied = copiedId == msg.id,
                                onCopy = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("AI Insight", msg.text))
                                    copiedId = msg.id
                                    scope.launch {
                                        delay(2000)
                                        if (copiedId == msg.id) copiedId = null
                                    }
                                },
                                onAppendToNotes = {
                                    onAppendToNotes(msg.text)
                                    appendedIds = appendedIds + msg.id
                                }
                            )
                        }

                        if (isThinking) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = BrandBlue
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        "Synthesizing paper context with Gemini 2.5 Flash...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = DividerLight)

            // -------------------------------------------------------------
            // Input Bar
            // -------------------------------------------------------------
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask about this paper...", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 120.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceInset,
                            unfocusedContainerColor = SurfaceInset,
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isThinking) {
                                    val q = inputText.trim()
                                    inputText = ""
                                    executeQuery(q)
                                }
                            }
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isThinking) BrandBlue else BrandBlue.copy(alpha = 0.3f))
                            .clickable(enabled = inputText.isNotBlank() && !isThinking) {
                                val q = inputText.trim()
                                inputText = ""
                                executeQuery(q)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoPilotMessageCard(
    message: AiCoPilotMessage,
    isAppended: Boolean,
    isCopied: Boolean,
    onCopy: () -> Unit,
    onAppendToNotes: () -> Unit
) {
    if (message.isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp),
                color = BrandBlue,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "AI Analysis",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = onCopy,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            if (isCopied) Icons.Outlined.Check else Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = if (isCopied) Color(0xFF057642) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isCopied) "Copied" else "Copy",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCopied) Color(0xFF057642) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = onAppendToNotes,
                        enabled = !isAppended,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(
                            if (isAppended) Icons.Outlined.Check else Icons.Filled.Notes,
                            contentDescription = null,
                            tint = if (isAppended) Color(0xFF057642) else BrandBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (isAppended) "Saved in Notes" else "+ Note",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAppended) Color(0xFF057642) else BrandBlue
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
