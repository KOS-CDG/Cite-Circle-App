package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * A single skeleton bone.
 *
 * Loading placeholders conventionally use a sweeping gradient shimmer. Gradients are out, so
 * this pulses opacity on a flat fill instead — same "something is coming" signal, one colour.
 */
@Composable
fun SkeletonBone(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton-alpha"
    )
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

/**
 * Placeholder for a feed post. Mirrors the real card's geometry so the swap does not shift
 * anything on screen.
 */
@Composable
fun PostCardSkeleton(modifier: Modifier = Modifier) {
    // Read outside the semantics lambda: stringResource is @Composable, that lambda is not.
    val label = stringResource(R.string.cd_loading_post)
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .semantics { contentDescription = label }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBone(Modifier.size(40.dp), CircleShape)
            Spacer(Modifier.width(12.dp))
            Column {
                SkeletonBone(Modifier.width(140.dp).height(14.dp))
                Spacer(Modifier.height(6.dp))
                SkeletonBone(Modifier.width(96.dp).height(11.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        SkeletonBone(Modifier.fillMaxWidth().height(12.dp))
        Spacer(Modifier.height(8.dp))
        SkeletonBone(Modifier.fillMaxWidth().height(12.dp))
        Spacer(Modifier.height(8.dp))
        SkeletonBone(Modifier.fillMaxWidth(0.6f).height(12.dp))
        Spacer(Modifier.height(16.dp))
        SkeletonBone(Modifier.fillMaxWidth().height(88.dp), MaterialTheme.shapes.small)
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(5) { SkeletonBone(Modifier.size(22.dp), MaterialTheme.shapes.extraSmall) }
        }
    }
}

/** Placeholder for a saved-entry row in the reading list. */
@Composable
fun ListRowSkeleton(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.cd_loading_entry)
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .semantics { contentDescription = label }
    ) {
        SkeletonBone(Modifier.fillMaxWidth(0.8f).height(15.dp))
        Spacer(Modifier.height(8.dp))
        SkeletonBone(Modifier.fillMaxWidth(0.45f).height(11.dp))
        Spacer(Modifier.height(14.dp))
        SkeletonBone(Modifier.fillMaxWidth().height(11.dp))
        Spacer(Modifier.height(6.dp))
        SkeletonBone(Modifier.fillMaxWidth(0.7f).height(11.dp))
    }
}

/** Placeholder for a comment in a discussion thread. */
@Composable
fun CommentSkeleton(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.cd_loading_comment)
    Row(modifier.fillMaxWidth().semantics { contentDescription = label }) {
        SkeletonBone(Modifier.size(32.dp), CircleShape)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            SkeletonBone(Modifier.width(120.dp).height(12.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBone(Modifier.fillMaxWidth().height(11.dp))
            Spacer(Modifier.height(6.dp))
            SkeletonBone(Modifier.fillMaxWidth(0.55f).height(11.dp))
        }
    }
}
