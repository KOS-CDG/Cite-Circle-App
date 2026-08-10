package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shown when a list has no content. Extracted from `MainActivity` so every screen can use it —
 * it previously existed but was only wired into the fields list.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = CiteCircleDefaults.hairlineColor(alpha = 0.2f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = CiteCircleDefaults.hairlineColor(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onAction,
                shape = CiteCircleDefaults.ButtonShape,
                border = CiteCircleDefaults.cardBorder(alpha = 0.2f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

/** Shown when a lookup fails — most often a navigation argument that resolves to nothing. */
@Composable
fun ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal,
            ),
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = CiteCircleDefaults.hairlineColor(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onAction,
                shape = CiteCircleDefaults.ButtonShape,
                border = CiteCircleDefaults.cardBorder(alpha = 0.2f),
            ) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** A single pulsing placeholder bar. */
@Composable
private fun SkeletonBar(widthFraction: Float, height: Int, alpha: Float) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height.dp)
            .clip(RoundedCornerShape(2.dp))
            .alpha(alpha)
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)),
    )
}

/** Card-shaped skeleton standing in for a paper or list item while content settles. */
@Composable
fun LoadingCardPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CiteCircleDefaults.CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
    ) {
        SkeletonBar(widthFraction = 0.5f, height = 12, alpha = alpha)
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonBar(widthFraction = 1f, height = 10, alpha = alpha)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBar(widthFraction = 0.9f, height = 10, alpha = alpha)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBar(widthFraction = 0.7f, height = 10, alpha = alpha)
        Spacer(modifier = Modifier.height(20.dp))
        SkeletonBar(widthFraction = 1f, height = 48, alpha = alpha)
    }
}

/** Vertical stack of [LoadingCardPlaceholder]s matching the feed's rhythm. */
@Composable
fun LoadingList(modifier: Modifier = Modifier, itemCount: Int = 3) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CiteCircleDefaults.ScreenPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        repeat(itemCount) { LoadingCardPlaceholder() }
    }
}
