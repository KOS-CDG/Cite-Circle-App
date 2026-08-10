package com.example.ui.opportunities

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Opportunity(
    val id: String,
    val type: String, // Grant, Job, Call for Papers
    val title: String,
    val institution: String,
    val deadline: String,
    val description: String
)

val sampleOpportunities = listOf(
    Opportunity("1", "Grant", "Early Career Researcher Fellowship", "NSF", "Deadline: Nov 15, 2026", "Funding for innovative theoretical physics research."),
    Opportunity("2", "Academic Job", "Assistant Professor in Computational Linguistics", "Stanford University", "Review begins: Oct 1, 2026", "Seeking candidates with strong publication records in LLM semantics."),
    Opportunity("3", "Call for Papers", "Special Issue: Entropy in Distributed Systems", "Journal of Archival Science", "Submission: Dec 10, 2026", "We invite papers exploring localized data cluster architectures.")
)

val opportunityFilters = listOf("All", "Grant", "Academic Job", "Call for Papers")

/** Narrows [opportunities] to [filter], where "All" means no filtering. */
fun filterOpportunities(opportunities: List<Opportunity>, filter: String): List<Opportunity> =
    if (filter == "All") opportunities else opportunities.filter { it.type == filter }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunitiesScreen() {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = opportunityFilters

    val filteredOpportunities = filterOpportunities(sampleOpportunities, selectedFilter)

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Filter bar
        ScrollableTabRow(
            selectedTabIndex = filters.indexOf(selectedFilter),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 24.dp,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)) }
        ) {
            filters.forEachIndexed { index, filter ->
                Tab(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    text = { Text(filter, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)) }
                )
            }
        }
        
        LazyColumn(
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredOpportunities) { opp ->
                OpportunityCard(opp)
            }
        }
    }
}

@Composable
fun OpportunityCard(opportunity: Opportunity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    opportunity.type.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    opportunity.deadline,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(opportunity.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(opportunity.institution, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                opportunity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
            ) {
                Text("VIEW DETAILS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
