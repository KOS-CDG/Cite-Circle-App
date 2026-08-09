package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SavedPaper
import com.example.ui.theme.Gradients
import com.example.ui.theme.Spacing
import com.example.ui.theme.eyebrowTight
import com.example.ui.theme.meta
import com.example.ui.theme.postBody

/**
 * Takes callbacks rather than a HomeViewModel. A shared component should not depend on a specific
 * screen's ViewModel -- that coupling is what made the profile screen and the feed unable to
 * evolve independently.
 */
@Composable
fun PostCard(
    paper: SavedPaper,
    onToggleEndorse: (id: String, currentlyEndorsed: Boolean) -> Unit,
    onComment: () -> Unit = {},
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(Spacing.avatarMd)
                        .clip(CircleShape)
                        .background(Gradients.avatarFallback(paper.id)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        paper.authorInitials,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            paper.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            "· ${paper.timeAgo}",
                            style = MaterialTheme.typography.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        paper.affiliation,
                        style = MaterialTheme.typography.eyebrowTight,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                paper.content,
                style = MaterialTheme.typography.postBody,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(Spacing.base))
            CitationBlock(paper.citation)
            Spacer(modifier = Modifier.height(Spacing.base))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                OutlinedButton(
                    onClick = { onToggleEndorse(paper.id, paper.isEndorsed) },
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.touchTarget),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(
                        1.dp,
                        if (paper.isEndorsed) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (paper.isEndorsed) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                        contentColor = if (paper.isEndorsed) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ),
                ) {
                    Icon(
                        if (paper.isEndorsed) Icons.Filled.Verified else Icons.Outlined.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        if (paper.isEndorsed) "Endorsed" else "Endorse",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                // Was a "VIEW CONTEXT" button with onClick = { } -- a dead affordance. Now the
                // comment entry point; the thread sheet itself arrives with the feed work.
                OutlinedButton(
                    onClick = onComment,
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.touchTarget),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Discuss", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
