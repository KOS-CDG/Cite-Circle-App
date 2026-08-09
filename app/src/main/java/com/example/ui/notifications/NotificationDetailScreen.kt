package com.example.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.data.notifications.NotificationType
import com.example.ui.components.Avatar
import com.example.ui.theme.Spacing
import com.example.ui.theme.eyebrow
import com.example.util.TimeFormat

/**
 * Parameterized by notification id. The previous version took no argument at all and rendered
 * the same hardcoded "Dr. Julian Thorne" card no matter which notification you tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    viewModel: NotificationsViewModel,
    notificationId: String,
    onBack: () -> Unit,
    onMessage: (userId: String) -> Unit,
) {
    val notification = viewModel.byId(notificationId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification") },
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
        if (notification == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "This notification is no longer available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screenHorizontal),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(Spacing.cardPadding)) {
                    Text(
                        notification.type.name.replace('_', ' '),
                        style = MaterialTheme.typography.eyebrow,
                        color = notification.type.color(),
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            Avatar(
                                initials = notification.actorInitials,
                                seed = notification.actorId,
                                size = Spacing.avatarMd,
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerLowest,
                                    )
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
                        Column {
                            Text(
                                notification.actorName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                TimeFormat.relative(notification.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.base))
                    Text(
                        notification.body.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    notification.detail?.let { detail ->
                        Spacer(modifier = Modifier.height(Spacing.base))
                        QuotedExcerpt(detail)
                    }

                    Spacer(modifier = Modifier.height(Spacing.base))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(Spacing.base))

                    when (notification.type) {
                        NotificationType.CONNECTION_REQUEST -> Row(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Button(
                                onClick = { },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text("Accept")
                            }
                            Spacer(modifier = Modifier.width(Spacing.md))
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small,
                            ) {
                                Text("Decline")
                            }
                        }

                        NotificationType.MESSAGE -> Button(
                            onClick = { onMessage(notification.actorId) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text("Open conversation")
                        }

                        else -> Button(
                            onClick = { },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text("View full paper")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Row + IntrinsicSize.Min rather than Box + fillMaxHeight: the accent rail has to match the text
 * height, and fillMaxHeight resolves against the incoming max constraint, which is infinite
 * inside this screen's verticalScroll.
 */
@Composable
internal fun QuotedExcerpt(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.tertiary),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(Spacing.base),
        )
    }
}
