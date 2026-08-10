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
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CitationNotification
import com.example.data.SampleData
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.EmptyState
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.QuoteBlock
import com.example.ui.components.ScreenHeader

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenNotification: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notifications = SampleData.notifications
    val unreadCount = notifications.count { !it.isRead }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(title = "NOTIFICATIONS", onBack = onBack)

        if (notifications.isEmpty()) {
            EmptyState(
                title = "All Caught Up",
                message = "New citations of your work will appear here.",
                icon = Icons.Outlined.NotificationsNone,
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(CiteCircleDefaults.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (unreadCount > 0) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            if (unreadCount == 1) {
                                "NEW FORMAL CITATION"
                            } else {
                                "$unreadCount NEW FORMAL CITATIONS"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                            ),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            items(notifications, key = { it.id }) { notification ->
                CitationNotificationCard(
                    notification = notification,
                    onClick = { onOpenNotification(notification.id) },
                )
            }
        }
    }
}

@Composable
fun CitationNotificationCard(
    notification: CitationNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val read = notification.isRead

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (read) 0.dp else 2.dp,
                shape = CiteCircleDefaults.CardShape,
                spotColor = Color(0x0D1A1A1A),
            )
            .clickable(
                onClickLabel = "Open citation from ${notification.citingAuthorName}",
                onClick = onClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (read) {
                MaterialTheme.colorScheme.background
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        shape = CiteCircleDefaults.CardShape,
        border = CiteCircleDefaults.cardBorder(alpha = if (read) 0.05f else 0.1f),
    ) {
        Column(modifier = Modifier.padding(CiteCircleDefaults.ScreenPadding)) {
            Text(
                notification.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                color = CiteCircleDefaults.hairlineColor(alpha = 0.05f),
                thickness = 1.dp,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                notification.paperTitle,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                notification.paperMeta,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))
            QuoteBlock(notification.quote)

            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(notification.citingAuthorInitials)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "AFFILIATION: ${notification.citingAffiliation.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    "REVIEW FULL PAPER",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = (-0.5).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}
