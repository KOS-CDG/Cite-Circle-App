package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.Avatar
import com.example.HomeViewModel
import com.example.R
import com.example.data.CitationFormatter
import com.example.data.CitationStyle
import com.example.data.SavedPaper
import com.example.data.chat.ChatMessageEntity
import com.example.data.chat.ChatRepository
import com.example.data.formatTimeAgo
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerLight
import com.example.ui.theme.SurfaceInset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    conversationId: String,
    chatRepository: ChatRepository,
    homeViewModel: HomeViewModel,
    navController: NavController
) {
    val conversation by chatRepository.conversation(conversationId).collectAsStateWithLifecycle(initialValue = null)
    val messages by chatRepository.messagesForConversation(conversationId).collectAsStateWithLifecycle(initialValue = emptyList())
    val savedPapers by homeViewModel.savedPapers.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by rememberSaveable { mutableStateOf("") }
    var selectedPaperToCite by remember { mutableStateOf<SavedPaper?>(null) }
    var showPaperPicker by remember { mutableStateOf(false) }
    var showVoiceCallDialog by remember { mutableStateOf(false) }
    var showVideoCallDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        chatRepository.markAsRead(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Modal sheet to pick a paper from saved library to cite inside the chat
    if (showPaperPicker) {
        ModalBottomSheet(
            onDismissRequest = { showPaperPicker = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    stringResource(R.string.messenger_select_paper_to_share),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                if (savedPapers.isEmpty()) {
                    Text(
                        "No papers found in your library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedPapers, key = { it.id }) { paper ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPaperToCite = paper
                                        showPaperPicker = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        paper.title.ifBlank { paper.content },
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "${paper.authorName} • ${paper.venue.ifBlank { paper.year }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (conversation != null) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Avatar(conversation!!.participantInitials, 38.dp)
                            if (conversation!!.isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(AccentGreen)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                conversation!!.participantName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (conversation!!.isOnline) "Online • ${conversation!!.participantAffiliation}"
                                else conversation!!.participantAffiliation,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (conversation!!.isOnline) AccentGreen
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (conversation!!.attachedPaperId.isNotBlank()) {
                            TextButton(
                                onClick = { navController.navigate("post/${conversation!!.attachedPaperId}") }
                            ) {
                                Text(
                                    stringResource(R.string.messenger_view_paper),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Action icons: voice call, video call, info
                        IconButton(onClick = { showVoiceCallDialog = true }) {
                            Icon(
                                Icons.Filled.Call,
                                contentDescription = "Voice Call",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showVideoCallDialog = true }) {
                            Icon(
                                Icons.Filled.Videocam,
                                contentDescription = "Video Call",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Citation snippet preview if a paper is staged to attach
                selectedPaperToCite?.let { paper ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.Article,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Attaching: ${paper.title.ifBlank { paper.content }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { selectedPaperToCite = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left attachment icons (Messenger-style)
                    IconButton(onClick = { showPaperPicker = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Filled.AddCircle,
                            contentDescription = "Attach Paper",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            inputText = if (inputText.isBlank()) "[Figure Attached]" else "$inputText [Figure Attached]"
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Camera",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            inputText = if (inputText.isBlank()) "[Chart Attached]" else "$inputText [Chart Attached]"
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = "Gallery",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            inputText = if (inputText.isBlank()) "[Audio Note 0:15]" else "$inputText [Audio Note 0:15]"
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Audio",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Paper citation picker icon
                    IconButton(
                        onClick = { showPaperPicker = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.FormatQuote,
                            contentDescription = stringResource(R.string.cd_attach_paper),
                            tint = if (selectedPaperToCite != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                stringResource(
                                    R.string.messenger_input_placeholder,
                                    conversation?.participantName?.split(" ")?.firstOrNull() ?: ""
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(Modifier.width(4.dp))

                    // Dynamic right button: Send when content available, ThumbUp otherwise
                    val canSend = inputText.isNotBlank() || selectedPaperToCite != null
                    if (canSend) {
                        IconButton(
                            onClick = {
                                val textToSend = inputText
                                val paper = selectedPaperToCite
                                inputText = ""
                                selectedPaperToCite = null
                                scope.launch {
                                    chatRepository.sendMessage(
                                        conversationId = conversationId,
                                        text = textToSend,
                                        quotedPaperId = paper?.id.orEmpty(),
                                        quotedPaperTitle = paper?.title.orEmpty(),
                                        quotedCitation = paper?.let {
                                            CitationFormatter.format(it, CitationStyle.DEFAULT)
                                        }.orEmpty()
                                    )
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = stringResource(R.string.cd_send),
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    chatRepository.sendMessage(
                                        conversationId = conversationId,
                                        text = "Endorsed your research update",
                                        quotedPaperId = "",
                                        quotedPaperTitle = "",
                                        quotedCitation = ""
                                    )
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Filled.ThumbUp,
                                contentDescription = "Endorse",
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(message = message, navController = navController)
            }
        }
    }

    if (showVoiceCallDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceCallDialog = false },
            title = { Text("Encrypted Voice Session") },
            text = { Text("Connecting audio channel with ${conversation?.participantName ?: "researcher"} (${conversation?.participantAffiliation ?: "Academic Circle"}).") },
            confirmButton = {
                TextButton(onClick = { showVoiceCallDialog = false }) {
                    Text("End Session", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (showVideoCallDialog) {
        AlertDialog(
            onDismissRequest = { showVideoCallDialog = false },
            title = { Text("Peer Review Video Seminar") },
            text = { Text("Starting secure video collaboration room with ${conversation?.participantName ?: "researcher"}.") },
            confirmButton = {
                TextButton(onClick = { showVideoCallDialog = false }) {
                    Text("Leave Seminar", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Conversation Info") },
            text = {
                Column {
                    Text("Collaborator: ${conversation?.participantName ?: "Unknown"}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Affiliation: ${conversation?.participantAffiliation.orEmpty().ifBlank { "Academic Peer" }}")
                    if (conversation?.attachedPaperTitle?.isNotBlank() == true) {
                        Spacer(Modifier.height(6.dp))
                        Text("Discussion Subject: ${conversation?.attachedPaperTitle}")
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Security: End-to-End Academic Sandbox Protocol")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    navController: NavController
) {
    val alignment = if (message.isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    // Outgoing: solid BrandBlue. Incoming: solid SurfaceInset.
    val bgColor = if (message.isOutgoing) BrandBlue else SurfaceInset
    val textColor = if (message.isOutgoing) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (message.isOutgoing) 4.dp else 16.dp
                    )
                )
                .background(bgColor)
                .padding(12.dp)
                .widthIn(max = 300.dp)
        ) {
            // Embedded paper citation card if present
            if (message.quotedPaperTitle.isNotBlank() || message.quotedCitation.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (message.isOutgoing) MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .clickable {
                            if (message.quotedPaperId.isNotBlank()) {
                                navController.navigate("post/${message.quotedPaperId}")
                            }
                        }
                        .padding(10.dp)
                ) {
                    // Paper title row with Article icon instead of emoji
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Article,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (message.isOutgoing) MaterialTheme.colorScheme.tertiary
                                   else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            message.quotedPaperTitle.ifBlank { "Attached Paper" },
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (message.isOutgoing) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (message.quotedCitation.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            message.quotedCitation,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = if (message.isOutgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimeAgo(message.timestamp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = if (message.isOutgoing) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (message.isOutgoing) {
                    Spacer(Modifier.width(4.dp))
                    // DoneAll icon replaces double checkmark icon
                    Icon(
                        Icons.Filled.DoneAll,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
