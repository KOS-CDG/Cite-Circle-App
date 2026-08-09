package com.example.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing tokens. A plain object rather than a CompositionLocal: spacing does not vary by theme,
 * so the indirection would buy nothing.
 *
 * Note `screenHorizontal` is 16.dp, down from the 24.dp the app used everywhere. The old 24.dp
 * rhythm (plus 24.dp gaps between feed items) is a large part of what made this read as a print
 * journal; a Facebook-like feed needs 16.dp gutters and an 8.dp gap between cards so that cards
 * feel like a stream rather than a series of posters.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val base = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Horizontal padding for full-screen content. */
    val screenHorizontal = 16.dp

    /** Interior padding of a feed card. */
    val cardPadding = 16.dp

    /** Vertical gap between consecutive feed cards. */
    val feedGutter = 8.dp

    val avatarSm = 32.dp
    val avatarMd = 44.dp
    val avatarLg = 64.dp
    val avatarXl = 96.dp

    /** Minimum interactive size, per Material accessibility guidance. */
    val touchTarget = 48.dp

    val hairline = 1.dp
}
