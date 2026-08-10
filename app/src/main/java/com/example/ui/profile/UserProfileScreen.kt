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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.ui.components.CitationChart
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.ErrorState
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.PaperCard
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionDivider
import com.example.ui.components.SectionLabel

/**
 * Another researcher's profile, reached by tapping the author on a paper card.
 *
 * The argument is a paper id: the author is derived from the paper, because papers are the only
 * real records the app holds and there is no separate researcher directory yet.
 */
@Composable
fun UserProfileScreen(
    paperId: String?,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onViewPaper: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val papers by viewModel.savedPapers.collectAsStateWithLifecycle()
    val subject = papers.firstOrNull { it.id == paperId }
    var isFollowing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(title = "RESEARCHER", onBack = onBack)

        if (subject == null) {
            ErrorState(
                title = "Researcher Not Found",
                message = "This profile is no longer available.",
                actionLabel = "GO BACK",
                onAction = onBack,
            )
            return@Column
        }

        val authoredPapers = papers.filter { it.authorName == subject.authorName }

        LazyColumn(contentPadding = PaddingValues(CiteCircleDefaults.ScreenPadding)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InitialsAvatar(subject.authorInitials, size = 64.dp, fontSize = 22.sp)
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            subject.authorName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            subject.affiliation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (isFollowing) {
                    OutlinedButton(
                        onClick = { isFollowing = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CiteCircleDefaults.ButtonShape,
                        border = CiteCircleDefaults.cardBorder(alpha = 0.2f),
                    ) {
                        Text(
                            "FOLLOWING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Button(
                        onClick = { isFollowing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CiteCircleDefaults.ButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            "FOLLOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }

                SectionDivider()

                CitationChart()

                SectionDivider()

                SectionLabel("PUBLICATIONS")
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(authoredPapers.size) { index ->
                val paper = authoredPapers[index]
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
