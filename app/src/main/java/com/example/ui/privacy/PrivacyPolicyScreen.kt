package com.example.ui.privacy

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MyApplication
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val scope = rememberCoroutineScope()

    var showWipeConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var cachedPapersCount by remember { mutableIntStateOf(0) }
    var vaultPapersCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        cachedPapersCount = app.repository.countAllPapers()
        vaultPapersCount = app.repository.countVaultPapers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Privacy & Data Governance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                "Cite Circle Privacy Policy & Terms",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Effective Date: September 2026 • Version 2.4",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            PrivacySectionCard(
                icon = Icons.Outlined.VerifiedUser,
                title = "1. Academic Identity & Data Ownership",
                body = "Cite Circle is built for scholars, researchers, and students. When you create an account, we record your academic display name, verified institution or university affiliation, and primary research field. You retain complete intellectual ownership of all commentaries, notes, and academic posts created within the app."
            )

            Spacer(Modifier.height(12.dp))

            PrivacySectionCard(
                icon = Icons.Outlined.Storage,
                title = "2. Local-First Storage & Database Limiters",
                body = "All research papers (PDF, Word DOCX, text manuscripts) and citation metadata are cached locally on your device in sandboxed application storage (vault_pdfs). To prevent device freezing or unbounded memory usage, Cite Circle employs automated database cache limiters (max 200 items retention policy with automatic eviction of unbookmarked items)."
            )

            Spacer(Modifier.height(12.dp))

            PrivacySectionCard(
                icon = Icons.Outlined.Security,
                title = "3. Anti-Malware & File Integrity Protections",
                body = "To prevent distribution of malicious software or trojans in academic circles, uploaded manuscripts are checked for magic bytes (blocking executable PE/MZ and ELF binaries, shell scripts, and disguised ZIP archives). File inspection is conducted via local stream analysis with a strict 50 MB payload limiter to protect bandwidth and device stability."
            )

            Spacer(Modifier.height(12.dp))

            PrivacySectionCard(
                icon = Icons.Outlined.Notifications,
                title = "4. Permissions Disclosure",
                body = "• Post Notifications (POST_NOTIFICATIONS): Required on Android 13+ to alert you when peers endorse your research, reply in discussion threads, or send academic messages.\n• Document Picker: Operates via Android's secure Storage Access Framework (SAF), granting read access solely to the individual papers you choose to attach."
            )

            Spacer(Modifier.height(12.dp))

            PrivacySectionCard(
                icon = Icons.Outlined.Lock,
                title = "5. Your Rights (GDPR & Data Erasure)",
                body = "You have the unconditional right to access your stored data, export your reading library in BibTeX/RIS format, clear all local cached papers, or permanently erase your profile and credentials from this device."
            )

            Spacer(Modifier.height(24.dp))

            Text(
                "Data Governance Actions",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Current database: $cachedPapersCount posts cached, $vaultPapersCount vault documents attached.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { showWipeConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(Icons.Outlined.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear Local Cache ($cachedPapersCount papers)")
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { showDeleteAccountDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete Account & Wipe All Local Data")
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            title = { Text("Clear Local Cache?") },
            text = {
                Text("This will purge all unbookmarked cached papers and temporary vault files to free device storage. Bookmarked papers and account details will remain safe.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeConfirmDialog = false
                        scope.launch {
                            app.repository.clearUnbookmarkedCache()
                            cachedPapersCount = app.repository.countAllPapers()
                            vaultPapersCount = app.repository.countVaultPapers()
                            Toast.makeText(context, "Local cache cleared successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear Cache", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account & Wipe Data?") },
            text = {
                Text("Are you sure you want to delete your Cite Circle account and wipe all stored data from this device? This action is permanent and complies with GDPR Right to Erasure.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        scope.launch {
                            app.sessionManager.deleteAccount()
                            app.repository.clearUnbookmarkedCache()
                            Toast.makeText(context, "Account and local data wiped", Toast.LENGTH_LONG).show()
                            onAccountDeleted()
                        }
                    }
                ) {
                    Text("Delete Everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PrivacySectionCard(
    icon: ImageVector,
    title: String,
    body: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
