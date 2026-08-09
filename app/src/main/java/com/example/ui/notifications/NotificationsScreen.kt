package com.example.ui.notifications

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Gradients
import com.example.ui.theme.Spacing
import com.example.ui.theme.citation
import com.example.ui.theme.eyebrow
import com.example.ui.theme.eyebrowTight

/**
 * Still rendering two hardcoded cards, exactly as before. A real notification model with types,
 * unread state and badges is a later phase; this move is structural only.
 */
@Composable
fun NotificationsScreen(onOpenDetail: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = Spacing.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    "NEW FORMAL CITATION",
                    style = MaterialTheme.typography.eyebrow,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        item { CitationNotificationCard(read = false, onClick = onOpenDetail) }
        item { CitationNotificationCard(read = true, onClick = onOpenDetail) }
    }
}

@Composable
fun CitationNotificationCard(read: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (read) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            },
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = if (read) 0.dp else 2.dp),
        border = if (read) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else {
            null
        },
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Text(
                "Your publication has been formally referenced by Dr. Julian Thorne in a new " +
                    "preprint released to the Theoretical Physics circle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.md))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                "Entropy and the Architecture of Distributed Knowledge Systems",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                "Published Oct 2023 · ID: CC-882-XJ",
                style = MaterialTheme.typography.citation,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.md))
            QuotedExcerpt()
            Spacer(modifier = Modifier.height(Spacing.base))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(Spacing.avatarSm)
                            .clip(CircleShape)
                            .background(Gradients.avatarFallback("julian-thorne")),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "JT",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        "AFFILIATION: CERN",
                        style = MaterialTheme.typography.eyebrowTight,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    "REVIEW FULL PAPER",
                    style = MaterialTheme.typography.eyebrowTight,
                    color = MaterialTheme.colorScheme.secondary,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

/**
 * Row + IntrinsicSize.Min, not Box + fillMaxHeight.
 *
 * The accent rail has to match the height of the quote text next to it. fillMaxHeight resolves
 * against the incoming max constraint, which is fine in a plain Column but is infinite inside the
 * verticalScroll on the detail screen -- so the rail would either blow up or collapse. Measuring
 * the Row at its minimum intrinsic height makes fillMaxHeight mean "as tall as the text".
 */
@Composable
internal fun QuotedExcerpt() {
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
            "\"...as proposed in Thorne's recent synthesis, the friction within localized data " +
                "clusters mirrors the thermodynamic decay observed in early archival structures " +
                "(Thorne, 2023).\"",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(Spacing.base),
        )
    }
}
