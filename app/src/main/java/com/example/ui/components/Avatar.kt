package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Gradients
import com.example.ui.theme.PresenceOnline
import com.example.ui.theme.Spacing

/**
 * Avatars were re-implemented inline at four different sizes across the app, always as
 * Box + CircleShape + solid primary + initials.
 *
 * The gradient fill is derived deterministically from the seed (a stable user id), so the same
 * person is always the same colour and a list of people looks varied without any image assets.
 */
@Composable
fun Avatar(
    initials: String,
    seed: String,
    modifier: Modifier = Modifier,
    size: Dp = Spacing.avatarMd,
    showPresence: Boolean = false,
    isOnline: Boolean = false,
    ring: Boolean = false,
) {
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .then(
                    if (ring) {
                        Modifier
                            .border(2.dp, Gradients.presenceRing(), CircleShape)
                            .padding(3.dp)
                    } else {
                        Modifier
                    },
                )
                .clip(CircleShape)
                .background(Gradients.avatarFallback(seed)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials,
                color = Color.White,
                style = if (size >= Spacing.avatarLg) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
            )
        }

        if (showPresence && isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.28f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(PresenceOnline),
            )
        }
    }
}
