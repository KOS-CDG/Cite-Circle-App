package com.example.ui.compose

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.HomeViewModel
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionLabel
import com.example.ui.lists.editorFieldColors
import com.example.ui.profile.rememberResearcherIdentity

/**
 * Publish a preprint to the local registry.
 *
 * This is the first caller of [HomeViewModel.publishPaper] — before this screen existed the
 * save path was written but unreachable, so nothing in the app could create a paper.
 */
@Composable
fun ComposePaperScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val identity = rememberResearcherIdentity()
    val context = LocalContext.current

    var abstract by remember { mutableStateOf("") }
    var citation by remember { mutableStateOf("") }
    var affiliation by remember { mutableStateOf(identity.affiliation) }

    val canPublish = abstract.isNotBlank() && citation.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(title = "NEW PREPRINT", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(CiteCircleDefaults.ScreenPadding),
        ) {
            SectionLabel("ABSTRACT")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = abstract,
                onValueChange = { abstract = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = { Text("What did you find, and why does it matter?") },
                shape = CiteCircleDefaults.CardShape,
                colors = editorFieldColors(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("CITATION")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = citation,
                onValueChange = { citation = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        "Doe, J. (2026). Title. Folio Preprints, CC-000-XX.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                shape = CiteCircleDefaults.CardShape,
                colors = editorFieldColors(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Enter the APA form. The card renders MLA and Chicago from it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionLabel("AFFILIATION")
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = affiliation,
                onValueChange = { affiliation = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = CiteCircleDefaults.CardShape,
                colors = editorFieldColors(),
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    viewModel.publishPaper(
                        content = abstract.trim(),
                        citation = citation.trim(),
                        authorName = identity.name,
                        authorInitials = identity.initials,
                        affiliation = affiliation.trim().ifBlank { identity.affiliation },
                    )
                    android.widget.Toast.makeText(
                        context,
                        "Preprint published",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                    onBack()
                },
                enabled = canPublish,
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
                    "PUBLISH",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(modifier = Modifier.height(CiteCircleDefaults.ScreenPadding))
        }
    }
}
