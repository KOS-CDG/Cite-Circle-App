package com.example.ui.settings

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.HomeViewModel
import com.example.MyApplication
import com.example.data.AuthorIdentity
import com.example.data.auth.AuthResult
import com.example.data.auth.UserAccount
import kotlinx.coroutines.launch

/**
 * Settings and Privacy interface structured identically to Facebook/Meta settings,
 * featuring an Accounts Center, personal details editor, password & security controls,
 * preferences, and dedicated Facebook-style logout and profile switching.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val sessionManager = app.sessionManager
    val scope = rememberCoroutineScope()

    val currentName by sessionManager.currentUserName.collectAsStateWithLifecycle(initialValue = "")
    val currentEmail by sessionManager.currentUserEmail.collectAsStateWithLifecycle(initialValue = "")
    val currentAffiliation by sessionManager.currentUserAffiliation.collectAsStateWithLifecycle(initialValue = "")
    val currentField by sessionManager.currentUserField.collectAsStateWithLifecycle(initialValue = "")
    val rememberLogin by sessionManager.rememberLoginInfo.collectAsStateWithLifecycle(initialValue = true)
    val savedAccounts by sessionManager.getAllSavedAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showPersonalDetailsDialog by remember { mutableStateOf(false) }
    var showPasswordSecurityDialog by remember { mutableStateOf(false) }
    var showFacebookLogoutDialog by remember { mutableStateOf(false) }
    var showSwitchAccountDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showHelpSupportDialog by remember { mutableStateOf(false) }
    var showAboutAppDialog by remember { mutableStateOf(false) }
    var showPatchNotesDialog by remember { mutableStateOf(false) }

    var dataSaverEnabled by remember { mutableStateOf(false) }
    var citationAlertsEnabled by remember { mutableStateOf(true) }
    var cachePaperCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        cachePaperCount = app.repository.countAllPapers()
    }

    val authorInitials = remember(currentName) {
        if (currentName.isNotBlank()) AuthorIdentity.initialsOf(currentName) else "AR"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Privacy",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Facebook-Style Settings Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search settings and privacy...", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
            }

            // 2. Meta-Style Accounts Center Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Cite Circle Accounts Center",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Meta Architecture",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Manage your connected profile, personal academic details, password, and security across Cite Circle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        // Active Profile Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showPersonalDetailsDialog = true }
                                .padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    authorInitials,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    currentName.ifBlank { "Dr. Alex Rivera" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    currentEmail.ifBlank { "alex.rivera@citecircle.edu" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))

                        // Accounts Center Sub-Options
                        FacebookSettingRow(
                            icon = Icons.Outlined.Person,
                            title = "Personal details",
                            subtitle = "${currentAffiliation.ifBlank { "Stanford AI" }} • ${currentField.ifBlank { "AI & ML" }}",
                            onClick = { showPersonalDetailsDialog = true }
                        )

                        FacebookSettingRow(
                            icon = Icons.Outlined.Lock,
                            title = "Password and security",
                            subtitle = "Change password, security alerts, device memory",
                            onClick = { showPasswordSecurityDialog = true }
                        )

                        FacebookSettingRow(
                            icon = Icons.Outlined.Description,
                            title = "Your information and permissions",
                            subtitle = "Cached preprints ($cachePaperCount papers), BibTeX export",
                            onClick = { navController.navigate("privacy_policy") }
                        )
                    }
                }
            }

            // 3. Preferences Section (Facebook Style)
            item {
                SectionHeader("Preferences")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        // Dark Mode Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Dark Mode", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))
                                    Text(if (isDarkMode) "On" else "Off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.toggleTheme() },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Data Saver Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.DataUsage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Media & Data Saver", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))
                                    Text("Limit 50 MB PDF downloads to Wi-Fi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = dataSaverEnabled,
                                onCheckedChange = { dataSaverEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Notifications
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Citation & Review Alerts", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))
                                    Text("Receive citation badges and mentions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = citationAlertsEnabled,
                                onCheckedChange = { citationAlertsEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            // 4. Privacy & Governance Section
            item {
                SectionHeader("Audience and Visibility")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        FacebookSettingRow(
                            icon = Icons.Outlined.PrivacyTip,
                            title = "Privacy Policy & Sandboxed Storage",
                            subtitle = "View permissions, anti-malware rules, and local cache",
                            onClick = { navController.navigate("privacy_policy") }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        FacebookSettingRow(
                            icon = Icons.Outlined.Badge,
                            title = "Terms of Service & Academic Governance",
                            subtitle = "Peer review code of conduct and DOI citations",
                            onClick = { showTermsDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        FacebookSettingRow(
                            icon = Icons.Outlined.DeleteForever,
                            title = "Account Ownership and Control",
                            subtitle = "Deactivate account or execute GDPR data erasure",
                            onClick = { showDeleteAccountDialog = true },
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 5. Community & Support
            item {
                SectionHeader("Community & Legal")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        FacebookSettingRow(
                            icon = Icons.AutoMirrored.Outlined.HelpOutline,
                            title = "Help & Support Center",
                            subtitle = "Troubleshooting, manuscript submission guidelines",
                            onClick = { showHelpSupportDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        FacebookSettingRow(
                            icon = Icons.Outlined.Info,
                            title = "About Cite Circle",
                            subtitle = "Version ${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                            onClick = { showAboutAppDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        FacebookSettingRow(
                            icon = Icons.Outlined.Description,
                            title = "Release Notes & Patch Changelog",
                            subtitle = "View what's new in version ${com.example.BuildConfig.VERSION_NAME}",
                            onClick = { showPatchNotesDialog = true }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        val isCheckingUpdates by viewModel.isCheckingUpdates.collectAsState()
                        FacebookSettingRow(
                            icon = Icons.Rounded.SystemUpdate,
                            title = "Check for Updates",
                            subtitle = if (isCheckingUpdates) "Checking for updates..." else "Currently on v${com.example.BuildConfig.VERSION_NAME}",
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
            }

            // 6. Facebook-Style Login & Logout Controls (Distinct Bottom Section)
            item {
                SectionHeader("Login & Sessions")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Switch Profile / Add Another Account (Classic Facebook Feature)
                    OutlinedButton(
                        onClick = { showSwitchAccountDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Outlined.SwitchAccount, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Switch Account (${savedAccounts.size} saved profiles)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Iconic Facebook Full-Width "Log Out" Button
                    Button(
                        onClick = { showFacebookLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Log Out ${currentName.ifBlank { "Account" }}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    // Log Out of All Sessions
                    TextButton(
                        onClick = { showFacebookLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Log out of all active sessions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ==========================================
    // DIALOGS & EDITORS (Facebook Style)
    // ==========================================

    // 1. Edit Personal Details Dialog
    if (showPersonalDetailsDialog) {
        var editName by remember { mutableStateOf(currentName) }
        var editAffiliation by remember { mutableStateOf(currentAffiliation) }
        var editField by remember { mutableStateOf(currentField) }
        var isSaving by remember { mutableStateOf(false) }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showPersonalDetailsDialog = false },
            title = {
                Text(
                    "Personal Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Update your academic identity across Cite Circle mobile and cloud sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (errorText != null) {
                        Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editAffiliation,
                        onValueChange = { editAffiliation = it },
                        label = { Text("Academic Institution / University") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editField,
                        onValueChange = { editField = it },
                        label = { Text("Research Field / Specialty") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        errorText = null
                        scope.launch {
                            val res = sessionManager.updateProfile(editName, editAffiliation, editField)
                            isSaving = false
                            if (res is AuthResult.Success) {
                                showPersonalDetailsDialog = false
                            } else if (res is AuthResult.Error) {
                                errorText = res.message
                            }
                        }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    else Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPersonalDetailsDialog = false }, enabled = !isSaving) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Change Password Dialog
    if (showPasswordSecurityDialog) {
        var oldPassword by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var oldVisible by remember { mutableStateOf(false) }
        var newVisible by remember { mutableStateOf(false) }
        var isSubmitting by remember { mutableStateOf(false) }
        var errorMsg by remember { mutableStateOf<String?>(null) }
        var successMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showPasswordSecurityDialog = false },
            title = {
                Text("Password & Security", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Choose a strong password with at least 6 characters.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (errorMsg != null) {
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (successMsg != null) {
                        Text(successMsg!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = if (oldVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { oldVisible = !oldVisible }) {
                                Icon(if (oldVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password (min 6 chars)") },
                        visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newVisible = !newVisible }) {
                                Icon(if (newVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPassword != confirmPassword) {
                            errorMsg = "New passwords do not match."
                            return@Button
                        }
                        isSubmitting = true
                        errorMsg = null
                        scope.launch {
                            val res = sessionManager.changePassword(oldPassword, newPassword)
                            isSubmitting = false
                            if (res is AuthResult.Success) {
                                successMsg = "Password updated successfully!"
                                showPasswordSecurityDialog = false
                            } else if (res is AuthResult.Error) {
                                errorMsg = res.message
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    else Text("Update Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordSecurityDialog = false }, enabled = !isSubmitting) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Facebook-Style Dedicated Logout Dialog
    if (showFacebookLogoutDialog) {
        var keepCredentialsChecked by remember { mutableStateOf(rememberLogin) }

        AlertDialog(
            onDismissRequest = { showFacebookLogoutDialog = false },
            title = {
                Text("Log Out?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile Avatar & Name
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(authorInitials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }

                    Text(
                        currentName.ifBlank { "Dr. Alex Rivera" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "Are you sure you want to log out of your Cite Circle academic account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Facebook-style "Remember login info on this device" Checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { keepCredentialsChecked = !keepCredentialsChecked }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = keepCredentialsChecked,
                            onCheckedChange = { keepCredentialsChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Remember login info", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Fast sign-in without retyping credentials", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFacebookLogoutDialog = false
                        scope.launch {
                            sessionManager.setRememberLoginInfo(keepCredentialsChecked)
                            sessionManager.signOut(keepSavedOnDevice = keepCredentialsChecked)
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFacebookLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Switch Account Dialog (Multi-Profile)
    if (showSwitchAccountDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchAccountDialog = false },
            title = { Text("Switch Accounts", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a saved researcher profile or add another account:", style = MaterialTheme.typography.bodySmall)

                    savedAccounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    showSwitchAccountDialog = false
                                    scope.launch {
                                        sessionManager.switchAccount(account.email)
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (account.email == currentEmail) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(AuthorIdentity.initialsOf(account.displayName), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(account.displayName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text(account.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (account.email == currentEmail) {
                                Icon(Icons.Filled.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSwitchAccountDialog = false
                    navController.navigate("auth")
                }) {
                    Text("+ Add Another Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchAccountDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // 5. Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service & Code of Conduct") },
            text = {
                Text(
                    "Cite Circle is committed to transparent peer review, academic integrity, and safe preprint dissemination. " +
                    "All manuscripts are inspected with anti-malware safeguards. Do not upload copyrighted or prohibited archives.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Close") }
            }
        )
    }

    // 6. Delete Account Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account & Wipe Data?") },
            text = {
                Text(
                    "This executes your GDPR Right to Erasure. Your local credentials, bookmarks, and sessions will be permanently deleted from this device and Firebase Auth.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        scope.launch {
                            sessionManager.deleteAccount()
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 7. Help & Support Dialog
    if (showHelpSupportDialog) {
        AlertDialog(
            onDismissRequest = { showHelpSupportDialog = false },
            title = {
                Text(
                    "Help & Support Center",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Frequently asked questions and guides for manuscript sharing and reading circles:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("• BibTeX & Citation Export", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("Tap the overflow icon on any paper to copy full BibTeX records formatted according to IEEE/ACM standards.", style = MaterialTheme.typography.bodySmall)

                        Text("• Offline Storage & Vault", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("Papers saved to the Vault are stored locally in Room v5 database and accessible with zero network connectivity.", style = MaterialTheme.typography.bodySmall)

                        Text("• Peer Collaboration & Reviews", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("Use Messenger to launch encrypted academic discussions with co-authors and cited researchers.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpSupportDialog = false }) { Text("Got it") }
            }
        )
    }

    // 8. About Cite Circle Dialog
    if (showAboutAppDialog) {
        AlertDialog(
            onDismissRequest = { showAboutAppDialog = false },
            title = {
                Text(
                    "About Cite Circle",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Cite Circle for Android",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Version: ${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})")
                    Text("Design System: Meta Android Architecture (Facebook / Messenger)")
                    Text("Storage: Room v5 (Offline SQLite) + Supabase Realtime + Cloudflare R2")
                    Text("AI Intelligence: Google Gemini 2.5 Flash + Search Grounding")
                    Text("UI Framework: Jetpack Compose + Material 3")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Cite Circle is built for researchers, scholars, and peer reviewers to collaborate without algorithmic bloat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutAppDialog = false }) { Text("Close") }
            }
        )
    }

    // 9. Release Notes & Patch Changelog Dialog
    if (showPatchNotesDialog) {
        AlertDialog(
            onDismissRequest = { showPatchNotesDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "What's New in v${com.example.BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Build ${com.example.BuildConfig.VERSION_CODE} · Latest Release",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "Current Configuration & Improvements:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val notes = listOf(
                        "🤖 Gemini AI Assistant" to "Powered by Gemini 2.5 Flash (default), 3.5 Flash & 3.1 Flash Lite with real-time Google Search Grounding for verified literature citations and arXiv synthesis.",
                        "🖼️ Multimodal Vision Inspection" to "Instant diagram, figure, chart, and mathematical formula transcription from camera or uploaded research manuscripts.",
                        "🔔 Real-Time Activity Alerts" to "Instant WebSocket notifications & unread badge counters for paper endorsements, peer reviews, comments, and researcher direct messages.",
                        "🔄 Smart In-App Updater" to "Silent background startup verification when up to date, manual update checks in Settings, and one-tap background APK downloads with progress indicator.",
                        "📄 CrossRef DOI Resolver" to "Live metadata fetching and academic citation formatting in BibTeX, APA, IEEE, and MLA formats.",
                        "🛡️ Stability & Resilience" to "Android 9+ hardware bitmap memory safety, automatic 3-attempt exponential backoff retries, and Cloudflare R2 decentralized vault."
                    )
                    items(notes.size) { idx ->
                        val (title, desc) = notes[idx]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPatchNotesDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
private fun FacebookSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
