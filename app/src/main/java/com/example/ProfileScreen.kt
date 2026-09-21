package com.example

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.data.AuthorIdentity
import com.example.data.ProfileStats
import com.example.data.CitationFormatter
import com.example.data.ExportFormat
import com.example.data.SavedPaper
import com.example.ui.components.RefreshableBox
import com.example.ui.components.EmptyState
import com.example.ui.components.ListRowSkeleton
import com.example.ui.post.PostCard
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.DividerLight
import com.example.ui.theme.PageNeutral
import com.example.ui.theme.SurfaceInset
import com.example.ui.theme.SurfaceWhite
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.launch
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class SkillItem(
    val name: String,
    var endorsementCount: Int,
    var isEndorsed: Boolean = false
)

data class ExperienceItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val institution: String,
    val duration: String,
    val location: String,
    val description: String
)

data class EducationItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val school: String,
    val degree: String,
    val years: String,
    val details: String
)

private fun parseExperienceJson(json: String): List<ExperienceItem> {
    if (json.isBlank()) {
        return listOf(
            ExperienceItem(
                title = "Postdoctoral Research Fellow",
                institution = "Stanford AI & Quantum Lab",
                duration = "2024 – Present · 2 yrs",
                location = "Palo Alto, California · Full-time",
                description = "Investigating quantum foundation model reasoning, distributed parameter sharding, and reproducible peer review verification."
            ),
            ExperienceItem(
                title = "Doctoral Researcher & Teaching Fellow",
                institution = "MIT CSAIL",
                duration = "2020 – 2024 · 4 yrs",
                location = "Cambridge, Massachusetts",
                description = "Published 6 first-author manuscripts across NeurIPS, ICML, and ICLR on transformer attention mechanics."
            ),
            ExperienceItem(
                title = "Visiting Research Scientist",
                institution = "Google DeepMind / Quantum AI",
                duration = "Summer 2023 · 5 mos",
                location = "Mountain View, California",
                description = "Co-developed tensor-network representations for scalable quantum circuit simulation."
            )
        )
    }
    return try {
        val array = JSONArray(json)
        val list = mutableListOf<ExperienceItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ExperienceItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.optString("title", ""),
                    institution = obj.optString("institution", ""),
                    duration = obj.optString("duration", ""),
                    location = obj.optString("location", ""),
                    description = obj.optString("description", "")
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun serializeExperienceList(list: List<ExperienceItem>): String {
    val array = JSONArray()
    for (item in list) {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("title", item.title)
        obj.put("institution", item.institution)
        obj.put("duration", item.duration)
        obj.put("location", item.location)
        obj.put("description", item.description)
        array.put(obj)
    }
    return array.toString()
}

private fun parseEducationJson(json: String): List<EducationItem> {
    if (json.isBlank()) {
        return listOf(
            EducationItem(
                school = "Massachusetts Institute of Technology (MIT)",
                degree = "Doctor of Philosophy (Ph.D.), Computer Science & AI",
                years = "2020 – 2024",
                details = "Dissertation: 'Scalable Attention Dynamics in Non-Euclidean Latent Spaces'"
            ),
            EducationItem(
                school = "University of California, Berkeley",
                degree = "Bachelor of Science (B.S.), Electrical Engineering & Computer Sciences",
                years = "2016 – 2020",
                details = "Summa Cum Laude · Regents' and Chancellor's Scholar"
            )
        )
    }
    return try {
        val array = JSONArray(json)
        val list = mutableListOf<EducationItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                EducationItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    school = obj.optString("school", ""),
                    degree = obj.optString("degree", ""),
                    years = obj.optString("years", ""),
                    details = obj.optString("details", "")
                )
            )
        }
        list
    } catch (_: Exception) {
        emptyList()
    }
}

private fun serializeEducationList(list: List<EducationItem>): String {
    val array = JSONArray()
    for (item in list) {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("school", item.school)
        obj.put("degree", item.degree)
        obj.put("years", item.years)
        obj.put("details", item.details)
        array.put(obj)
    }
    return array.toString()
}

/**
 * High-fidelity, fully interactive LinkedIn-style Profile screen for Cite Circle.
 * Features left-aligned overlapping avatar, working photo pickers, comprehensive intro editor,
 * "Open to" sheet, analytics insights, interactive endorsements, and structured academic sections.
 */
