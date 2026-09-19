package com.example.ui.opportunities

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.HomeViewModel
import com.example.ui.components.EmptyState
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerLight
import com.example.ui.theme.PageNeutral
import com.example.ui.theme.SurfaceInset
import com.example.ui.theme.SurfaceWhite

enum class OpportunityCategory(val label: String) {
    ALL("All Opportunities"),
    CFP("Calls for Papers"),
    GRANT("Grants & Funding"),
    FELLOWSHIP("Fellowships & Postdoc")
}

data class AcademicOpportunity(
    val id: String,
    val title: String,
    val organization: String,
    val category: OpportunityCategory,
    val deadline: String,
    val fundingOrTrack: String,
    val description: String,
    val tags: List<String>,
    val url: String
)

private val CuratedOpportunities = listOf(
    AcademicOpportunity(
        id = "opp_1",
        title = "NeurIPS 2025: Annual Conference on Neural Information Processing Systems",
        organization = "Neural Information Processing Systems Foundation",
        category = OpportunityCategory.CFP,
        deadline = "May 22, 2025",
        fundingOrTrack = "Main Conference & Workshop Tracks",
        description = "Inviting full papers on theoretical foundations of machine learning, deep neural network optimization, multimodal reasoning, reinforcement learning, and ethical AI safeguards.",
        tags = listOf("Machine Learning", "Deep Learning", "Peer Review", "NeurIPS"),
        url = "https://neurips.cc/Conferences/2025/CallForPapers"
    ),
    AcademicOpportunity(
        id = "opp_2",
        title = "NSF Computer and Information Science Research Initiation Initiative (CRII)",
        organization = "National Science Foundation (NSF)",
        category = OpportunityCategory.GRANT,
        deadline = "September 17, 2025",
        fundingOrTrack = "\$175,000 / 2 Years (Single PI)",
        description = "Supports early-career academicians who lack essential research resources. Encourages independent research trajectories in scalable computing, algorithmic fairness, and data-intensive systems.",
        tags = listOf("NSF", "Early Career", "Research Grant", "Funding"),
        url = "https://www.nsf.gov/funding/pgm_summ.jsp?pims_id=504828"
    ),
    AcademicOpportunity(
        id = "opp_3",
        title = "Simons Postdoctoral Fellowship in Theoretical Computer Science & AI",
        organization = "Simons Institute for the Theory of Computing, UC Berkeley",
        category = OpportunityCategory.FELLOWSHIP,
        deadline = "December 1, 2025",
        fundingOrTrack = "\$95,000 / Year + \$15,000 Travel Allowance",
        description = "Two-year postdoctoral appointment focused on foundational aspects of artificial intelligence, computational complexity, probabilistic inference, and quantum computing.",
        tags = listOf("Postdoc", "UC Berkeley", "Algorithms", "Simons"),
        url = "https://simons.berkeley.edu/fellowships"
    ),
    AcademicOpportunity(
        id = "opp_4",
        title = "Nature Machine Intelligence: Special Issue on Foundation Model Interpretability",
        organization = "Springer Nature Publishing",
        category = OpportunityCategory.CFP,
        deadline = "July 31, 2025",
        fundingOrTrack = "Peer-Reviewed Journal Special Issue",
        description = "Call for original research papers uncovering internal mechanisms of large language models, mechanistic interpretability, attribution graphs, and scientific reproducibility.",
        tags = listOf("Nature", "Interpretability", "Open Access", "High Impact"),
        url = "https://www.nature.com/natmachintell/"
    ),
    AcademicOpportunity(
        id = "opp_5",
        title = "DARPA Young Faculty Award (YFA) in Intelligent Autonomous Systems",
        organization = "Defense Advanced Research Projects Agency (DARPA)",
        category = OpportunityCategory.GRANT,
        deadline = "November 5, 2025",
        fundingOrTrack = "Up to \$500,000 / 24 Months",
        description = "Identifies rising research stars in junior faculty positions. Focuses on transformative ideas bridging symbolic logic, neural representation learning, and high-assurance distributed systems.",
        tags = listOf("DARPA", "Faculty Award", "Autonomous Systems", "Government"),
        url = "https://www.darpa.mil/work-with-us/for-universities/young-faculty-award"
    ),
    AcademicOpportunity(
        id = "opp_6",
        title = "ICLR 2026: International Conference on Learning Representations",
        organization = "ICLR Foundation",
        category = OpportunityCategory.CFP,
        deadline = "September 28, 2025",
        fundingOrTrack = "Oral & Poster Tracks with OpenReview",
        description = "Premier gathering dedicated to advancing representation learning. Emphasizes open review discussions, rebuttal transparency, and reproducible benchmark evaluations.",
        tags = listOf("ICLR", "Representation Learning", "OpenReview", "Conference"),
        url = "https://iclr.cc/"
    )
)

