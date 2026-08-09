package com.example.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.components.EmptyState
import com.example.ui.fields.FieldsScreen

private val tabs = listOf("Fields", "People")

/**
 * Fields used to be its own bottom-nav tab. It now shares Discover with People, which frees a
 * bottom-nav slot for Messages without pushing the bar to six tabs.
 *
 * The People tab is deliberately an empty state rather than fake rows -- the connections graph
 * and the seeded researcher directory land in a later phase.
 */
@Composable
fun DiscoverScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }

        when (selectedTab) {
            0 -> FieldsScreen(modifier = Modifier.weight(1f))
            else -> Box(modifier = Modifier.weight(1f)) {
                EmptyState(
                    title = "People are coming",
                    message = "Researcher discovery and connections arrive with the messaging work.",
                    icon = Icons.Outlined.Groups,
                )
            }
        }
    }
}
