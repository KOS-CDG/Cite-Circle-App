package com.example.ui.notifications

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.notifications.AppNotification
import com.example.data.notifications.NotificationType
import com.example.ui.components.Avatar
import com.example.ui.components.EmptyState
import com.example.ui.theme.Spacing
import com.example.ui.theme.eyebrow
import com.example.ui.theme.meta
import com.example.util.TimeFormat
import java.util.concurrent.TimeUnit

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onOpenDetail: (String) -> Unit,
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()

    if (notifications.isEmpty()) {
        EmptyState(
            title = "Nothing new",
            message = "Citations, reactions and connection requests will appear here.",
            icon = Icons.Outlined.NotificationsNone,
        )
        return
    }

    val cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
    val recent = notifications.filter { !it.isRead || it.createdAt >= cutoff }
    val earlier = notifications - recent.toSet()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = Spacing.xxl),
    ) {
        if (unread > 0) {
            item(key = "mark-all") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = viewModel::markAllRead) {
                        Text("Mark all read", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            item(key = "new-header") { SectionHeader("NEW") }
            items(recent, key = { it.id }) { notification ->
                NotificationRow(
                    notification = notification,
                    onClick = {
                        viewModel.markRead(notification.id)
                        onOpenDetail(notification.id)
                    },
                    // Marking one read moves it between the New and Earlier groups.
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (earlier.isNotEmpty()) {
            item(key = "earlier-header") { SectionHeader("EARLIER") }
            items(earlier, key = { it.id }) { notification ->
                NotificationRow(
                    notification = notification,
                    onClick = {
                        viewModel.markRead(notification.id)
                        onOpenDetail(notification.id)
                    },
                    // Marking one read moves it between the New and Earlier groups.
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            horizontal = Spacing.screenHorizontal,
            vertical = Spacing.sm,
        ),
    )
}

/** Each notification type gets its own colour and glyph, so the list is scannable by shape. */
@Composable
internal fun NotificationType.color(): Color = when (this) {
    NotificationType.CITATION -> MaterialTheme.colorScheme.primary
    NotificationType.REACTION -> MaterialTheme.colorScheme.secondary
    NotificationType.COMMENT -> MaterialTheme.colorScheme.tertiary
    NotificationType.CONNECTION_REQUEST -> MaterialTheme.colorScheme.primary
    NotificationType.MESSAGE -> MaterialTheme.colorScheme.secondary
}

internal fun NotificationType.icon(): ImageVector = when (this) {
    NotificationType.CITATION -> Icons.Filled.FormatQuote
    NotificationType.REACTION -> Icons.Filled.Verified
    NotificationType.COMMENT -> Icons.Filled.ChatBubble
    NotificationType.CONNECTION_REQUEST -> Icons.Filled.PersonAdd
    NotificationType.MESSAGE -> Icons.AutoMirrored.Filled.Message
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unread = !notification.isRead

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (unread) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.background
                },
            )
            .padding(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.md,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        Box {
            Avatar(
                initials = notification.actorInitials,
                seed = notification.actorId,
                size = Spacing.avatarMd,
            )
            // Type badge overlapping the avatar -- the multi-colour system doing useful work
            // rather than decoration.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(notification.type.color()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    notification.type.icon(),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(notification.actorName)
                    }
                    append(" ")
                    append(notification.body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            notification.detail?.let { detail ->
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                TimeFormat.relative(notification.createdAt),
                style = MaterialTheme.typography.meta,
                color = if (unread) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        if (unread) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
