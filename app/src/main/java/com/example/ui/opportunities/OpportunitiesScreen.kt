package com.example.ui.opportunities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.components.EmptyState

/**
 * Grants, positions and calls for papers.
 *
 * This screen previously rendered three invented listings — an NSF fellowship, a Stanford
 * professorship, a journal special issue — behind a filter bar, with a "View details" button
 * that did nothing. Nothing in the app can source real listings; they would have to come from
 * a backend the project does not have.
 *
 * Rather than keep convincing fixtures, the screen says so. When a listings source exists,
 * this is where it goes.
 */
@Composable
fun OpportunitiesScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmptyState(
            title = "No opportunities yet",
            message = "Grants, positions and calls for papers will appear here once a " +
                "listings source is connected.",
            icon = Icons.Outlined.WorkOutline
        )
    }
}
