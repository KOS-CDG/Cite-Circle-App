package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Gradients
import com.example.ui.theme.Spacing
import com.example.ui.theme.citation
import com.example.ui.theme.eyebrowTight

private val formats = listOf("APA", "MLA", "CHICAGO")

@Composable
fun CitationBlock(citation: String) {
    var format by remember { mutableStateOf("APA") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Gradients.citationPanel())
            .padding(Spacing.lg),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "CITATION",
                    style = MaterialTheme.typography.eyebrowTight,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    formats.forEach { f ->
                        val selected = format == f
                        Text(
                            f,
                            style = MaterialTheme.typography.eyebrowTight,
                            color = if (selected) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            },
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { format = f }
                                .border(
                                    width = 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            val displayCitation = formatCitation(citation, format)
            Text(
                displayCitation,
                style = MaterialTheme.typography.citation,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
            )

            Spacer(modifier = Modifier.height(Spacing.base))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(Spacing.base))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                // These were Toast-only stubs that claimed to export. They now actually put
                // something on the clipboard.
                CitationExportButton(
                    label = "Copy",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        clipboard.setText(AnnotatedString(displayCitation))
                        Toast.makeText(context, "Citation copied", Toast.LENGTH_SHORT).show()
                    },
                )
                CitationExportButton(
                    label = "BibTeX",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        clipboard.setText(AnnotatedString(toBibTeX(citation)))
                        Toast.makeText(context, "BibTeX copied", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}

@Composable
private fun CitationExportButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = MaterialTheme.shapes.extraSmall,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Reformats between citation styles.
 *
 * This is the same naive string substitution the original shipped: it only rearranges the year,
 * and it only matches a hardcoded "2026". Preserved as-is rather than quietly rewritten, because
 * doing it properly means parsing structured citation fields, which the data model does not have
 * yet -- SavedPaper.citation is a single free-text string.
 */
private fun formatCitation(citation: String, format: String): String = when (format) {
    "MLA" -> citation.replace(" (2026). ", ". ")
    "CHICAGO" -> citation.replace(" (2026).", ", 2026.")
    else -> citation
}

private fun toBibTeX(citation: String): String =
    "@article{citecircle,\n  note = {$citation}\n}"
