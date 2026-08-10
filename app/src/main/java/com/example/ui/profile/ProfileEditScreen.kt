package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionLabel
import com.example.ui.lists.editorFieldColors

/**
 * Edit the signed-in researcher's public details.
 *
 * Reuses the existing [ProfileViewModel], which already reads and writes a Firestore document
 * for the current user, rather than introducing a second auth-aware view model.
 */
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: ProfileViewModel = viewModel(),
) {
    val identity = rememberResearcherIdentity()
    val context = LocalContext.current
    val storedNote by profileViewModel.userData.collectAsStateWithLifecycle()

    var displayName by remember { mutableStateOf(identity.name) }
    var affiliation by remember { mutableStateOf(identity.affiliation) }
    var bio by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(title = "EDIT PROFILE", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(CiteCircleDefaults.ScreenPadding),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                InitialsAvatar(
                    initialsOf(displayName.ifBlank { identity.name }),
                    size = 80.dp,
                    fontSize = 28.sp,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            SectionLabel("DISPLAY NAME")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = CiteCircleDefaults.CardShape,
                colors = editorFieldColors(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("AFFILIATION")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = affiliation,
                onValueChange = { affiliation = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Senior Researcher • Oxford") },
                singleLine = true,
                shape = CiteCircleDefaults.CardShape,
                colors = editorFieldColors(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("RESEARCH NOTE")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                placeholder = { Text(storedNote) },
                shape = CiteCircleDefaults.CardShape,
                colors = editorFieldColors(),
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (bio.isNotBlank()) profileViewModel.saveUserData(bio)
                    android.widget.Toast.makeText(
                        context,
                        "Profile updated",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                    onBack()
                },
                enabled = displayName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = CiteCircleDefaults.ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    "SAVE CHANGES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