/**
 * Academic Opportunities, Grants & Calls for Papers Hub.
 *
 * Full-featured screen for discovering grant deadlines, conference submission dates,
 * postdoctoral fellowships, and peer-reviewed journals.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OpportunitiesScreen(
    viewModel: HomeViewModel? = null,
    navController: NavController? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(OpportunityCategory.ALL) }
    val savedOpportunities = remember { mutableStateMapOf<String, Boolean>() }

    val filteredList = remember(searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        CuratedOpportunities.filter { opp ->
            val matchesCategory = selectedCategory == OpportunityCategory.ALL || opp.category == selectedCategory
            val matchesQuery = q.isEmpty() ||
                opp.title.lowercase().contains(q) ||
                opp.organization.lowercase().contains(q) ||
                opp.description.lowercase().contains(q) ||
                opp.tags.any { it.lowercase().contains(q) }
            matchesCategory && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageNeutral)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (navController != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Text(
                        text = "Opportunities & Grants",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = if (navController == null) 8.dp else 0.dp)
                    )
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = {
                        Text(
                            "Search grants, conferences, or funders...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = DividerLight,
                        focusedContainerColor = SurfaceInset,
                        unfocusedContainerColor = SurfaceInset
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                // Category Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(OpportunityCategory.entries) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) BrandBlue else SurfaceInset)
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                category.label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) SurfaceWhite else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
            }
        }

        // Listings Stream
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "No Opportunities Found",
                    message = "No calls for papers or grants match \"$searchQuery\". Try adjusting your search query or category filter.",
                    icon = Icons.Outlined.WorkOutline,
                    actionLabel = "Clear Filter",
                    onAction = {
                        searchQuery = ""
                        selectedCategory = OpportunityCategory.ALL
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filteredList, key = { it.id }) { opp ->
                    val isSaved = savedOpportunities[opp.id] == true
                    OpportunityCard(
                        opportunity = opp,
                        isSaved = isSaved,
                        onToggleSave = {
                            val next = !isSaved
                            savedOpportunities[opp.id] = next
                            viewModel?.report(
                                if (next) "Opportunity saved to vault"
                                else "Opportunity removed from vault"
                            )
                        },
                        onCopyLink = {
                            clipboardManager.setText(AnnotatedString("${opp.title}\n${opp.url}\nDeadline: ${opp.deadline}"))
                            viewModel?.report("Opportunity link & details copied to clipboard")
                        },
                        onDiscuss = {
                            navController?.navigate("messenger")
                        },
                        onOpenUrl = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(opp.url))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                viewModel?.report("Could not open external browser for: ${opp.url}")
                            }
                        }
                    )
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(PageNeutral)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpportunityCard(
    opportunity: AcademicOpportunity,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    onCopyLink: () -> Unit,
    onDiscuss: () -> Unit,
    onOpenUrl: () -> Unit
) {
    val categoryBadgeColor = when (opportunity.category) {
        OpportunityCategory.CFP -> BrandBlue
        OpportunityCategory.GRANT -> AccentGreen
        OpportunityCategory.FELLOWSHIP -> Color(0xFF6A0DAD)
        OpportunityCategory.ALL -> BrandBlue
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Category + Deadline Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(categoryBadgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    opportunity.category.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    color = categoryBadgeColor
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Event,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    opportunity.deadline,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Title
        Text(
            text = opportunity.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(4.dp))

        // Organization
        Text(
            text = opportunity.organization,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = BrandBlue
        )

        Spacer(Modifier.height(6.dp))

        // Scope / Funding highlight
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceInset)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Track / Funding: ",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = opportunity.fundingOrTrack,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(8.dp))

        // Description
        Text(
            text = opportunity.description,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(10.dp))

        // Tags
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            opportunity.tags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceInset)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "#$tag",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
        Spacer(Modifier.height(10.dp))

        // Actions: Save to Vault | Copy Info | Discuss | Official Call
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save button
            OutlinedButton(
                onClick = onToggleSave,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(
                    if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSaved) BrandBlue else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isSaved) "Saved" else "Save",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Copy Link button
            OutlinedButton(
                onClick = onCopyLink,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(4.dp))
                Text("Copy", style = MaterialTheme.typography.labelSmall)
            }

            // Discuss button
            OutlinedButton(
                onClick = onDiscuss,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(
                    Icons.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(4.dp))
                Text("Discuss", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.weight(1f))

            // Apply / View Call button
            Button(
                onClick = onOpenUrl,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = SurfaceWhite
                )
            ) {
                Text(
                    "View Call",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = SurfaceWhite
                )
            }
        }
    }
}
