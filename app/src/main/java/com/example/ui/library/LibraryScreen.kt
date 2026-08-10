package com.example.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.data.lists.ReadingListRepository
import com.example.ui.lists.ReadingListsScreen
import com.example.ui.opportunities.OpportunitiesScreen

private val tabs = listOf("Reading Lists", "Opportunities")

/**
 * Merges what were two separate bottom-nav tabs. Both are list screens over static sample data and
 * neither is a daily-use destination, so pairing them costs nothing and buys a nav slot.
 */
@Composable
fun LibraryScreen(readingListRepository: ReadingListRepository) {
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
            0 -> ReadingListsScreen(
                repository = readingListRepository,
                modifier = Modifier.weight(1f),
            )
            else -> OpportunitiesScreen(modifier = Modifier.weight(1f))
        }
    }
}
