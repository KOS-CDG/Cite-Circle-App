package com.example.ui.opportunities

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.DetailRow
import com.example.ui.components.ErrorState
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionDivider
import com.example.ui.components.SectionLabel

@Composable
fun OpportunityDetailScreen(
    opportunityId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val opportunity = SampleData.opportunityById(opportunityId)
    val context = LocalContext.current
    var isSaved by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(
            title = "OPPORTUNITY",
            onBack = onBack,
            actions = {
                if (opportunity != null) {
                    IconButton(onClick = { isSaved = !isSaved }) {
                        Icon(
                            Icons.Filled.BookmarkBorder,
                            contentDescription = if (isSaved) {
                                "Remove from saved opportunities"
                            } else {
                                "Save this opportunity"
                            },
                            tint = if (isSaved) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                        )
                    }
                }
            },
        )

        if (opportunity == null) {
            ErrorState(
                title = "Listing Not Found",
                message = "This opportunity has closed or been withdrawn.",
                actionLabel = "GO BACK",
                onAction = onBack,
            )
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(CiteCircleDefaults.ScreenPadding)) {
            item {
                Text(
                    opportunity.type.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    opportunity.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    opportunity.institution,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    opportunity.deadline,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SectionDivider()

                DetailRow(label = "ABOUT", value = opportunity.description)

                SectionDivider()

                SectionLabel("ELIGIBILITY")
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(opportunity.eligibility.size) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Spacer(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        opportunity.eligibility[index],
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp,
                    )
                }
            }

            item {
                SectionDivider()

                DetailRow(label = "CONTACT", value = opportunity.contact)

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Application flow is not available in this preview",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CiteCircleDefaults.ButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        "START APPLICATION",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}
