package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Gradients
import com.example.ui.theme.Spacing

/**
 * A single shimmer sweep shared by every placeholder on screen.
 *
 * Hoisted rather than created per-block on purpose: an infinite transition inside each placeholder
 * gives every block its own phase, so a column of them ripples independently and reads as noise.
 * One brush means one sweep travelling across the whole screen.
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_200, easing = LinearEasing)),
        label = "shimmerProgress",
    )
    return Gradients.shimmer(progress)
}

/** One shimmering block. Sized by the caller; every placeholder below is built out of these. */
@Composable
fun ShimmerBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(brush),
    )
}

@Composable
private fun ShimmerLine(brush: Brush, width: Float, height: Dp = 12.dp) {
    ShimmerBlock(
        brush = brush,
        modifier = Modifier
            .fillMaxWidth(width)
            .height(height),
        shape = MaterialTheme.shapes.extraSmall,
    )
}

/**
 * Stands in for a PostCard while the feed loads.
 *
 * Deliberately mirrors PostCard's real geometry -- same card shape, same accent edge, same avatar
 * size, an image block on some of them. A placeholder whose proportions do not match what replaces
 * it produces a visible jolt at swap time, which is worse than showing nothing.
 */
@Composable
fun PostCardSkeleton(brush: Brush, withImage: Boolean = false, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        ShimmerBlock(
            brush = brush,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
        )
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBlock(
                    brush = brush,
                    modifier = Modifier.size(Spacing.avatarMd),
                    shape = CircleShape,
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Column {
                    ShimmerLine(brush, width = 0.5f, height = 14.dp)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    ShimmerLine(brush, width = 0.3f, height = 10.dp)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            ShimmerLine(brush, width = 1f)
            Spacer(modifier = Modifier.height(Spacing.xs))
            ShimmerLine(brush, width = 1f)
            Spacer(modifier = Modifier.height(Spacing.xs))
            ShimmerLine(brush, width = 0.6f)

            if (withImage) {
                Spacer(modifier = Modifier.height(Spacing.md))
                ShimmerBlock(
                    brush = brush,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.base))
            ShimmerBlock(
                brush = brush,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = MaterialTheme.shapes.medium,
            )
        }
    }
}

// There is deliberately no ConversationRowSkeleton. The messenger repository is in-memory and
// seeded at construction, so its flows emit on the first frame -- a placeholder there would never
// be visible, and building one would mean either dead code or an artificial delay staged to show
// off an animation. The feed is different: Room is a real disk read.
