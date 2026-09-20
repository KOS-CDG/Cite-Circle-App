package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.data.AuthorIdentity
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerLight
import com.example.ui.theme.PageNeutral
import com.example.ui.theme.SurfaceInset

// ---------------------------------------------------------------------------
// MenuScreen
// ---------------------------------------------------------------------------

@Composable
fun MenuScreen(viewModel: HomeViewModel, navController: NavController) {
    val context = LocalContext.current
    val identity = remember(context) { AuthorIdentity.current(context) }

    var showHelp by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showConferencesDialog by remember { mutableStateOf(false) }
    var showCommunityStandardsDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }

    // Build the 6 shortcuts as a flat list and chunk them into rows of 2.
    data class Shortcut(
        val icon: ImageVector,
        val badgeColor: Color,
        val title: String,
        val onClick: () -> Unit
    )

    val shortcuts = listOf(
        Shortcut(
            icon = Icons.Filled.Chat,
            badgeColor = BrandBlue,
            title = "Messenger",
            onClick = { navController.navigate("messenger") }
        ),
        Shortcut(
            icon = Icons.Filled.Groups,
            badgeColor = BrandBlue,
            title = "Research Fields",
            onClick = { navController.navigate("fields") }
        ),
        Shortcut(
            icon = Icons.Filled.Bookmark,
            badgeColor = Color(0xFF6A0DAD),
            title = "Paper Vault",
            onClick = { navController.navigate("lists") }
        ),
        Shortcut(
            icon = Icons.Filled.AutoAwesome,
            badgeColor = Color(0xFFE7A33E),
            title = "Gemini Assistant",
            onClick = { navController.navigate("chat") }
        ),
        Shortcut(
            icon = Icons.Filled.Event,
            badgeColor = Color(0xFFB3261E),
            title = "Conferences",
            onClick = { showConferencesDialog = true }
        ),
        Shortcut(
            icon = Icons.Filled.Analytics,
            badgeColor = Color(0xFF057642),
            title = "Citation Metrics",
            onClick = { navController.navigate("profile") }
        )
    )

    val shortcutRows = shortcuts.chunked(2)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ----------------------------------------------------------------
        // Section 1: Header Row
        // ----------------------------------------------------------------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Menu",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { navController.navigate("fields") }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
            }
        }

        // ----------------------------------------------------------------
        // Section 2: Profile Shortcut Card
        // ----------------------------------------------------------------
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { navController.navigate("profile") }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    initials = identity.initials,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = identity.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "View your profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
        }

        // 8dp PageNeutral spacer block
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(PageNeutral)
            )
        }

        // ----------------------------------------------------------------
        // Section 3: 2-Column Shortcut Grid (3 rows of 2 tiles each)
        // ----------------------------------------------------------------
        shortcutRows.forEachIndexed { rowIndex, rowShortcuts ->
            item(key = "shortcut_row_$rowIndex") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowShortcuts.forEachIndexed { tileIndex, shortcut ->
                        ShortcutTile(
                            icon = shortcut.icon,
                            badgeColor = shortcut.badgeColor,
                            title = shortcut.title,
                            onClick = shortcut.onClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // If the last row has only 1 item, fill the second slot with a spacer.
                    if (rowShortcuts.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 8dp PageNeutral spacer block after grid
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(PageNeutral)
            )
        }

        // ----------------------------------------------------------------
        // Section 4: Collapsible Section Drawers
        // ----------------------------------------------------------------

        // Drawer 1: Help & Support
        item {
            HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHelp = !showHelp }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Help & Support",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (showHelp) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (showHelp) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (showHelp) {
                DrawerChildRow(
                    icon = Icons.Outlined.Shield,
                    title = "Community Standards",
                    onClick = { showCommunityStandardsDialog = true }
                )
                DrawerChildRow(
                    icon = Icons.Outlined.BugReport,
                    title = "Report a Problem",
                    onClick = { showReportDialog = true }
                )
            }
        }

        // Drawer 2: Settings & Privacy
        item {
            HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSettings = !showSettings }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings & Privacy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (showSettings) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (showSettings) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (showSettings) {
                DrawerChildRow(
                    icon = Icons.Outlined.Settings,
                    title = "Settings",
                    onClick = { navController.navigate("settings") }
                )
                DrawerChildRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Shortcuts",
                    onClick = { navController.navigate("privacy_policy") }
                )
                DrawerChildRow(
                    icon = Icons.Outlined.PhoneAndroid,
                    title = "Device Permissions & Cache",
                    onClick = { showCacheDialog = true }
                )
                DrawerChildRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Check for Updates",
                    onClick = {
                        viewModel.checkForUpdates(
                            currentVersionCode = com.example.BuildConfig.VERSION_CODE,
                            isManualCheck = true,
                            onUpToDate = {
                                viewModel.report("Cite Circle is up to date (v${com.example.BuildConfig.VERSION_NAME})")
                            }
                        )
                    }
                )
            }
        }

        // ----------------------------------------------------------------
        // Bottom Spacer
        // ----------------------------------------------------------------
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showConferencesDialog) {
        AlertDialog(
            onDismissRequest = { showConferencesDialog = false },
            title = {
                Text(
                    text = "Upcoming Academic Conferences",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Submission deadlines and conference schedules across core venues:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(
                        "NeurIPS 2026" to "Abstract: May 15 · Paper: May 22 (New Orleans)",
                        "ICML 2026" to "Abstract: Jan 26 · Paper: Feb 02 (Vienna)",
                        "CVPR 2026" to "Full Paper: Nov 14 (Denver)",
                        "ICLR 2026" to "Paper Submission: Sep 28 (Singapore)",
                        "ACL 2026" to "Paper Submission: Dec 15 (Toronto)"
                    ).forEach { (conf, deadline) ->
                        Column {
                            Text(conf, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BrandBlue)
                            Text(deadline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        showConferencesDialog = false
                        navController.navigate("fields")
                    }) {
                        Text("Fields")
                    }
                    Button(onClick = {
                        showConferencesDialog = false
                        navController.navigate("opps")
                    }) {
                        Text("All Opportunities")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showConferencesDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCommunityStandardsDialog) {
        AlertDialog(
            onDismissRequest = { showCommunityStandardsDialog = false },
            title = {
                Text(
                    text = "Cite Circle Community Standards",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Principles governing academic publication, preprint dissemination, and peer interactions:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("1. Citation Integrity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("All claims, lemmas, and comparative baselines must accurately cite published and preprint sources without fabrication.", style = MaterialTheme.typography.bodySmall)

                        Text("2. Constructive Peer Review", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("Discussions and reviews must evaluate scientific rigor objectively and professionally without personal hostility.", style = MaterialTheme.typography.bodySmall)

                        Text("3. Open Access & Attribution", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("Reproducible code, datasets, and arXiv preprints must preserve original author credits and open licenses.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCommunityStandardsDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    if (showReportDialog) {
        var issueText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text(
                    text = "Report an Academic Issue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Describe broken citations, manuscript PDF rendering errors, or community standard violations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = issueText,
                        onValueChange = { issueText = it },
                        placeholder = { Text("Details regarding the issue or violation...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReportDialog = false
                        viewModel.report("Thank you. Academic issue report logged for peer review.")
                    },
                    enabled = issueText.isNotBlank()
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCacheDialog) {
        val cacheSizeBytes = remember {
            val cacheSize = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            val filesSize = context.filesDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            cacheSize + filesSize
        }
        val formattedSize = remember(cacheSizeBytes) {
            when {
                cacheSizeBytes < 1024 -> "$cacheSizeBytes B"
                cacheSizeBytes < 1024 * 1024 -> "${cacheSizeBytes / 1024} KB"
                else -> String.format(java.util.Locale.US, "%.1f MB", cacheSizeBytes / (1024f * 1024f))
            }
        }

        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = {
                Text(
                    text = "Device Storage & Cache",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Cite Circle stores research papers, author citations, and PDF manuscripts securely in local sandboxed storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceInset)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Local Vault & Cache Storage:", style = MaterialTheme.typography.bodyMedium)
                        Text(formattedSize, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = BrandBlue)
                    }

                    Text(
                        text = "• Room v5 SQLite: Offline paper database\n• PDF Vault: Sandboxed research PDFs\n• Figure Cache: Manuscript diagrams & plots",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        try {
                            context.cacheDir.deleteRecursively()
                            viewModel.report("Temporary cache cleared successfully")
                        } catch (e: Exception) {
                            viewModel.report("Failed to clear cache")
                        }
                        showCacheDialog = false
                    }) {
                        Text("Clear Cache")
                    }
                    Button(onClick = {
                        showCacheDialog = false
                        navController.navigate("onboarding_permissions")
                    }) {
                        Text("Permissions")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCacheDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// ShortcutTile
// ---------------------------------------------------------------------------

@Composable
private fun ShortcutTile(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ---------------------------------------------------------------------------
// DrawerChildRow — private helper for collapsible drawer child items
// ---------------------------------------------------------------------------

@Composable
private fun DrawerChildRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 40.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
