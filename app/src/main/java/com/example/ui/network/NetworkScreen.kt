package com.example.ui.network

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.HomeViewModel
import com.example.MyApplication
import com.example.data.ConnectionReasons
import com.example.data.ConnectionStatus
import com.example.data.ScholarConnection
import com.example.ui.components.EmptyState
import com.example.ui.components.ListRowSkeleton
import com.example.ui.components.RefreshableBox
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerLight
import com.example.ui.theme.PageNeutral
import com.example.ui.theme.SurfaceInset
import com.example.ui.theme.SurfaceWhite

/**
 * Dedicated LinkedIn-style academic networking screen for Cite Circle.
 *
 * Provides:
 * - Invitations tray (Accept / Ignore received requests with reason and personal note preview).
 * - Suggested Scholars ("People You May Know" / Peers in your field or institution).
 * - "How are you gonna connect with them" interactive dialog with 5 academic relationship reasons
 *   and personal note template chips.
 * - "My Circle" tab with active connections and management options.
 */
@Composable
fun NetworkScreen(
    viewModel: HomeViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val sessionManager = app.sessionManager

    val currentUserSchool by sessionManager.currentUserAffiliation.collectAsStateWithLifecycle(initialValue = "")
    val currentUserField by sessionManager.currentUserField.collectAsStateWithLifecycle(initialValue = "")

    val suggestedState by viewModel.suggestedScholars.collectAsStateWithLifecycle()
    val pendingState by viewModel.pendingInvitations.collectAsStateWithLifecycle()
    val connectedState by viewModel.connectedScholars.collectAsStateWithLifecycle()
    val connectedCount by viewModel.connectedCount.collectAsStateWithLifecycle()
    val pendingInvitationCount by viewModel.pendingInvitationCount.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Grow Network, 1: My Circle
    var selectedFilterChip by rememberSaveable { mutableStateOf("All") }

    // Connect modal state
    var scholarToConnect by remember { mutableStateOf<ScholarConnection?>(null) }
    var scholarToRemove by remember { mutableStateOf<ScholarConnection?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageNeutral)
    ) {
        // -----------------------------------------------------------------
        // Header Bar
        // -----------------------------------------------------------------
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Network",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$connectedCount Connections" +
                                    if (pendingInvitationCount > 0) " · $pendingInvitationCount Pending" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            "Search scholars, school, or research field...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = SurfaceInset
                    )
                )

                // Tabs: Grow Network vs My Circle
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = BrandBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = BrandBlue,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Grow Network",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                                if (pendingInvitationCount > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(BrandBlue)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "$pendingInvitationCount",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "My Circle ($connectedCount)",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
            }
        }

        // -----------------------------------------------------------------
        // Tab Content
        // -----------------------------------------------------------------
        RefreshableBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (selectedTab == 0) {
                // Grow Network Tab
                GrowNetworkContent(
                    pendingInvitations = pendingState.items,
                    suggestedScholars = suggestedState.items,
                    isLoading = suggestedState.isLoading,
                    searchQuery = searchQuery,
                    selectedFilterChip = selectedFilterChip,
                    onFilterChipSelected = { selectedFilterChip = it },
                    currentUserSchool = currentUserSchool,
                    currentUserField = currentUserField,
                    onAccept = { viewModel.acceptConnection(it.id, it.name) },
                    onIgnore = { viewModel.ignoreConnection(it.id) },
                    onConnectClick = { scholarToConnect = it },
                    onWithdrawClick = { viewModel.ignoreConnection(it.id) }
                )
            } else {
                // My Circle Tab
                MyCircleContent(
                    connectedScholars = connectedState.items,
                    isLoading = connectedState.isLoading,
                    searchQuery = searchQuery,
                    onRemove = { scholarToRemove = it }
                )
            }
        }
    }

    // ---------------------------------------------------------------------
    // Connect with Scholar Modal Dialog
    // ---------------------------------------------------------------------
    scholarToConnect?.let { scholar ->
        ConnectDialog(
            scholar = scholar,
            onDismiss = { scholarToConnect = null },
            onSend = { note ->
                viewModel.sendConnectionRequest(scholarId = scholar.id, note = note)
                scholarToConnect = null
            }
        )
    }

    // ---------------------------------------------------------------------
    // Remove Connection Confirmation Dialog
    // ---------------------------------------------------------------------
    scholarToRemove?.let { scholar ->
        AlertDialog(
            onDismissRequest = { scholarToRemove = null },
            title = { Text("Remove Connection?") },
            text = { Text("Are you sure you want to remove ${scholar.name} from your academic circle?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeConnection(scholar.id, scholar.name)
                        scholarToRemove = null
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { scholarToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =========================================================================
// Grow Network Content
// =========================================================================

@Composable
private fun GrowNetworkContent(
    pendingInvitations: List<ScholarConnection>,
    suggestedScholars: List<ScholarConnection>,
    isLoading: Boolean,
    searchQuery: String,
    selectedFilterChip: String,
    onFilterChipSelected: (String) -> Unit,
    currentUserSchool: String,
    currentUserField: String,
    onAccept: (ScholarConnection) -> Unit,
    onIgnore: (ScholarConnection) -> Unit,
    onConnectClick: (ScholarConnection) -> Unit,
    onWithdrawClick: (ScholarConnection) -> Unit
) {
    val trimmed = searchQuery.trim().lowercase()

    val filteredSuggestions = remember(suggestedScholars, trimmed, selectedFilterChip, currentUserSchool, currentUserField) {
        suggestedScholars.filter { scholar ->
            val matchesSearch = trimmed.isBlank() ||
                    scholar.name.lowercase().contains(trimmed) ||
                    scholar.affiliation.lowercase().contains(trimmed) ||
                    scholar.researchField.lowercase().contains(trimmed)

            val matchesFilter = when (selectedFilterChip) {
                "Same School" -> currentUserSchool.isNotBlank() &&
                        scholar.affiliation.contains(currentUserSchool, ignoreCase = true)
                "Same Field" -> currentUserField.isNotBlank() &&
                        scholar.researchField.contains(currentUserField, ignoreCase = true)
                "Top Mutual" -> scholar.mutualCount >= 10
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // -----------------------------------------------------------------
        // Section: Received Invitations Tray
        // -----------------------------------------------------------------
        if (pendingInvitations.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Invitations (${pendingInvitations.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pending received",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandBlue
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    pendingInvitations.forEach { inv ->
                        InvitationCard(
                            invitation = inv,
                            onAccept = { onAccept(inv) },
                            onIgnore = { onIgnore(inv) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                HorizontalDivider(thickness = 8.dp, color = PageNeutral)
            }
        }

        // -----------------------------------------------------------------
        // Section: Filter Chips & Header for Suggestions
        // -----------------------------------------------------------------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "People You May Know in Academia",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = "Discover and connect with scholars based on your school, field, and citations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Filter Chips Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chips = listOf("All", "Same School", "Same Field", "Top Mutual")
                    items(chips) { chip ->
                        FilterChip(
                            selected = selectedFilterChip == chip,
                            onClick = { onFilterChipSelected(chip) },
                            label = { Text(chip, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // Section: Suggested Scholars List
        // -----------------------------------------------------------------
        when {
            isLoading -> {
                item {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) { ListRowSkeleton() }
                    }
                }
            }

            filteredSuggestions.isEmpty() -> {
                item {
                    EmptyState(
                        title = "No Scholars Found",
                        message = if (searchQuery.isNotBlank())
                            "No scholar matching '$searchQuery' was found. Try a different query."
                        else "No suggested scholars found for this filter.",
                        icon = Icons.Filled.People,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            }

            else -> {
                items(filteredSuggestions, key = { it.id }) { scholar ->
                    SuggestedScholarCard(
                        scholar = scholar,
                        onConnectClick = { onConnectClick(scholar) },
                        onWithdrawClick = { onWithdrawClick(scholar) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// =========================================================================
// Invitation Card (Pending Received)
// =========================================================================

@Composable
private fun InvitationCard(
    invitation: ScholarConnection,
    onAccept: () -> Unit,
    onIgnore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar
                ScholarAvatar(initials = invitation.initials, size = 52.dp)

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invitation.name + if (invitation.degree.isNotBlank()) ", ${invitation.degree}" else "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.School,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = invitation.affiliation,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = invitation.researchField,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (invitation.connectionReason.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandBlue.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Context: ${invitation.connectionReason}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = BrandBlue
                            )
                        }
                    }
                }
            }

            // Personal note quote box
            if (invitation.personalNote.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceInset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Outlined.FormatQuote,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "\"${invitation.personalNote}\"",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons: Ignore | Accept
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onIgnore,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Ignore", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(Modifier.width(10.dp))

                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Accept", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
    }
}

// =========================================================================
// Suggested Scholar Card
// =========================================================================

@Composable
private fun SuggestedScholarCard(
    scholar: ScholarConnection,
    onConnectClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column {
            // Subtle decorative top accent
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                BrandBlue.copy(alpha = 0.85f),
                                AccentGreen.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Overlapping avatar
                ScholarAvatar(
                    initials = scholar.initials,
                    size = 54.dp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scholar.name + if (scholar.degree.isNotBlank()) ", ${scholar.degree}" else "",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(3.dp))

                    // Affiliation / School
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.School,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = scholar.affiliation,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    // Research Field
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Science,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = scholar.researchField,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (scholar.mutualCount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${scholar.mutualCount} mutual connections in Circle",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bottom Action Row
            HorizontalDivider(thickness = 0.5.dp, color = DividerLight)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (scholar.status) {
                    ConnectionStatus.PENDING_SENT.name -> {
                        OutlinedButton(
                            onClick = onWithdrawClick,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, BrandBlue),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Pending",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandBlue
                            )
                        }
                    }

                    ConnectionStatus.CONNECTED.name -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = AccentGreen.copy(alpha = 0.12f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Connected",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AccentGreen
                                )
                            }
                        }
                    }

                    else -> {
                        Button(
                            onClick = onConnectClick,
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.PersonAdd,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Connect",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// My Circle Tab Content (Active Connections)
// =========================================================================

@Composable
private fun MyCircleContent(
    connectedScholars: List<ScholarConnection>,
    isLoading: Boolean,
    searchQuery: String,
    onRemove: (ScholarConnection) -> Unit
) {
    val trimmed = searchQuery.trim().lowercase()
    val filtered = remember(connectedScholars, trimmed) {
        if (trimmed.isBlank()) connectedScholars else connectedScholars.filter {
            it.name.lowercase().contains(trimmed) ||
                    it.affiliation.lowercase().contains(trimmed) ||
                    it.researchField.lowercase().contains(trimmed)
        }
    }

    when {
        isLoading -> {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { ListRowSkeleton() }
            }
        }

        filtered.isEmpty() -> {
            EmptyState(
                title = if (connectedScholars.isEmpty()) "No Connections Yet" else "No Matching Scholars",
                message = if (connectedScholars.isEmpty())
                    "Grow your academic network by connecting with colleagues, co-authors, and peers from the suggestions tab."
                else "No connected scholar matched '$searchQuery'.",
                icon = Icons.Filled.People,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp)
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filtered, key = { it.id }) { scholar ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScholarAvatar(initials = scholar.initials, size = 48.dp)

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = scholar.name + if (scholar.degree.isNotBlank()) ", ${scholar.degree}" else "",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = scholar.affiliation,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = BrandBlue
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                text = scholar.researchField,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { onRemove(scholar) }) {
                            Icon(
                                Icons.Default.PersonRemove,
                                contentDescription = "Remove Connection",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
                }
            }
        }
    }
}

// =========================================================================
// Connect with Scholar Modal Dialog
// =========================================================================

@Composable
private fun ConnectDialog(
    scholar: ScholarConnection,
    onDismiss: () -> Unit,
    onSend: (note: String) -> Unit
) {
    var personalNote by remember { mutableStateOf("") }

    val quickNoteTemplates = remember(scholar) {
        listOf(
            "I admire your research work in ${scholar.researchField.split("&").first().trim()}!",
            "I would love to collaborate on upcoming research papers.",
            "Fellow academic reaching out to stay connected on Cite Circle.",
            "Loved citing your recent work in our publications."
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Connect with ${scholar.name}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "You can add a note to personalize your invitation to ${scholar.name}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Quick template chips
                Text(
                    text = "Quick suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(quickNoteTemplates) { tpl ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceInset,
                            modifier = Modifier.clickable { personalNote = tpl }
                        ) {
                            Text(
                                text = tpl,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = BrandBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = personalNote,
                    onValueChange = { if (it.length <= 300) personalNote = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp),
                    placeholder = {
                        Text(
                            "Mention why you'd like to connect, papers you enjoyed, or potential collaborations...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Text(
                    text = "${personalNote.length}/300",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textAlign = TextAlign.End
                )

                Spacer(Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(10.dp))

                    Button(
                        onClick = { onSend(personalNote) },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Send Invitation",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// Helper Avatar Component
// =========================================================================

@Composable
private fun ScholarAvatar(
    initials: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(BrandBlue),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotBlank()) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2.5f).sp
            )
        } else {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}
