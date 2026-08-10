package com.example.ui.profile

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.ui.components.CitationChart
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.EmptyState
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.LoadingList
import com.example.ui.components.PaperCard
import com.example.ui.components.SectionLabel

@Composable
fun ProfileScreen(
    viewModel: HomeViewModel,
    onViewPaper: (String) -> Unit,
    onEditProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onComposePaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val papers by viewModel.savedPapers.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val identity = rememberResearcherIdentity()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(CiteCircleDefaults.ScreenPadding),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InitialsAvatar(identity.initials, size = 64.dp, fontSize = 22.sp)
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            identity.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            identity.email ?: identity.affiliation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onEditProfile) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit profile",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            CitationChart()
            Spacer(modifier = Modifier.height(32.dp))
            SectionLabel("PUBLICATIONS")
            Spacer(modifier = Modifier.height(16.dp))
        }

        when {
            isLoading -> item { LoadingList(itemCount = 2) }

            papers.isEmpty() -> item {
                EmptyState(
                    title = "No Publications",
                    message = "Papers you publish will be listed on your profile.",
                    icon = Icons.Outlined.BookmarkBorder,
                    actionLabel = "PUBLISH A PAPER",
                    onAction = onComposePaper,
                )
            }

            else -> items(papers, key = { it.id }) { paper ->
                PaperCard(
                    paper = paper,
                    onEndorse = { viewModel.toggleEndorsement(paper.id, paper.isEndorsed) },
                    onViewContext = { onViewPaper(paper.id) },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
