package com.example.ui.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.ui.components.CitationBlock
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.ErrorState
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.QuoteBlock
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionDivider
import com.example.ui.components.SectionLabel

/**
 * The destination behind "VIEW CONTEXT" and "VIEW FULL PAPER".
 *
 * Reads from [HomeViewModel.savedPapers] rather than introducing a view model of its own —
 * the feed already holds every persisted paper.
 */
@Composable
fun PaperDetailScreen(
    paperId: String?,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onViewAuthor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val papers by viewModel.savedPapers.collectAsStateWithLifecycle()
    val paper = papers.firstOrNull { it.id == paperId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(
            title = "PAPER",
            onBack = onBack,
            actions = {
                if (paper != null) {
                    IconButton(
                        onClick = { viewModel.toggleEndorsement(paper.id, paper.isEndorsed) },
                    ) {
                        Icon(
                            if (paper.isEndorsed) {
                                Icons.Filled.Verified
                            } else {
                                Icons.Outlined.Verified
                            },
                            contentDescription = if (paper.isEndorsed) {
                                "Withdraw endorsement"
                            } else {
                                "Endorse this paper"
                            },
                            tint = if (paper.isEndorsed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                        )
                    }
                }
            },
        )

        if (paper == null) {
            ErrorState(
                title = "Paper Not Found",
                message = "This paper is no longer in your registry.",
                actionLabel = "GO BACK",
                onAction = onBack,
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(CiteCircleDefaults.ScreenPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(paper.authorInitials, size = 40.dp, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        paper.authorName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${paper.affiliation} • ${paper.timeAgo}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "VIEW PROFILE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .clickable(
                            onClickLabel = "View author profile",
                            onClick = { onViewAuthor(paper.id) },
                        )
                        .padding(8.dp),
                )
            }

            SectionDivider()

            SectionLabel("ABSTRACT")
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                paper.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp,
            )

            SectionDivider()

            CitationBlock(paper.citation)

            SectionDivider()

            SectionLabel("CITATION CONTEXT", color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(16.dp))
            QuoteBlock(
                "\"...as proposed in this synthesis, the friction within localized data clusters " +
                    "mirrors the thermodynamic decay observed in early archival structures.\"",
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (paper.isEndorsed) {
                    "You have endorsed this paper."
                } else {
                    "You have not endorsed this paper yet."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(CiteCircleDefaults.ScreenPadding))
        }
    }
}
