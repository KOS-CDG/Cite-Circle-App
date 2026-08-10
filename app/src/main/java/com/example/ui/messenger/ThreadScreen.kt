package com.example.ui.messenger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.messenger.DeliveryStatus
import com.example.ui.components.Avatar
import com.example.ui.theme.BubbleShapes
import com.example.ui.theme.Gradients
import com.example.ui.theme.Spacing
import com.example.ui.theme.bubbleText
import com.example.ui.theme.eyebrowTight
import com.example.ui.theme.meta
import com.example.util.TimeFormat

private val quickReactions = listOf("👍", "❤️", "😂", "😮", "😢", "🎉")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    viewModel: ThreadViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // reverseLayout keeps new messages pinned to the bottom and makes the IME behave, so the
    // newest item is index 0 -- scroll there when anything arrives.
    LaunchedEffect(state.items.size, state.isTyping) {
        listState.animateScrollToItem(0)
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(
                            initials = state.initials,
                            seed = state.title,
                            size = 36.dp,
                            showPresence = true,
                            isOnline = state.isOnline,
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column {
                            Text(
                                state.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                            )
                            Text(
                                state.subtitle,
                                style = MaterialTheme.typography.meta,
                                color = if (state.isOnline) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                reverseLayout = true,
                contentPadding = PaddingValues(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                if (state.isTyping) {
                    item(key = "typing") { TypingIndicator() }
                }

                if (state.showSeenReceipt) {
                    item(key = "seen") { SeenReceipt(initials = state.initials, seed = state.title) }
                }

                // The item list is oldest-first; reverseLayout renders index 0 at the bottom,
                // so it has to be walked backwards.
                items(
                    count = state.items.size,
                    key = { index -> state.items[state.items.lastIndex - index].key },
                ) { index ->
                    when (val item = state.items[state.items.lastIndex - index]) {
                        is ThreadItem.Day -> DaySeparator(item.label)
                        is ThreadItem.Bubble -> MessageRow(
                            item = item,
                            onReact = { emoji -> viewModel.react(item.message.id, emoji) },
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Spacing.screenHorizontal,
                        vertical = Spacing.sm,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                    maxLines = 4,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.send(input)
                            input = ""
                        }
                    },
                    enabled = input.isNotBlank(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (input.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySeparator(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.eyebrowTight,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRow(
    item: ThreadItem.Bubble,
    onReact: (String) -> Unit,
) {
    var showReactions by remember { mutableStateOf(false) }
    // Tapping a bubble reveals its exact time -- the standard messenger affordance, and what the
    // bubble's onClick was missing. It showed a ripple and did nothing, so every bubble looked
    // tappable and none was.
    var timeRevealed by remember { mutableStateOf(false) }
    val isMine = item.isMine

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!isMine) {
                // The avatar shows only next to the last bubble of a run; earlier bubbles get a
                // spacer so the run stays aligned.
                if (item.isLastInGroup) {
                    Avatar(
                        initials = item.senderInitials,
                        seed = item.senderId,
                        size = 28.dp,
                    )
                } else {
                    Spacer(modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
            }

            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(
                            if (isMine) {
                                BubbleShapes.outgoing(item.isFirstInGroup, item.isLastInGroup)
                            } else {
                                BubbleShapes.incoming(item.isFirstInGroup, item.isLastInGroup)
                            },
                        )
                        .then(
                            if (isMine) {
                                Modifier.background(Gradients.outgoingBubble())
                            } else {
                                Modifier.background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                )
                            },
                        )
                        .combinedClickable(
                            onClick = { timeRevealed = !timeRevealed },
                            onLongClick = { showReactions = true },
                            onDoubleClick = { onReact("❤️") },
                        )
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                ) {
                    Text(
                        item.message.text,
                        style = MaterialTheme.typography.bubbleText,
                        color = if (isMine) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }

                if (item.message.reactions.isNotEmpty()) {
                    Text(
                        item.message.reactions.values.joinToString(""),
                        style = MaterialTheme.typography.meta,
                        modifier = Modifier
                            .padding(top = Spacing.xxs)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                    )
                }

                if (item.showTimestamp || timeRevealed) {
                    Text(
                        buildString {
                            append(TimeFormat.clockTime(item.message.sentAt))
                            if (isMine) append(" · ").append(item.message.status.label())
                        },
                        style = MaterialTheme.typography.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            top = Spacing.xxs,
                            bottom = Spacing.xs,
                        ),
                    )
                }
            }
        }

        AnimatedVisibility(visible = showReactions) {
            Row(
                modifier = Modifier
                    .padding(vertical = Spacing.xs)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                quickReactions.forEach { emoji ->
                    Text(
                        emoji,
                        modifier = Modifier
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = {
                                    onReact(emoji)
                                    showReactions = false
                                },
                            )
                            .padding(Spacing.xs),
                    )
                }
            }
        }
    }
}

private fun DeliveryStatus.label(): String = when (this) {
    DeliveryStatus.SENDING -> "Sending"
    DeliveryStatus.SENT -> "Sent"
    DeliveryStatus.DELIVERED -> "Delivered"
    DeliveryStatus.SEEN -> "Seen"
}

@Composable
private fun SeenReceipt(initials: String, seed: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xxs),
        horizontalArrangement = Arrangement.End,
    ) {
        Avatar(initials = initials, seed = seed, size = 14.dp)
    }
}

/** Three dots with a staggered fade, inside an incoming-shaped bubble. */
@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier.padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(28.dp + Spacing.sm))
        Row(
            modifier = Modifier
                .clip(BubbleShapes.single)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            repeat(3) { index ->
                val alpha by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * 150),
                    ),
                    label = "dot$index",
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
                )
            }
        }
    }
}
