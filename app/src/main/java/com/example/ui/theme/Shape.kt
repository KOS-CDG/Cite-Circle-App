package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The previous theme never passed a Shapes instance to MaterialTheme at all, which is why every
 * call site hardcoded RoundedCornerShape(4.dp) for cards and 2.dp for buttons. Those near-square
 * corners were the strongest "printed journal" signal in the whole app; this scale is the single
 * cheapest change that makes it feel like a modern social product.
 */
val CiteCircleShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // was 2.dp -- buttons
    small = RoundedCornerShape(12.dp),       // chips, small buttons
    medium = RoundedCornerShape(16.dp),      // input fields, tiles
    large = RoundedCornerShape(20.dp),       // was 4.dp -- feed cards
    extraLarge = RoundedCornerShape(28.dp),  // sheets, FAB, composer pill
)

/**
 * Messenger bubble corners. Consecutive messages from the same sender collapse into a run, and
 * only the last bubble in a run gets the full-radius "tail" corner -- that asymmetry is what makes
 * a thread read as grouped conversation rather than a list of separate boxes.
 */
object BubbleShapes {
    private val Round = 18.dp
    private val Tail = 4.dp

    fun outgoing(isFirstInGroup: Boolean, isLastInGroup: Boolean) = RoundedCornerShape(
        topStart = Round,
        topEnd = if (isFirstInGroup) Round else Tail,
        bottomEnd = if (isLastInGroup) Round else Tail,
        bottomStart = Round,
    )

    fun incoming(isFirstInGroup: Boolean, isLastInGroup: Boolean) = RoundedCornerShape(
        topStart = if (isFirstInGroup) Round else Tail,
        topEnd = Round,
        bottomEnd = Round,
        bottomStart = if (isLastInGroup) Round else Tail,
    )

    val single = RoundedCornerShape(Round)
}
