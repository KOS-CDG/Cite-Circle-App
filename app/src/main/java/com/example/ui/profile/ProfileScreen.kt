package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.data.messenger.CURRENT_USER_ID
import com.example.data.people.ConnectionState
import com.example.data.people.Person
import com.example.data.prefs.ThemeMode
import com.example.ui.components.Avatar
import com.example.ui.components.CitationChart
import com.example.ui.components.PostCard
import com.example.ui.people.PeopleViewModel
import com.example.ui.theme.Gradients
import com.example.ui.theme.Spacing
import com.example.ui.theme.accent
import com.example.ui.theme.eyebrow
import com.example.ui.theme.numeric

/**
 * Serves both the signed-in user's profile and any other researcher's.
 *
 * Everything here used to be hardcoded inline -- "JD", "Dr. Jane Doe",
 * "Senior Researcher · Oxford", and a chart whose data points were a literal
 * listOf(10f, 15f, ...) with h-index 24 baked into the component's default argument.
 */
@Composable
fun ProfileScreen(
    viewModel: HomeViewModel,
    peopleViewModel: PeopleViewModel,
    userId: String = CURRENT_USER_ID,
    onMessage: (String) -> Unit = {},
) {
    val person by peopleViewModel.observePerson(userId)
        .collectAsStateWithLifecycle(initialValue = null)
    val papers by viewModel.savedPapers.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val isSelf = userId == CURRENT_USER_ID
    val isDarkMode = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val current = person ?: return

    // Own profile shows the user's own posts; someone else's shows theirs.
    val visiblePapers = if (isSelf) {
        papers.filter { it.authorId == CURRENT_USER_ID || it.authorId.isEmpty() }
    } else {
        papers.filter { it.authorId == userId }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.feedGutter),
    ) {
        item(key = "header") {
            ProfileHeader(
                person = current,
                isSelf = isSelf,
                isDarkMode = isDarkMode,
                onToggleTheme = {
                    viewModel.setThemeMode(
                        if (isDarkMode) ThemeMode.LIGHT else ThemeMode.DARK,
                    )
                },
                onConnect = { peopleViewModel.connect(current.user.id) },
                onDisconnect = { peopleViewModel.disconnect(current.user.id) },
                onMessage = {
                    peopleViewModel.openConversation(current.user.id, onMessage)
                },
            )
        }

        item(key = "chart") {
            Box(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {
                CitationChart(
                    dataPoints = current.citationSeries,
                    hIndex = current.hIndex,
                )
            }
        }

        item(key = "publications-header") {
            Text(
                "PUBLICATIONS",
                style = MaterialTheme.typography.eyebrow,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.sm,
                ),
            )
        }

        if (visiblePapers.isEmpty()) {
            item(key = "no-publications") {
                Text(
                    "No publications yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.screenHorizontal),
                )
            }
        }

        items(visiblePapers, key = { it.id }) { paper ->
            Box(modifier = Modifier.padding(horizontal = Spacing.screenHorizontal)) {
                PostCard(
                    paper = paper,
                    onReact = { reaction ->
                        viewModel.setReaction(paper.id, reaction.key, paper.myReaction)
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    person: Person,
    isSelf: Boolean,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onMessage: () -> Unit,
) {
    val accent = person.user.field.accent()

    Column {
        Box {
            // Field-gradient cover with the avatar overlapping it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Gradients.fieldBanner(person.user.field)),
            )
            if (isSelf) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Icon(
                        if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = if (isDarkMode) {
                            "Switch to light theme"
                        } else {
                            "Switch to dark theme"
                        },
                        tint = MaterialTheme.colorScheme.surface,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-40).dp)
                .padding(horizontal = Spacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .padding(4.dp),
            ) {
                Avatar(
                    initials = person.user.initials,
                    seed = person.user.id,
                    size = Spacing.avatarXl,
                    showPresence = true,
                    isOnline = person.user.isOnline,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                person.user.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                person.headline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                "${person.user.field.label} · ${person.user.affiliation}",
                style = MaterialTheme.typography.labelMedium,
                color = accent.base,
            )

            Spacer(modifier = Modifier.height(Spacing.base))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat("${person.publications}", "Publications")
                Stat("${person.citations}", "Citations")
                Stat("${person.hIndex}", "h-index")
                Stat("${person.mutualConnections}", "Mutual")
            }

            Spacer(modifier = Modifier.height(Spacing.base))
            if (!isSelf) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    when (person.connectionState) {
                        ConnectionState.NONE -> Button(
                            onClick = onConnect,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Icon(
                                Icons.Filled.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.width(18.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Connect")
                        }

                        ConnectionState.PENDING -> OutlinedButton(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Icon(
                                Icons.Filled.Schedule,
                                contentDescription = null,
                                modifier = Modifier.width(18.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Requested")
                        }

                        ConnectionState.CONNECTED -> FilledTonalButton(
                            onClick = onDisconnect,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.extraLarge,
                        ) {
                            Text("Connected")
                        }
                    }

                    FilledTonalButton(
                        onClick = onMessage,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.width(18.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Message")
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            label,
            style = MaterialTheme.typography.numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
