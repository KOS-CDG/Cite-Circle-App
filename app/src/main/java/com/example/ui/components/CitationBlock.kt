package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val citationFormats = listOf("APA", "MLA", "CHICAGO")

/**
 * The dark citation panel with format tabs and export buttons.
 *
 * Format switching is presentational string surgery on the stored APA citation, not real
 * bibliographic conversion, and the export buttons acknowledge the action with a toast rather
 * than writing a file. Both are known placeholders.
 */
@Composable
fun CitationBlock(citation: String, modifier: Modifier = Modifier) {
    var format by remember { mutableStateOf(citationFormats.first()) }
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CiteCircleDefaults.CardShape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(20.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "CITATION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                    ),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                )

                Row {
                    citationFormats.forEach { candidate ->
                        val isSelected = format == candidate
                        Text(
                            candidate,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .clickable { format = candidate }
                                .border(
                                    1.dp,
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                    } else {
                                        Color.Transparent
                                    },
                                    RoundedCornerShape(2.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                formatCitation(citation, format),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp,
                ),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf("BibTeX", "RIS").forEach { exportFormat ->
                    OutlinedButton(
                        onClick = {
                            android.widget.Toast.makeText(
                                context,
                                "Exported as $exportFormat",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(2.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = "Export as $exportFormat",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            exportFormat,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun formatCitation(citation: String, format: String): String = when (format) {
    "MLA" -> citation.replace(" (2026). ", ". ")
    "CHICAGO" -> citation.replace(" (2026).", ", 2026.")
    else -> citation
}
