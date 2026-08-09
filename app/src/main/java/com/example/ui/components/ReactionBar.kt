package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EmojiObjects
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Spacing

/**
 * Academic reactions, not Facebook's. Endorse preserves the existing isEndorsed semantics so the
 * one working social interaction in the app does not regress.
 */
enum class Reaction(val key: String, val label: String) {
    ENDORSE("ENDORSE", "Endorse"),
    INSIGHTFUL("INSIGHTFUL", "Insightful"),
    CITE_WORTHY("CITE_WORTHY", "Cite-worthy"),
    ;

    companion object {
        fun fromKey(key: String?): Reaction? = entries.firstOrNull { it.key == key }
    }
}

@Composable
private fun Reaction.color(): Color = when (this) {
    Reaction.ENDORSE -> MaterialTheme.colorScheme.primary
    Reaction.INSIGHTFUL -> MaterialTheme.colorScheme.tertiary
    Reaction.CITE_WORTHY -> MaterialTheme.colorScheme.secondary
}

private fun Reaction.icon(selected: Boolean): ImageVector = when (this) {
    Reaction.ENDORSE -> if (selected) Icons.Filled.Verified else Icons.Outlined.Verified
    Reaction.INSIGHTFUL -> Icons.Outlined.EmojiObjects
    Reaction.CITE_WORTHY -> Icons.Outlined.PushPin
}

@Composable
fun ReactionBar(
    selected: Reaction?,
    commentCount: Int,
    onReact: (Reaction) -> Unit,
    onComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Reaction.entries.forEach { reaction ->
            ReactionButton(
                reaction = reaction,
                isSelected = selected == reaction,
                onClick = { onReact(reaction) },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onComment)
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Comments",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (commentCount > 0) {
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text(
                    "$commentCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReactionButton(
    reaction: Reaction,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Springy scale on selection -- the cheapest way to make a tap feel like it landed.
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "reactionScale",
    )
    val tint = if (isSelected) reaction.color() else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .height(Spacing.touchTarget)
            .scale(scale)
            .clip(MaterialTheme.shapes.small)
            .background(
                if (isSelected) {
                    reaction.color().copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            reaction.icon(isSelected),
            contentDescription = reaction.label,
            modifier = Modifier.size(18.dp),
            tint = tint,
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            reaction.label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
        )
    }
}
