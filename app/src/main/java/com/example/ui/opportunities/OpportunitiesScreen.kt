package com.example.ui.opportunities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Opportunity
import com.example.data.SampleData
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.EmptyState

@Composable
fun OpportunitiesScreen(
    onOpenOpportunity: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = SampleData.opportunityFilters
    var selectedFilter by remember { mutableStateOf(filters.first()) }

    val filteredOpportunities = if (selectedFilter == filters.first()) {
        SampleData.opportunities
    } else {
        SampleData.opportunities.filter { it.type == selectedFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScrollableTabRow(
            selectedTabIndex = filters.indexOf(selectedFilter).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = CiteCircleDefaults.ScreenPadding,
            divider = {
                HorizontalDivider(color = CiteCircleDefaults.hairlineColor())
            },
        ) {
            filters.forEach { filter ->
                Tab(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    text = {
                        Text(
                            filter,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    },
                )
            }
        }

        if (filteredOpportunities.isEmpty()) {
            EmptyState(
                title = "Nothing Open",
                message = "There are no $selectedFilter listings right now. Try another category.",
                icon = Icons.Filled.BusinessCenter,
                actionLabel = "SHOW ALL",
                onAction = { selectedFilter = filters.first() },
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(CiteCircleDefaults.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(filteredOpportunities, key = { it.id }) { opportunity ->
                    OpportunityCard(
                        opportunity = opportunity,
                        onViewDetails = { onOpenOpportunity(opportunity.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun OpportunityCard(
    opportunity: Opportunity,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "View ${opportunity.title}",
                onClick = onViewDetails,
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = CiteCircleDefaults.CardShape,
        border = CiteCircleDefaults.cardBorder(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    opportunity.type.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    opportunity.deadline,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                opportunity.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                opportunity.institution,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                opportunity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onViewDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = CiteCircleDefaults.ButtonShape,
                border = CiteCircleDefaults.cardBorder(alpha = 0.2f),
            ) {
                Text(
                    "VIEW DETAILS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
