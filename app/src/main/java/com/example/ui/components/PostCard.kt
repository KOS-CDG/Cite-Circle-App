package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SavedPaper
import com.example.ui.theme.AcademicField
import com.example.ui.theme.Gradients
import com.example.ui.theme.Spacing
import com.example.ui.theme.accent
import com.example.ui.theme.eyebrowTight
import com.example.ui.theme.meta
import com.example.ui.theme.postBody
import com.example.util.TimeFormat

/**
 * Takes callbacks rather than a HomeViewModel. A shared component should not depend on a specific
 * screen's ViewModel -- that coupling is what made the profile screen and the feed unable to
 * evolve independently.
 */
@Composable
fun PostCard(
    paper: SavedPaper,
    onReact: (Reaction) -> Unit,
    onComment: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val field = AcademicField.fromKey(paper.fieldKey)
    val accent = field.accent()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        // Field-accent top edge: the per-discipline palette doing scanning work.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(accent.base),
        )
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
                            "· " + if (paper.createdAt > 0L) {
                                TimeFormat.relative(paper.createdAt)
                            } else {
                                paper.timeAgo
                            },
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

            ReactionBar(
                selected = Reaction.fromKey(paper.myReaction),
                commentCount = paper.commentCount,
                onReact = onReact,
                onComment = onComment,
            )
        }
    }
}