@Composable
fun ProfileScreen(viewModel: HomeViewModel, navController: NavController) {
    val feed by viewModel.feed.collectAsStateWithLifecycle()
    val activityState by viewModel.activity.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val sessionManager = app.sessionManager
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Observe persistent profile data
    val currentName by sessionManager.currentUserName.collectAsStateWithLifecycle(initialValue = "")
    val currentHeadline by sessionManager.currentUserHeadline.collectAsStateWithLifecycle(initialValue = "")
    val currentAffiliation by sessionManager.currentUserAffiliation.collectAsStateWithLifecycle(initialValue = "")
    val currentField by sessionManager.currentUserField.collectAsStateWithLifecycle(initialValue = "")
    val currentLocation by sessionManager.currentUserLocation.collectAsStateWithLifecycle(initialValue = "")
    val currentBio by sessionManager.currentUserBio.collectAsStateWithLifecycle(initialValue = "")
    val currentAvatarUri by sessionManager.currentUserAvatarUri.collectAsStateWithLifecycle(initialValue = "")
    val currentCoverUri by sessionManager.currentUserCoverUri.collectAsStateWithLifecycle(initialValue = "")
    val currentOrcid by sessionManager.currentUserOrcid.collectAsStateWithLifecycle(initialValue = "")
    val currentWebsite by sessionManager.currentUserWebsite.collectAsStateWithLifecycle(initialValue = "")
    val currentEmail by sessionManager.currentUserEmail.collectAsStateWithLifecycle(initialValue = "")
    val currentOpenTo by sessionManager.currentUserOpenTo.collectAsStateWithLifecycle(initialValue = "")
    val currentDegree by sessionManager.currentUserDegree.collectAsStateWithLifecycle(initialValue = "")
    val currentExperienceJson by sessionManager.currentUserExperienceJson.collectAsStateWithLifecycle(initialValue = "")
    val currentEducationJson by sessionManager.currentUserEducationJson.collectAsStateWithLifecycle(initialValue = "")
    val currentSkillsCsv by sessionManager.currentUserSkillsCsv.collectAsStateWithLifecycle(initialValue = "")

    // Fallbacks
    val authorIdentity = remember(context) { AuthorIdentity.current(context) }
    val displayName = currentName.ifBlank { authorIdentity.name.ifBlank { "Dr. Alex Rivera" } }
    val affiliation = currentAffiliation.ifBlank { "Stanford University · AI & Quantum Lab" }
    val headline = currentHeadline.ifBlank { "Senior Research Scientist @ Stanford AI Lab | Cite Circle Fellow" }
    val researchField = currentField.ifBlank { "Computer Science & Machine Learning" }
    val location = currentLocation.ifBlank { "Palo Alto, California, United States" }
    val bio = currentBio.ifBlank {
        "Lead researcher investigating foundation model reasoning, distributed systems scalability, and open peer review reproducibility. Passionate about transparent preprint dissemination and interdisciplinary collaboration."
    }
    val orcid = currentOrcid.ifBlank { "0009-0004-8921-4412" }
    val website = currentWebsite.ifBlank { "https://citecircle.org/author/arivera" }
    val email = currentEmail.ifBlank { "alex.rivera@citecircle.edu" }
    val openTo = currentOpenTo.ifBlank { "Research Collaborations · Peer Review" }
    val degreeSuffix = currentDegree.ifBlank { "(Ph.D.)" }

    val stats = remember(feed.items) { ProfileStats.from(feed.items) }
    val initials = remember(displayName) { AuthorIdentity.initialsOf(displayName) }

    // Dialog & UI interaction states
    var showEditIntroDialog by remember { mutableStateOf(false) }
    var showContactInfoDialog by remember { mutableStateOf(false) }
    var showOpenToDialog by remember { mutableStateOf(false) }
    var showVerificationDialog by remember { mutableStateOf(false) }
    var showConnectionsDialog by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Academic Experience, Education, and Skills state
    val experiences = remember(currentExperienceJson) {
        parseExperienceJson(currentExperienceJson).toMutableStateList()
    }
    var showAddEditExperienceDialog by remember { mutableStateOf(false) }
    var experienceToEdit by remember { mutableStateOf<ExperienceItem?>(null) }

    val educations = remember(currentEducationJson) {
        parseEducationJson(currentEducationJson).toMutableStateList()
    }
    var showAddEditEducationDialog by remember { mutableStateOf(false) }
    var educationToEdit by remember { mutableStateOf<EducationItem?>(null) }

    val defaultSkillNames = remember {
        listOf(
            "Deep Learning & PyTorch",
            "Quantum Information Science",
            "Distributed System Architecture",
            "Academic Peer Review & Ethics",
            "Transformer Model Alignment"
        )
    }
    val skillNames = remember(currentSkillsCsv) {
        if (currentSkillsCsv.isNotBlank()) {
            currentSkillsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            defaultSkillNames
        }
    }
    val skills = remember(skillNames) {
        skillNames.mapIndexed { idx, name ->
            SkillItem(name, 48 - idx * 7, false)
        }.toMutableStateList()
    }
    var showAddSkillDialog by remember { mutableStateOf(false) }

    var connectionCount by remember { mutableIntStateOf(1248) }
    var selectedActivityTab by remember { mutableIntStateOf(0) }
    var isBioExpanded by remember { mutableStateOf(false) }

    // Photo pickers
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                sessionManager.updateCoverUri(it.toString())
                viewModel.report("Cover banner updated successfully.")
            }
        }
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                sessionManager.updateAvatarUri(it.toString())
                viewModel.report("Profile photo updated successfully.")
            }
        }
    }

    RefreshableBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PageNeutral)
        ) {
            // -------------------------------------------------------------
            // Top App Bar (LinkedIn Search & Header)
            // -------------------------------------------------------------
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(54.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(onClick = { navController.navigate("fields") }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // Profile Main Scrollable Stream
            // -------------------------------------------------------------
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // =========================================================
                // 1. Hero Intro Card (LinkedIn Top Card)
                // =========================================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Banner & Overlapping Avatar Container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            ) {
                                // Cover Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(135.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                ) {
                                    if (currentCoverUri.isNotBlank()) {
                                        AsyncImage(
                                            model = currentCoverUri,
                                            contentDescription = "Cover Banner",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Camera edit button on Banner (top-right / bottom-right)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(10.dp)
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                                            .clickable { coverPickerLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.CameraAlt,
                                            contentDescription = "Edit cover",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Overlapping Avatar (LinkedIn style: LEFT-ALIGNED!)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 16.dp)
                                        .size(106.dp)
                                ) {
                                    // Outer ring
                                    Box(
                                        modifier = Modifier
                                            .size(106.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(3.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (currentAvatarUri.isNotBlank()) {
                                            AsyncImage(
                                                model = currentAvatarUri,
                                                contentDescription = "Profile Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(98.dp)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Avatar(initials, 98.dp)
                                        }
                                    }

                                    // Camera badge on Avatar
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                            .clickable { avatarPickerLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.AddAPhoto,
                                            contentDescription = "Edit profile photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Pencil Edit Icon (Top Right of Identity Block)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 12.dp, bottom = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = { showEditIntroDialog = true },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = "Edit Intro",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Identity Info Section (LEFT-ALIGNED)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                // Name + Verified Checkmark + Pronouns
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        Icons.Filled.Verified,
                                        contentDescription = "Verified Scholar",
                                        tint = BrandBlue,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { showVerificationDialog = true }
                                    )
                                    if (degreeSuffix.isNotBlank()) {
                                        Text(
                                            text = degreeSuffix,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // Professional Headline
                                Text(
                                    text = headline,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(Modifier.height(6.dp))

                                // Current Institution / School
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.School,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = affiliation,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                // Location & Contact Info link
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = location,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "·",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Contact info",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBlue
                                        ),
                                        modifier = Modifier.clickable { showContactInfoDialog = true }
                                    )
                                }

                                Spacer(Modifier.height(6.dp))

                                // Connections Count
                                Text(
                                    text = "$connectionCount Circle connections · ${stats.citations} peer citations",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandBlue
                                    ),
                                    modifier = Modifier.clickable { showConnectionsDialog = true }
                                )

                                Spacer(Modifier.height(10.dp))

                                // "Open to" Badge Container (Classic LinkedIn)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BrandBlue.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showOpenToDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Open to academic opportunities",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = BrandBlue
                                            )
                                            Text(
                                                text = openTo,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = null,
                                            tint = BrandBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                // Action Buttons Row (LinkedIn Owner Pill Buttons)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 1. Open to (Filled Pill)
                                    Button(
                                        onClick = { showOpenToDialog = true },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Open to",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    // 2. Edit Profile (Outlined Pill)
                                    OutlinedButton(
                                        onClick = { showEditIntroDialog = true },
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Edit Profile",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    // 3. Share Profile (Outlined Pill)
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(website))
                                            viewModel.report("Profile URL copied to clipboard")
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Share",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }

                                    // 4. More (...) Button
                                    Box {
                                        OutlinedButton(
                                            onClick = { showMoreMenu = true },
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.MoreHoriz,
                                                contentDescription = "More",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showMoreMenu,
                                            onDismissRequest = { showMoreMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Share Profile via Link") },
                                                onClick = {
                                                    showMoreMenu = false
                                                    clipboardManager.setText(AnnotatedString(website))
                                                    viewModel.report("Profile URL copied to clipboard")
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Send Profile in Direct Message") },
                                                onClick = {
                                                    showMoreMenu = false
                                                    navController.navigate("messenger") {
                                                        popUpTo("feed") { saveState = true }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Export Academic CV (BibTeX)") },
                                                onClick = {
                                                    showMoreMenu = false
                                                    val bibtex = feed.items.joinToString("\n\n") { paper ->
                                                        CitationFormatter.export(paper, ExportFormat.BIBTEX)
                                                    }
                                                    clipboardManager.setText(AnnotatedString(bibtex))
                                                    viewModel.report("Academic BibTeX records copied to clipboard")
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Academic Verification Badge") },
                                                onClick = {
                                                    showMoreMenu = false
                                                    showVerificationDialog = true
                                                },
                                                leadingIcon = { Icon(Icons.Outlined.Verified, contentDescription = null) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // 2. LinkedIn "Analytics" Card (Private to you)
                // =========================================================
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAnalyticsDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Analytics",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Private to you",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AnalyticsMetricTile(
                                        icon = Icons.Outlined.Visibility,
                                        value = "1,280",
                                        label = "profile views",
                                        subtext = "Past 7 days"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    AnalyticsMetricTile(
                                        icon = Icons.Outlined.AutoGraph,
                                        value = "3,420",
                                        label = "post impressions",
                                        subtext = "+18% this week"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    AnalyticsMetricTile(
                                        icon = Icons.Filled.Search,
                                        value = "412",
                                        label = "search appearances",
                                        subtext = "Top scholar search"
                                    )
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // 3. LinkedIn "About" Card (Summary & Research Domains)
                // =========================================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "About",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { showEditIntroDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = "Edit About",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isBioExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.animateContentSize()
                            )

                            Text(
                                text = if (isBioExpanded) "see less" else "...see more",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BrandBlue),
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .clickable { isBioExpanded = !isBioExpanded }
                            )

                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Top Specialties & Research Domains",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(8.dp))

                            val specialties = remember(researchField, skillNames) {
                                (listOf(researchField) + skillNames).distinct().filter { it.isNotBlank() }.take(6)
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(specialties) { domain ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = SurfaceInset,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = domain,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // 4. LinkedIn "Featured Publications" Card
                // =========================================================
                item {
                    val preprints = feed.items.filter { it.pdfLocalPath.isNotBlank() || it.doi.isNotBlank() || it.url.isNotBlank() }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "Featured Publications",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Top selected manuscripts and research preprints",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { navController.navigate("compose") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add Publication",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            if (preprints.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SurfaceInset,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No manuscripts pinned yet. Tap + to feature your publications here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(preprints.take(5), key = { "feat_${it.id}" }) { paper ->
                                        FeaturedPaperCard(paper, onRead = {
                                            navController.navigate("post/${paper.id}")
                                        })
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // 5. LinkedIn "Activity & Posts" Card
                // =========================================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Activity",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$connectionCount followers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrandBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { navController.navigate("compose") },
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Create a post", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            // Activity Tabs
                            val tabs = listOf("Posts", "Preprints", "Reviews", "Figures")
                            ScrollableTabRow(
                                selectedTabIndex = selectedActivityTab,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = BrandBlue,
                                edgePadding = 16.dp,
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedActivityTab]),
                                        color = BrandBlue,
                                        height = 2.5.dp
                                    )
                                },
                                divider = { HorizontalDivider(thickness = 0.5.dp, color = DividerLight) }
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedActivityTab == index,
                                        onClick = { selectedActivityTab = index },
                                        text = {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (selectedActivityTab == index) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = if (selectedActivityTab == index) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Render Dynamic Stream based on Activity Tab
                when (selectedActivityTab) {
                    0 -> { // All Posts
                        if (feed.isLoading) {
                            items(2) { ListRowSkeleton() }
                        } else if (feed.isEmpty) {
                            item {
                                EmptyState(
                                    title = stringResource(R.string.profile_empty_title),
                                    message = stringResource(R.string.profile_empty_message),
                                    icon = Icons.Outlined.Article,
                                    actionLabel = stringResource(R.string.action_write_entry),
                                    onAction = { navController.navigate("compose") }
                                )
                            }
                        } else {
                            items(feed.items, key = { it.id }) { paper ->
                                PostCard(paper, viewModel, navController)
                            }
                        }
                    }
                    1 -> { // Preprints
                        val preprints = feed.items.filter { it.pdfLocalPath.isNotBlank() || it.doi.isNotBlank() || it.url.isNotBlank() }
                        if (preprints.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "No Preprints Uploaded",
                                    message = "Manuscripts with PDF or DOI links will appear in your Preprints archive.",
                                    icon = Icons.Outlined.Article,
                                    actionLabel = "Upload Preprint",
                                    onAction = { navController.navigate("compose") }
                                )
                            }
                        } else {
                            items(preprints, key = { "prep_${it.id}" }) { paper ->
                                PostCard(paper, viewModel, navController)
                            }
                        }
                    }
                    2 -> { // Reviews & Comments
                        if (activityState.items.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "No Peer Reviews Yet",
                                    message = "Peer review commentary and manuscript endorsements will appear here.",
                                    icon = Icons.Outlined.Notifications,
                                    actionLabel = "Browse Papers",
                                    onAction = { navController.navigate("feed") }
                                )
                            }
                        } else {
                            items(activityState.items) { item ->
                                ActivityRow(item) {
                                    when (item) {
                                        is ActivityItem.Replied -> navController.navigate("post/${item.comment.paperId}")
                                        is ActivityItem.Cited -> navController.navigate("post/${item.quote.quotedId}")
                                        is ActivityItem.Endorsed -> navController.navigate("post/${item.paperId}")
                                        is ActivityItem.Messaged -> {
                                            if (item.targetPaperId.isNotBlank()) navController.navigate("chat_thread/${item.targetPaperId}")
                                            else navController.navigate("messenger")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // Figures
                        val figurePapers = feed.items.filter { it.imageUri.isNotBlank() }
                        if (figurePapers.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "No Figures Uploaded",
                                    message = "Upload scientific plots, equations, or architecture diagrams to showcase them here.",
                                    icon = Icons.Outlined.Image,
                                    actionLabel = "Attach Figure",
                                    onAction = { navController.navigate("compose") }
                                )
                            }
                        } else {
                            items(figurePapers, key = { "fig_${it.id}" }) { paper ->
                                PostCard(paper, viewModel, navController)
                            }
                        }
                    }
                }

                // =========================================================
                // 6. LinkedIn "Experience & Academic Positions" Card (EDITABLE)
                // =========================================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Experience & Academic Positions",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        experienceToEdit = null
                                        showAddEditExperienceDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add Experience",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            if (experiences.isEmpty()) {
                                Text(
                                    text = "No academic positions added yet. Tap + to add experience.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                experiences.forEachIndexed { index, exp ->
                                    TimelineExperienceItem(
                                        title = exp.title,
                                        institution = exp.institution,
                                        duration = exp.duration,
                                        location = exp.location,
                                        description = exp.description,
                                        onEdit = {
                                            experienceToEdit = exp
                                            showAddEditExperienceDialog = true
                                        }
                                    )
                                    if (index < experiences.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            thickness = 0.5.dp,
                                            color = DividerLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // 7. LinkedIn "Education" Card (EDITABLE)
                // =========================================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Education",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        educationToEdit = null
                                        showAddEditEducationDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add Education",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            if (educations.isEmpty()) {
                                Text(
                                    text = "No education degrees added yet. Tap + to add education.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                educations.forEachIndexed { index, edu ->
                                    TimelineEducationItem(
                                        school = edu.school,
                                        degree = edu.degree,
                                        years = edu.years,
                                        details = edu.details,
                                        onEdit = {
                                            educationToEdit = edu
                                            showAddEditEducationDialog = true
                                        }
                                    )
                                    if (index < educations.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            thickness = 0.5.dp,
                                            color = DividerLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // 8. LinkedIn "Skills & Endorsements" Card (EDITABLE & INTERACTIVE)
                // =========================================================
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Skills & Endorsements",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Peer-verified academic proficiencies",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { showAddSkillDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Add,
                                        contentDescription = "Add Skill",
                                        tint = BrandBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            if (skills.isEmpty()) {
                                Text(
                                    text = "No skills listed yet. Tap + to add academic proficiencies.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                skills.forEachIndexed { index, skill ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = skill.name,
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${skill.endorsementCount} endorsements by peer researchers",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    skill.isEndorsed = !skill.isEndorsed
                                                    skill.endorsementCount += if (skill.isEndorsed) 1 else -1
                                                    viewModel.report(
                                                        if (skill.isEndorsed) "Endorsed ${skill.name} for $displayName"
                                                        else "Removed endorsement"
                                                    )
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (skill.isEndorsed) BrandBlue.copy(alpha = 0.1f) else Color.Transparent,
                                                    contentColor = if (skill.isEndorsed) BrandBlue else MaterialTheme.colorScheme.onSurface
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.ThumbUp,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    if (skill.isEndorsed) "Endorsed" else "Endorse",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    skills.remove(skill)
                                                    coroutineScope.launch {
                                                        sessionManager.updateSkillsCsv(skills.joinToString(",") { it.name })
                                                        viewModel.report("Removed skill: ${skill.name}")
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = "Remove skill",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    if (index < skills.size - 1) {
                                        HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // =============================================================
    // Dialog 1: LinkedIn "Edit Intro" Full Working Modal
    // =============================================================
    if (showEditIntroDialog) {
        var editName by remember { mutableStateOf(displayName) }
        var editDegree by remember { mutableStateOf(degreeSuffix) }
        var editHeadline by remember { mutableStateOf(headline) }
        var editAffiliation by remember { mutableStateOf(affiliation) }
        var editField by remember { mutableStateOf(researchField) }
        var editLocation by remember { mutableStateOf(location) }
        var editBio by remember { mutableStateOf(bio) }
        var editOrcid by remember { mutableStateOf(orcid) }
        var editWebsite by remember { mutableStateOf(website) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditIntroDialog = false },
            title = {
                Text(
                    text = "Edit Intro",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Full Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editDegree,
                            onValueChange = { editDegree = it },
                            label = { Text("Academic Degree / Title Suffix (e.g. Ph.D., M.S.)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editHeadline,
                            onValueChange = { editHeadline = it },
                            label = { Text("Headline *") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editAffiliation,
                            onValueChange = { editAffiliation = it },
                            label = { Text("Current Institution / Affiliation") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editField,
                            onValueChange = { editField = it },
                            label = { Text("Research Field / Specialty") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editLocation,
                            onValueChange = { editLocation = it },
                            label = { Text("Location (City, Country)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editBio,
                            onValueChange = { editBio = it },
                            label = { Text("About / Academic Bio") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editOrcid,
                            onValueChange = { editOrcid = it },
                            label = { Text("ORCID ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = editWebsite,
                            onValueChange = { editWebsite = it },
                            label = { Text("Website / Scholar Link") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            sessionManager.updateFullProfile(
                                displayName = editName,
                                headline = editHeadline,
                                affiliation = editAffiliation,
                                researchField = editField,
                                location = editLocation,
                                bio = editBio,
                                orcid = editOrcid,
                                website = editWebsite,
                                degree = editDegree
                            )
                            isSaving = false
                            showEditIntroDialog = false
                            viewModel.report("Profile updated successfully.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditIntroDialog = false }, enabled = !isSaving) {
                    Text("Cancel")
                }
            }
        )
    }

    // =============================================================
    // Dialog 2: LinkedIn "Contact Info" Modal
    // =============================================================
    if (showContactInfoDialog) {
        AlertDialog(
            onDismissRequest = { showContactInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Email, contentDescription = null, tint = BrandBlue)
                    Text("$displayName's Contact Info", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ContactItemRow(
                        icon = Icons.Outlined.Email,
                        title = "Institutional Email",
                        value = email,
                        actionLabel = "Copy",
                        onAction = {
                            clipboardManager.setText(AnnotatedString(email))
                            viewModel.report("Email copied to clipboard.")
                        }
                    )
                    ContactItemRow(
                        icon = Icons.Outlined.Verified,
                        title = "ORCID Identifier",
                        value = orcid,
                        actionLabel = "Copy",
                        onAction = {
                            clipboardManager.setText(AnnotatedString("https://orcid.org/$orcid"))
                            viewModel.report("ORCID URL copied.")
                        }
                    )
                    ContactItemRow(
                        icon = Icons.Outlined.Language,
                        title = "Scholar Homepage / Lab",
                        value = website,
                        actionLabel = "Copy",
                        onAction = {
                            clipboardManager.setText(AnnotatedString(website))
                            viewModel.report("Website link copied.")
                        }
                    )
                    ContactItemRow(
                        icon = Icons.Outlined.LocationOn,
                        title = "Campus Office & Coordinates",
                        value = location,
                        actionLabel = null,
                        onAction = null
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // =============================================================
    // Dialog 3: LinkedIn "Open to" Modal
    // =============================================================
    if (showOpenToDialog) {
        var openCollab by remember { mutableStateOf(openTo.contains("Collaboration", ignoreCase = true)) }
        var openReview by remember { mutableStateOf(openTo.contains("Peer Review", ignoreCase = true)) }
        var openPostdoc by remember { mutableStateOf(openTo.contains("Faculty", ignoreCase = true) || openTo.contains("Postdoc", ignoreCase = true)) }

        AlertDialog(
            onDismissRequest = { showOpenToDialog = false },
            title = {
                Text("Open to Opportunities", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Highlight to the academic community what discussions, reviews, or roles you are receptive to:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = openCollab,
                            onCheckedChange = { openCollab = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Research Collaborations", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Grant applications, co-authorships, interdisciplinary projects", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = openReview,
                            onCheckedChange = { openReview = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Peer Review & Editorial Invitations", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Fast-track preprint assessments and journal reviews", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = openPostdoc,
                            onCheckedChange = { openPostdoc = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Postdoctoral & Faculty Roles", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Academic recruitment and visiting professorships", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val choices = mutableListOf<String>()
                        if (openCollab) choices.add("Research Collaborations")
                        if (openReview) choices.add("Peer Review")
                        if (openPostdoc) choices.add("Faculty/Postdoctoral Roles")
                        val resultStr = if (choices.isEmpty()) "Open to academic inquiries" else choices.joinToString(" · ")
                        coroutineScope.launch {
                            sessionManager.updateOpenTo(resultStr)
                            showOpenToDialog = false
                            viewModel.report("Academic preferences saved.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenToDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // =============================================================
    // Dialog 4: Academic Verification Dialog
    // =============================================================
    if (showVerificationDialog) {
        AlertDialog(
            onDismissRequest = { showVerificationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = BrandBlue)
                    Text("Verified Academic Profile", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Researcher: $displayName", fontWeight = FontWeight.SemiBold)
                    Text("Affiliation: $affiliation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ORCID: $orcid (Cryptographically Verified)", color = BrandBlue, fontWeight = FontWeight.SemiBold)
                    Text("Institutional Domain: $email (.edu certified)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Status: Active Peer Reviewer & Verified Scholar", style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                }
            },
            confirmButton = {
                TextButton(onClick = { showVerificationDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // =============================================================
    // Dialog 5: Circle Connections Modal
    // =============================================================
    if (showConnectionsDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionsDialog = false },
            title = {
                Text("Circle Connections ($connectionCount)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "$displayName is connected with $connectionCount verified scholars across Stanford, MIT, Harvard, Berkeley, and Oxford.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• 89 Mutual Circle Connections\n• 42 Co-authors in common\n• 500+ Cross-citations in preprint library",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showConnectionsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // =============================================================
    // Dialog 6: Analytics Insights Modal
    // =============================================================
    if (showAnalyticsDialog) {
        AlertDialog(
            onDismissRequest = { showAnalyticsDialog = false },
            title = {
                Text("Profile Analytics", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Private academic performance report (past 7 days):", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AnalyticsStatRow(label = "Profile Views", value = "1,280 (+14% vs last week)")
                        AnalyticsStatRow(label = "Paper Impressions on Feed", value = "3,420")
                        AnalyticsStatRow(label = "Search Appearances", value = "412 for 'Quantum Machine Learning'")
                        AnalyticsStatRow(label = "Author h-index", value = "18")
                        AnalyticsStatRow(label = "i10-index", value = "24")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnalyticsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // =============================================================
    // Dialog 7: Add / Edit Experience Dialog
    // =============================================================
    if (showAddEditExperienceDialog) {
        var title by remember { mutableStateOf(experienceToEdit?.title ?: "") }
        var institution by remember { mutableStateOf(experienceToEdit?.institution ?: "") }
        var duration by remember { mutableStateOf(experienceToEdit?.duration ?: "") }
        var expLocation by remember { mutableStateOf(experienceToEdit?.location ?: "") }
        var description by remember { mutableStateOf(experienceToEdit?.description ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddEditExperienceDialog = false
                experienceToEdit = null
            },
            title = {
                Text(
                    text = if (experienceToEdit == null) "Add Experience" else "Edit Experience",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Title / Role *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = institution,
                            onValueChange = { institution = it },
                            label = { Text("Institution / Organization *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            label = { Text("Duration (e.g., 2021 - Present)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = expLocation,
                            onValueChange = { expLocation = it },
                            label = { Text("Location (e.g., Stanford, CA)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description / Key Contributions") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && institution.isNotBlank()) {
                            val currentList = experiences.toMutableList()
                            val editing = experienceToEdit
                            if (editing != null) {
                                val index = currentList.indexOfFirst { it.id == editing.id }
                                if (index != -1) {
                                    currentList[index] = editing.copy(
                                        title = title.trim(),
                                        institution = institution.trim(),
                                        duration = duration.trim(),
                                        location = expLocation.trim(),
                                        description = description.trim()
                                    )
                                }
                            } else {
                                currentList.add(
                                    ExperienceItem(
                                        id = UUID.randomUUID().toString(),
                                        title = title.trim(),
                                        institution = institution.trim(),
                                        duration = duration.trim(),
                                        location = expLocation.trim(),
                                        description = description.trim()
                                    )
                                )
                            }
                            experiences.clear()
                            experiences.addAll(currentList)
                            coroutineScope.launch {
                                sessionManager.updateExperienceJson(serializeExperienceList(currentList))
                                viewModel.report("Experience updated successfully.")
                            }
                            showAddEditExperienceDialog = false
                            experienceToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    enabled = title.isNotBlank() && institution.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (experienceToEdit != null) {
                        TextButton(
                            onClick = {
                                val editing = experienceToEdit
                                if (editing != null) {
                                    experiences.removeAll { it.id == editing.id }
                                    coroutineScope.launch {
                                        sessionManager.updateExperienceJson(serializeExperienceList(experiences))
                                        viewModel.report("Experience removed.")
                                    }
                                }
                                showAddEditExperienceDialog = false
                                experienceToEdit = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                    TextButton(onClick = {
                        showAddEditExperienceDialog = false
                        experienceToEdit = null
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // =============================================================
    // Dialog 8: Add / Edit Education Dialog
    // =============================================================
    if (showAddEditEducationDialog) {
        var school by remember { mutableStateOf(educationToEdit?.school ?: "") }
        var degree by remember { mutableStateOf(educationToEdit?.degree ?: "") }
        var years by remember { mutableStateOf(educationToEdit?.years ?: "") }
        var details by remember { mutableStateOf(educationToEdit?.details ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddEditEducationDialog = false
                educationToEdit = null
            },
            title = {
                Text(
                    text = if (educationToEdit == null) "Add Education" else "Edit Education",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = school,
                            onValueChange = { school = it },
                            label = { Text("School / University *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = degree,
                            onValueChange = { degree = it },
                            label = { Text("Degree / Field of Study *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = years,
                            onValueChange = { years = it },
                            label = { Text("Years (e.g., 2016 - 2020)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = { Text("Details / Honors / Thesis") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (school.isNotBlank() && degree.isNotBlank()) {
                            val currentList = educations.toMutableList()
                            val editing = educationToEdit
                            if (editing != null) {
                                val index = currentList.indexOfFirst { it.id == editing.id }
                                if (index != -1) {
                                    currentList[index] = editing.copy(
                                        school = school.trim(),
                                        degree = degree.trim(),
                                        years = years.trim(),
                                        details = details.trim()
                                    )
                                }
                            } else {
                                currentList.add(
                                    EducationItem(
                                        id = UUID.randomUUID().toString(),
                                        school = school.trim(),
                                        degree = degree.trim(),
                                        years = years.trim(),
                                        details = details.trim()
                                    )
                                )
                            }
                            educations.clear()
                            educations.addAll(currentList)
                            coroutineScope.launch {
                                sessionManager.updateEducationJson(serializeEducationList(currentList))
                                viewModel.report("Education updated successfully.")
                            }
                            showAddEditEducationDialog = false
                            educationToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    enabled = school.isNotBlank() && degree.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (educationToEdit != null) {
                        TextButton(
                            onClick = {
                                val editing = educationToEdit
                                if (editing != null) {
                                    educations.removeAll { it.id == editing.id }
                                    coroutineScope.launch {
                                        sessionManager.updateEducationJson(serializeEducationList(educations))
                                        viewModel.report("Education removed.")
                                    }
                                }
                                showAddEditEducationDialog = false
                                educationToEdit = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    }
                    TextButton(onClick = {
                        showAddEditEducationDialog = false
                        educationToEdit = null
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // =============================================================
    // Dialog 9: Add Skill Dialog
    // =============================================================
    if (showAddSkillDialog) {
        var newSkillName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSkillDialog = false },
            title = {
                Text("Add Skill & Endorsement", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Add a research skill or academic proficiency to be endorsed by peers:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newSkillName,
                        onValueChange = { newSkillName = it },
                        label = { Text("Skill name (e.g. Statistical Analysis)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newSkillName.trim()
                        if (clean.isNotBlank() && skills.none { it.name.equals(clean, ignoreCase = true) }) {
                            skills.add(SkillItem(clean, 0, false))
                            coroutineScope.launch {
                                sessionManager.updateSkillsCsv(skills.joinToString(",") { it.name })
                                viewModel.report("Added skill: $clean")
                            }
                            showAddSkillDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    enabled = newSkillName.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSkillDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------
// Helper Composables for LinkedIn UI Elements
// -----------------------------------------------------------------

@Composable
private fun AnalyticsStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AnalyticsMetricTile(
    icon: ImageVector,
    value: String,
    label: String,
    subtext: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtext,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeaturedPaperCard(paper: SavedPaper, onRead: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onRead() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceInset),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = BrandBlue.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (paper.doi.isNotBlank()) "DOI VERIFIED" else "PREPRINT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${paper.repostCount} citations",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = paper.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = paper.venue.ifBlank { "Cite Circle Preprints" },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onRead,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Read PDF", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TimelineExperienceItem(
    title: String,
    institution: String,
    duration: String,
    location: String,
    description: String,
    onEdit: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BrandBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.WorkOutline, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(institution, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("$duration · $location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
            }
        }

        if (onEdit != null) {
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit position",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineEducationItem(
    school: String,
    degree: String,
    years: String,
    details: String,
    onEdit: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BrandBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.School, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(school, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(degree, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(years, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (details.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (onEdit != null) {
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit education",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactItemRow(
    icon: ImageVector,
    title: String,
    value: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
