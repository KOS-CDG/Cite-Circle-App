package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The handful of values the editorial design language repeats everywhere: 4.dp card corners,
 * 2.dp button corners, hairline borders at 10% of `onBackground`, and 24.dp screen padding.
 */
object CiteCircleDefaults {

    val CardShape = RoundedCornerShape(4.dp)
    val ButtonShape = RoundedCornerShape(2.dp)
    val ScreenPadding = 24.dp

    @Composable
    fun hairlineColor(alpha: Float = 0.1f): Color =
        MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)

    @Composable
    fun cardBorder(alpha: Float = 0.1f): BorderStroke = BorderStroke(1.dp, hairlineColor(alpha))
}

/** All-caps, wide-tracked section label — the app's standard heading for a block of content. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
        ),
        color = color,
    )
}

/** Circular avatar showing a researcher's initials. */
@Composable
fun InitialsAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    fontSize: TextUnit = 10.sp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
        )
    }
}

/** Italic pull-quote with a coloured rule down the left edge, used for citation context. */
@Composable
fun QuoteBlock(quote: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(CiteCircleDefaults.cardBorder(alpha = 0.05f)),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.tertiary)
                .align(Alignment.CenterStart),
        )
        Text(
            quote,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                lineHeight = 24.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 14.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

/**
 * Header for a full-screen destination: back affordance, all-caps title, optional trailing
 * actions, and the hairline rule that separates it from the content below.
 */
@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = CiteCircleDefaults.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            @Suppress("DEPRECATION")
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            SectionLabel(title, modifier = Modifier.weight(1f))
            actions()
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = CiteCircleDefaults.hairlineColor(), thickness = 1.dp)
    }
}

/** Label/value pair used across the detail screens. */
@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel(label)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Divider with the app's standard 24.dp breathing room above and below. */
@Composable
fun SectionDivider(modifier: Modifier = Modifier, spacing: Dp = 24.dp) {
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Spacer(modifier = Modifier.height(spacing))
        HorizontalDivider(color = CiteCircleDefaults.hairlineColor())
        Spacer(modifier = Modifier.height(spacing))
    }
}
