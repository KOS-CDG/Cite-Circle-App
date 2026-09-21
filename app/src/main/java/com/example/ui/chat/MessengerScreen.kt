package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.Avatar
import com.example.MyApplication
import com.example.R
import com.example.data.AuthorIdentity
import com.example.data.SavedPaper
import com.example.data.chat.ChatRepository
import com.example.data.chat.ConversationEntity
import com.example.data.formatTimeAgo
import com.example.ui.components.EmptyState
import kotlinx.coroutines.flow.flowOf

import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerLight
import com.example.ui.theme.SurfaceInset

private data class CollaboratorContact(
    val name: String,
    val initials: String,
    val affiliation: String
)

@Composable
fun MessengerScreen(
    chatRepository: ChatRepository,
    navController: NavController
) {
    val context = LocalContext.current
    val identity = remember(context) { AuthorIdentity.current(context) }
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as? MyApplication
    val savedPapersFlow = remember(app) {
        app?.repository?.allPapers ?: flowOf(emptyList<SavedPaper>())
    }
    val savedPapers by savedPapersFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    var showCameraModal by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var userNote by rememberSaveable { mutableStateOf("") }

    val conversations by chatRepository.conversations.collectAsStateWithLifecycle(initialValue = emptyList())
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filtered = remember(conversations, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) conversations
        else conversations.filter {
            it.participantName.lowercase().contains(q) ||
            it.participantAffiliation.lowercase().contains(q) ||
            it.lastMessage.lowercase().contains(q) ||
            it.attachedPaperTitle.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top header ────────────────────────────────────────────────────────
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button to safely exit Messenger from any entry point
                    IconButton(
                        onClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate("feed") {
                                    popUpTo("feed") { inclusive = false }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Current user avatar (tapping opens profile)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { navController.navigate("profile") }
                    ) {
                        Avatar(identity.initials, 36.dp)
                    }

                    Spacer(Modifier.width(10.dp))

                    // Start-aligned screen title: "Chats"
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.weight(1f))

                    // Right: action icons in circular SurfaceInset buttons (Messenger native)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceInset)
                                .clickable { showCameraModal = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Camera",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceInset)
                                .clickable { showNewChatDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "New Conversation",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
            }
        }

        // ── Search pill (BasicTextField with pixel-perfect vertical centering) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(BrandBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceInset),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search conversations and researchers...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                        if (searchQuery.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        // ── Main list ─────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Item A: Gemini AI Assistant banner (Messenger-style inset card)
            item(key = "banner_ai") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BrandBlue)
                            .clickable { navController.navigate("chat") }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini Research Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Multimodal AI research & citation partner",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Chat",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Item B: Active Now section
            if (conversations.isNotEmpty()) {
                item(key = "section_active_now") {
                    Column {
                        Text(
                            text = "Active Now",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Self "Your Note" card
                            item(key = "active_self") {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(68.dp)
                                        .clickable { showNoteDialog = true }
                                ) {
                                    if (userNote.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .border(0.5.dp, DividerLight, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = userNote,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                    }
                                    Box(
                                        modifier = Modifier.size(52.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, BrandBlue, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Avatar(identity.initials, 44.dp)
                                        }
                                        // Add overlay at bottom-end
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .align(Alignment.BottomEnd),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = if (userNote.isNotBlank()) "Your Note" else "Share Note",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Collaborator items (max 5)
                            items(
                                conversations.take(5),
                                key = { "active_${it.id}" }
                            ) { conv ->
                                ActiveCollaboratorItem(conv) {
                                    navController.navigate("chat_thread/${conv.id}")
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
                    }
                }
            }

            // Items C+: Conversation rows or empty state
            if (filtered.isEmpty()) {
                item(key = "empty_state") {
                    EmptyState(
                        title = if (searchQuery.isNotBlank()) "No Conversations Found" else stringResource(R.string.messenger_empty_title),
                        message = if (searchQuery.isNotBlank()) "No discussions matched \"$searchQuery\"." else stringResource(R.string.messenger_empty_message),
                        icon = Icons.Outlined.ChatBubbleOutline,
                        modifier = Modifier.padding(32.dp),
                        actionLabel = if (searchQuery.isNotBlank()) "Clear Search" else "Start Discussion",
                        onAction = {
                            if (searchQuery.isNotBlank()) searchQuery = "" else showNewChatDialog = true
                        }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { conv ->
                    ConversationRow(
                        conversation = conv,
                        onClick = { navController.navigate("chat_thread/${conv.id}") }
                    )
                }
            }
        }
    }

    if (showCameraModal) {
        AlertDialog(
            onDismissRequest = { showCameraModal = false },
            title = {
                Text(
                    text = "Preprint Camera & Figure Scanner",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Capture whiteboard derivations or scan diagrams from print manuscripts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            showCameraModal = false
                            navController.navigate("compose")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Capture Manuscript Figure")
                    }
                    OutlinedButton(
                        onClick = {
                            showCameraModal = false
                            navController.navigate("compose")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Import Figure from Gallery")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCameraModal = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showNewChatDialog) {
        var participantName by remember { mutableStateOf("") }
        val distinctAuthors = remember(savedPapers) {
            savedPapers.map {
                CollaboratorContact(it.authorName, it.authorInitials, it.affiliation)
            }.distinctBy { it.name }
        }
        val matchingAuthors = remember(distinctAuthors, participantName) {
            val q = participantName.trim().lowercase()
            if (q.isBlank()) distinctAuthors.take(4)
            else distinctAuthors.filter {
                it.name.lowercase().contains(q) || it.affiliation.lowercase().contains(q)
            }.take(4)
        }

        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = {
                Text(
                    text = "New Academic Discussion",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Start an encrypted peer review or co-author discussion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = participantName,
                        onValueChange = { participantName = it },
                        label = { Text("Researcher Name or Subject") },
                        placeholder = { Text("e.g. Dr. Geoffrey Hinton") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (matchingAuthors.isNotEmpty()) {
                        Text(
                            text = if (participantName.isBlank()) "Recent Co-Authors & Cited Researchers" else "Matching Researchers",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            matchingAuthors.forEach { author ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            scope.launch {
                                                val convId = chatRepository.startOrGetConversationWithAuthor(
                                                    author.name,
                                                    author.initials,
                                                    author.affiliation
                                                )
                                                showNewChatDialog = false
                                                navController.navigate("chat_thread/$convId")
                                            }
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Avatar(author.initials, 32.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(author.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(author.affiliation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (participantName.isNotBlank()) {
                            scope.launch {
                                val initials = participantName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                                val convId = chatRepository.startOrGetConversationWithAuthor(
                                    participantName.trim(),
                                    if (initials.isNotBlank()) initials else "AC",
                                    "Academic Peer"
                                )
                                showNewChatDialog = false
                                navController.navigate("chat_thread/$convId")
                            }
                        }
                    },
                    enabled = participantName.isNotBlank()
                ) {
                    Text("Start Discussion")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNoteDialog) {
        var noteDraft by remember { mutableStateOf(userNote) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = {
                Text("Set Academic Status Note", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Share what you are currently researching, reviewing, or writing with your peer network.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { if (it.length <= 60) noteDraft = it },
                        label = { Text("Status Note (max 60 chars)") },
                        placeholder = { Text("e.g. Drafting NeurIPS rebuttal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userNote = noteDraft.trim()
                        showNoteDialog = false
                    }
                ) {
                    Text("Update Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── Active Collaborator Chip ─────────────────────────────────────────────────

@Composable
private fun ActiveCollaboratorItem(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(68.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp),
            contentAlignment = Alignment.Center
        ) {
            // 2dp solid BrandBlue border ring around the avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(2.dp, BrandBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Avatar(conversation.participantInitials, 44.dp)
            }

            // Online indicator dot at BottomEnd
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = conversation.participantName.split(" ").firstOrNull()
                ?: conversation.participantName,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Conversation Row ─────────────────────────────────────────────────────────

@Composable
private fun ConversationRow(
    conversation: ConversationEntity,
    onClick: () -> Unit
) {
    val isUnread = conversation.unreadCount > 0

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick)
                .defaultMinSize(minHeight = 72.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Avatar with optional online dot ──────────────────────────────
            Box(contentAlignment = Alignment.BottomEnd) {
                Avatar(conversation.participantInitials, 54.dp)
                if (conversation.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // ── Name / timestamp / snippet ───────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                // Row 1: Name + timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.participantName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTimeAgo(conversation.lastMessageTimestamp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(2.dp))

                // Row 2: Snippet + unread badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        color = if (isUnread) MaterialTheme.colorScheme.onSurface
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isUnread) {
                        Spacer(Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(conversation.unreadCount.toString())
                        }
                    }
                }

                // Paper attachment chip
                if (conversation.attachedPaperTitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Article,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = conversation.attachedPaperTitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
    }
}
