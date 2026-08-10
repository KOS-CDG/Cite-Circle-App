package com.example.ui.search

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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.data.SampleData
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.EmptyState
import com.example.ui.lists.editorFieldColors

/** One row in the unified result list, tagged with where it came from. */
private data class SearchResult(
    val id: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val onOpen: () -> Unit,
)

@Composable
fun SearchScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onOpenPaper: (String) -> Unit,
    onOpenField: (String) -> Unit,
    onOpenList: (String) -> Unit,
    onOpenOpportunity: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val papers by viewModel.savedPapers.collectAsStateWithLifecycle()

    val results = remember(query, papers) {
        if (query.isBlank()) {
            emptyList()
        } else {
            buildList {
                papers.filter {
                    it.content.contains(query, true) ||
                        it.authorName.contains(query, true) ||
                        it.citation.contains(query, true)
                }.forEach { paper ->
                    add(
                        SearchResult(
                            id = "paper-${paper.id}",
                            category = "PAPER",
                            title = paper.content.take(60).trim() + "…",
                            subtitle = paper.authorName,
                            onOpen = { onOpenPaper(paper.id) },
                        ),
                    )
                }

                SampleData.fields.filter {
                    it.name.contains(query, true) || it.description.contains(query, true)
                }.forEach { field ->
                    add(
                        SearchResult(
                            id = "field-${field.id}",
                            category = "FIELD",
                            title = field.name,
                            subtitle = "${field.researcherCount} active researchers",
                            onOpen = { onOpenField(field.id) },
                        ),
                    )
                }

                SampleData.readingLists.filter {
                    it.title.contains(query, true) || it.description.contains(query, true)
                }.forEach { folder ->
                    add(
                        SearchResult(
                            id = "list-${folder.id}",
                            category = "READING LIST",
                            title = folder.title,
                            subtitle = "${folder.paperCount} papers",
                            onOpen = { onOpenList(folder.id) },
                        ),
                    )
                }

                SampleData.opportunities.filter {
                    it.title.contains(query, true) ||
                        it.institution.contains(query, true) ||
                        it.description.contains(query, true)
                }.forEach { opportunity ->
                    add(
                        SearchResult(
                            id = "opp-${opportunity.id}",
                            category = opportunity.type.uppercase(),
                            title = opportunity.title,
                            subtitle = opportunity.institution,
                            onOpen = { onOpenOpportunity(opportunity.id) },
                        ),
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CiteCircleDefaults.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Search papers, fields, lists…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = CiteCircleDefaults.CardShape,
                colors = editorFieldColors(),
            )
            Text(
                "CANCEL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClickLabel = "Close search", onClick = onBack)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
            )
        }

        when {
            query.isBlank() -> EmptyState(
                title = "Search the Registry",
                message = "Look across your papers, academic fields, reading lists and open " +
                    "opportunities.",
                icon = Icons.Outlined.Search,
            )

            results.isEmpty() -> EmptyState(
                title = "No Matches",
                message = "Nothing in the registry matches \"$query\".",
                icon = Icons.Outlined.SearchOff,
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = CiteCircleDefaults.ScreenPadding,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results, key = { it.id }) { result ->
                    SearchResultRow(result)
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CiteCircleDefaults.CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = "Open ${result.title}", onClick = result.onOpen)
            .padding(16.dp),
    ) {
        Text(
            result.category,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            result.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            result.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
