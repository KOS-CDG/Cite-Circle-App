package com.example.ui.discover

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.fields.FieldsScreen
import com.example.ui.theme.AcademicField
import com.example.ui.people.PeopleScreen
import com.example.ui.people.PeopleViewModel

private val tabs = listOf("Fields", "People")

/**
 * Fields used to be its own bottom-nav tab. It now shares Discover with People, which frees a
 * bottom-nav slot for Messages without pushing the bar to six tabs.
 *
 * The People tab hosts researcher discovery and the connections graph. Its "Message" action
 * opens (or creates) a conversation and deep-links straight into the thread, which is what ties
 * connections to the messenger rather than leaving them as two unrelated features.
 */
@Composable
fun DiscoverScreen(
    peopleViewModel: PeopleViewModel,
    onOpenThread: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    // Set when a field row is tapped; the People tab opens pre-filtered to that discipline.
    // Tapping a field used to do nothing at all, behind a chevron that promised navigation.
    var fieldFilter by remember { mutableStateOf<AcademicField?>(null) }

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
                    onClick = {
                        selectedTab = index
                        // Leaving People by hand clears the filter, so the tab does not silently
                        // stay narrowed to a field the user has forgotten they picked.
                        if (index == 0) fieldFilter = null
                    },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }

        when (selectedTab) {
            0 -> FieldsScreen(
                onOpenField = { field ->
                    fieldFilter = field
                    selectedTab = 1
                },
                modifier = Modifier.weight(1f),
            )

            else -> PeopleScreen(
                viewModel = peopleViewModel,
                onMessage = { userId ->
                    peopleViewModel.openConversation(userId, onOpenThread)
                },
                onOpenProfile = onOpenProfile,
                fieldFilter = fieldFilter,
                onClearFieldFilter = { fieldFilter = null },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
