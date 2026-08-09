package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
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
    var pickerOpen by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Box(modifier = modifier) {
        if (pickerOpen) {
            ReactionPickerPopup(
                selected = selected,
                onPick = {
                    pickerOpen = false
                    onReact(it)
                },
                onDismiss = { pickerOpen = false },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Reaction.entries.forEach { reaction ->
                ReactionButton(
                    reaction = reaction,
                    isSelected = selected == reaction,
                    onClick = { onReact(reaction) },
                    onLongClick = {
                        // Haptic first: the picker animating in is the confirmation, but the tick
                        // is what tells you the long-press registered before anything is drawn.
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        pickerOpen = true
                    },
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
}

/**
 * Facebook's long-press reaction tray, in this app's vocabulary.
 *
 * Sits in a Popup rather than inside the card so it can overhang the card's bounds -- a tray
 * clipped to the post it belongs to is the usual way this effect goes wrong. It is focusable, so
 * a tap anywhere outside dismisses it and back closes it.
 */
@Composable
private fun ReactionPickerPopup(
    selected: Reaction?,
    onPick: (Reaction) -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(x = 0, y = with(density) { (-64).dp.roundToPx() }),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        // targetState flips on first composition, so the tray animates in rather than appearing
        // fully formed. There is no exit animation: the popup is removed the moment a reaction is
        // picked, which is faster than waiting out a fade and is what the gesture expects.
        val transition = remember { MutableTransitionState(false) }
        transition.targetState = true

        AnimatedVisibility(
            visibleState = transition,
            enter = scaleIn(
                initialScale = 0.8f,
                transformOrigin = TransformOrigin(0.15f, 1f),
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            ) + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 6.dp,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = Spacing.sm,
                        vertical = Spacing.xs,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Reaction.entries.forEach { reaction ->
                        val isSelected = selected == reaction
                        Row(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(
                                    if (isSelected) {
                                        reaction.color().copy(alpha = 0.16f)
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .clickable { onPick(reaction) }
                                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                reaction.icon(isSelected),
                                contentDescription = reaction.label,
                                tint = reaction.color(),
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                reaction.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReactionButton(
    reaction: Reaction,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
