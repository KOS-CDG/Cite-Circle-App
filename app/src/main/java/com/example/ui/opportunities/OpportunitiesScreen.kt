package com.example.ui.opportunities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Spacing
import com.example.ui.theme.citation
import com.example.ui.theme.eyebrowTight

data class Opportunity(
    val id: String,
    val type: String, // Grant, Academic Job, Call for Papers
    val title: String,
    val institution: String,
    val deadline: String,
    val description: String,
)

val sampleOpportunities = listOf(
    Opportunity(
        "1", "Grant", "Early Career Researcher Fellowship", "NSF",
        "Deadline: Nov 15, 2026",
        "Funding for innovative theoretical physics research.",
    ),
    Opportunity(
        "2", "Academic Job", "Assistant Professor in Computational Linguistics",
        "Stanford University", "Review begins: Oct 1, 2026",
        "Seeking candidates with strong publication records in LLM semantics.",
    ),
    Opportunity(
        "3", "Call for Papers", "Special Issue: Entropy in Distributed Systems",
        "Journal of Archival Science", "Submission: Dec 10, 2026",
        "We invite papers exploring localized data cluster architectures.",
    ),
)

private val filters = listOf("All", "Grant", "Academic Job", "Call for Papers")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunitiesScreen(modifier: Modifier = Modifier) {
    var selectedFilter by remember { mutableStateOf("All") }

    val filtered = if (selectedFilter == "All") {
        sampleOpportunities
    } else {
        sampleOpportunities.filter { it.type == selectedFilter }
    }

    // modifier param so LibraryScreen can pass weight(1f); see FieldsScreen for why.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScrollableTabRow(
            selectedTabIndex = filters.indexOf(selectedFilter).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = Spacing.screenHorizontal,
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            },
        ) {
            filters.forEach { filter ->
                Tab(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    text = { Text(filter, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.feedGutter),
        ) {
            items(filtered, key = { it.id }) { opp ->
                OpportunityCard(opp)
            }
        }
    }
}

/** Each opportunity type gets its own colour so the list is scannable at a glance. */
@Composable
private fun typeColors(type: String): Pair<Color, Color> = when (type) {
    "Grant" -> MaterialTheme.colorScheme.tertiaryContainer to
        MaterialTheme.colorScheme.onTertiaryContainer
    "Academic Job" -> MaterialTheme.colorScheme.primaryContainer to
        MaterialTheme.colorScheme.onPrimaryContainer
    else -> MaterialTheme.colorScheme.secondaryContainer to
        MaterialTheme.colorScheme.onSecondaryContainer
}

@Composable
fun OpportunityCard(opportunity: Opportunity) {
    val (chipBg, chipFg) = typeColors(opportunity.type)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    opportunity.type.uppercase(),
                    style = MaterialTheme.typography.eyebrowTight,
                    color = chipFg,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(chipBg)
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                )
                Text(
                    opportunity.deadline,
                    style = MaterialTheme.typography.citation,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                opportunity.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                opportunity.institution,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                opportunity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.base))
            OutlinedButton(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.touchTarget),
                shape = MaterialTheme.shapes.small,
            ) {
                Text("View details", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
