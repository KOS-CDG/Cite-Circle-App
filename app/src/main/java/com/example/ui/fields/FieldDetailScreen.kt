package com.example.ui.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.ErrorState
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionDivider
import com.example.ui.components.SectionLabel
import com.example.ui.components.StatTile

@Composable
fun FieldDetailScreen(
    fieldId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val field = SampleData.fieldById(fieldId)
    var isFollowing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(title = "FIELD", onBack = onBack)

        if (field == null) {
            ErrorState(
                title = "Field Not Found",
                message = "This academic field is no longer part of the registry.",
                actionLabel = "GO BACK",
                onAction = onBack,
            )
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(CiteCircleDefaults.ScreenPadding),
        ) {
            item {
                Text(
                    field.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    field.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp,
                )

                SectionDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    StatTile(label = "Researchers", value = "${field.researcherCount}")
                    StatTile(label = "Active topics", value = "${field.activeTopics.size}")
                }

                SectionDivider()

                SectionLabel("ACTIVE TOPICS")
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(field.activeTopics.size) { index ->
                val topic = field.activeTopics[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(CiteCircleDefaults.CardShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(CiteCircleDefaults.cardBorder(), CiteCircleDefaults.CardShape)
                        .padding(16.dp),
                ) {
                    Text(
                        topic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                if (isFollowing) {
                    OutlinedButton(
                        onClick = { isFollowing = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CiteCircleDefaults.ButtonShape,
                        border = CiteCircleDefaults.cardBorder(alpha = 0.2f),
                    ) {
                        Text(
                            "FOLLOWING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Button(
                        onClick = { isFollowing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CiteCircleDefaults.ButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(
                            "FOLLOW THIS FIELD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
