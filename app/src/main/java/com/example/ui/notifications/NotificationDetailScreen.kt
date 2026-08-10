package com.example.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.components.ErrorState
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.QuoteBlock
import com.example.ui.components.ScreenHeader
import com.example.ui.components.SectionDivider
import com.example.ui.components.SectionLabel

@Composable
fun NotificationDetailScreen(
    notificationId: String?,
    onBack: () -> Unit,
    onViewPaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notification = SampleData.notificationById(notificationId)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenHeader(title = "CITATION DETAILS", onBack = onBack)

        if (notification == null) {
            ErrorState(
                title = "Citation Not Found",
                message = "This notification is no longer available.",
                actionLabel = "GO BACK",
                onAction = onBack,
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(CiteCircleDefaults.ScreenPadding),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = CiteCircleDefaults.CardShape,
                        spotColor = Color(0x0D1A1A1A),
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                shape = CiteCircleDefaults.CardShape,
                border = CiteCircleDefaults.cardBorder(),
            ) {
                Column(modifier = Modifier.padding(CiteCircleDefaults.ScreenPadding)) {
                    SectionLabel(
                        "CITATION CONTEXT",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    QuoteBlock(notification.quote)

                    SectionDivider()

                    SectionLabel("CITING AUTHOR")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(
                            notification.citingAuthorInitials,
                            size = 40.dp,
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                notification.citingAuthorName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${notification.citingAffiliation} • ${notification.citingField}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    SectionDivider()

                    SectionLabel("REFERENCED SECTION")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        notification.referencedSection,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(CiteCircleDefaults.ScreenPadding))

                    Button(
                        onClick = onViewPaper,
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
                            "VIEW FULL PAPER",
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
