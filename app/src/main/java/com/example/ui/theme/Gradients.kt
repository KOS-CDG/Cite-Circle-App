package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradient brushes. The previous theme contained none at all -- a grep for `Brush` across the
 * whole codebase returned nothing -- which is a large part of why it felt flat.
 *
 * These read from MaterialTheme.colorScheme rather than hardcoding hex, so they flip with the
 * light/dark toggle for free.
 *
 * RULES, because "bold multi-color" is exactly how a UI becomes unreadable:
 *  - Gradients go on fills and chrome only: banners, bubbles, buttons, avatar fallbacks, rails.
 *  - Never behind long-form text. Post bodies and citation text sit on flat surface tiers.
 *  - Gradient *text* is allowed in exactly one place, the wordmark, via TextStyle(brush = ...).
 *  - Two stops, except storyRing.
 */
object Gradients {

    /** App header and auth screen. Indigo -> raspberry, on the diagonal. */
    @Composable
    @ReadOnlyComposable
    fun brandHeader(): Brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
        ),
        start = Offset.Zero,
        end = Offset.Infinite,
    )

    /**
     * The citation panel on a post card -- the strongest visual moment in the app. Replaces the
     * flat `primary` fill it used to have. Text on top uses onPrimary.
     */
    @Composable
    @ReadOnlyComposable
    fun citationPanel(): Brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
        ),
        start = Offset.Zero,
        end = Offset.Infinite,
    )

    /**
     * The user's own message bubbles.
     *
     * Note this is a per-bubble gradient. Messenger itself paints one gradient across the entire
     * thread and has each bubble sample its own slice, which needs onGloballyPositioned feeding
     * each bubble its Y offset into a shared brush -- fiddly and jank-prone. Per-bubble gets
     * ~90% of the effect for ~10% of the risk. Revisit as polish if it ever matters.
     */
    @Composable
    @ReadOnlyComposable
    fun outgoingBubble(): Brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
        ),
    )

    /** Field-tinted banner for person cards, reading-list headers and profile covers. */
    @Composable
    @ReadOnlyComposable
    fun fieldBanner(field: AcademicField): Brush {
        val accent = LocalFieldAccents.current.getValue(field)
        return Brush.linearGradient(
            colors = listOf(accent.base, accent.container),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    }

    /** Deterministic two-stop fill behind monogram avatars, keyed off a stable user id. */
    @Composable
    @ReadOnlyComposable
    fun avatarFallback(seed: String): Brush {
        val fields = AcademicField.entries
        // floorMod, not abs(..) % n: abs(Int.MIN_VALUE) is still Int.MIN_VALUE, so a seed that
        // happened to hash to it would produce a negative index and throw.
        val index = Math.floorMod(seed.hashCode(), fields.size)
        val accent = LocalFieldAccents.current.getValue(fields[index])
        return Brush.linearGradient(
            colors = listOf(accent.base, accent.onContainer),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
    }

    /** Ring around "active now" avatars in the messenger. */
    @Composable
    @ReadOnlyComposable
    fun presenceRing(): Brush = Brush.sweepGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primary,
        ),
    )

    /** Bottom scrim over post images so overlaid text stays legible. */
    @Composable
    @ReadOnlyComposable
    fun imageScrim(): Brush = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Scrim.copy(alpha = 0.55f)),
    )

    /** Loading placeholder sweep. `progress` is expected to run 0f..1f from an infinite transition. */
    @Composable
    @ReadOnlyComposable
    fun shimmer(progress: Float): Brush {
        val base = MaterialTheme.colorScheme.surfaceContainerHigh
        val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
        return Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(progress * 1000f - 400f, 0f),
            end = Offset(progress * 1000f, 0f),
        )
    }
}
