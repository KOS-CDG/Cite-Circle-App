package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii, wired into MaterialTheme so components pick them up by role instead of each
 * call site hardcoding a number. The previous UI used 2dp and 4dp corners — a deliberately
 * squared, printed look. Social products round considerably harder, and the softer corner is
 * most of what separates a "card" from a "clipping".
 *
 * - [Shapes.extraSmall] chips, badges, skeleton bones
 * - [Shapes.small] buttons, text fields, inline images
 * - [Shapes.medium] cards, inset panels
 * - [Shapes.large] sheets and dialogs
 */
val AppShapes = Shapes(
  extraSmall = RoundedCornerShape(4.dp),
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(12.dp),
  large = RoundedCornerShape(16.dp),
  extraLarge = RoundedCornerShape(24.dp)
)
