package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Named styles for patterns the codebase was repeating by hand.
 *
 * There were 19 hand-written letterSpacing overrides across the app (10 at 2.sp, 9 at 1.sp),
 * nearly all of them the same all-caps "eyebrow" label -- e.g.
 * `labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold)` appears at
 * MainActivity.kt:70, 133, 202, 460, 628, 651, 665, 689, 712 alone.
 *
 * These are extension properties on Typography, so call sites read
 * `MaterialTheme.typography.eyebrow` with no extra plumbing.
 */

/** All-caps section label: "CITATION IMPACT", "PUBLICATIONS", "READING LISTS". */
val Typography.eyebrow: TextStyle
    get() = labelSmall.copy(
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold,
    )

/** Tighter eyebrow for dense rows (card headers, metadata strips). */
val Typography.eyebrowTight: TextStyle
    get() = eyebrow.copy(fontSize = 10.sp, letterSpacing = 1.sp)

/** The "Cite Circle" wordmark in the app header. */
val Typography.wordmark: TextStyle
    get() = headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)

/** Long-form post content. Slightly looser leading than bodyLarge for sustained reading. */
val Typography.postBody: TextStyle
    get() = bodyLarge.copy(lineHeight = 24.sp, letterSpacing = 0.1.sp)

/** Text inside a messenger bubble. */
val Typography.bubbleText: TextStyle
    get() = bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp)

/** Timestamps, affiliations, "3 comments" -- secondary metadata. */
val Typography.meta: TextStyle
    get() = bodySmall.copy(fontSize = 12.sp)

/** Counts and stats. Tabular figures so columns of numbers stay aligned. */
val Typography.numeric: TextStyle
    get() = labelMedium.copy(fontFeatureSettings = "tnum")

/** Citation strings and record IDs. */
val Typography.citation: TextStyle
    get() = bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 20.sp)

/** Empty-state and profile display names -- the serif face doing brand work. */
val Typography.displayTitle: TextStyle
    get() = headlineLarge.copy(fontWeight = FontWeight.Normal, fontStyle = FontStyle.Italic)
