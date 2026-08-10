package com.example.ui.people

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.people.ConnectionState
import com.example.data.people.Person
import com.example.ui.components.Avatar
import com.example.ui.components.EmptyState
import com.example.ui.navigation.avatarSharedKey
import com.example.ui.theme.AcademicField
import com.example.ui.theme.Gradients
import com.example.ui.theme.Spacing
import com.example.ui.theme.accent
import com.example.ui.theme.eyebrow
import com.example.ui.theme.meta
import com.example.ui.theme.numeric

@Composable
fun PeopleScreen(
    viewModel: PeopleViewModel,
    onMessage: (userId: String) -> Unit,
    onOpenProfile: (userId: String) -> Unit,
    modifier: Modifier = Modifier,
    fieldFilter: AcademicField? = null,
    onClearFieldFilter: () -> Unit = {},
) {
    val allPeople by viewModel.people.collectAsStateWithLifecycle()
    val suggested by viewModel.suggested.collectAsStateWithLifecycle()

    val people = fieldFilter?.let { field ->
        allPeople.filter { it.user.field == field }
    } ?: allPeople

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (fieldFilter != null) {
            item(key = "field-filter") {
                FieldFilterBar(field = fieldFilter, onClear = onClearFieldFilter)
            }
        }

        // The suggestion carousel is about breadth, so it is hidden while the list is narrowed
        // to one discipline -- otherwise it contradicts the filter sitting directly above it.
        if (fieldFilter == null && suggested.isNotEmpty()) {
            item(key = "suggested-header") {
                Text(
                    "RESEARCHERS YOU MAY KNOW",
                    style = MaterialTheme.typography.eyebrow,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = Spacing.screenHorizontal,
                        vertical = Spacing.sm,
                    ),
                )
            }
            item(key = "suggested-row") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    items(suggested, key = { it.user.id }) { person ->
                        SuggestedPersonCard(
                            person = person,
                            onConnect = { viewModel.connect(person.user.id) },
                            onClick = { onOpenProfile(person.user.id) },
                        )
                    }
                }
            }
        }

        item(key = "all-header") {
            Text(
                if (fieldFilter != null) fieldFilter.label.uppercase() else "ALL RESEARCHERS",
                style = MaterialTheme.typography.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.sm,
                ),
            )
        }

        if (people.isEmpty()) {
            item(key = "no-people") {
                EmptyState(
                    title = "No researchers here yet",
                    message = fieldFilter?.let { "Nobody in ${it.label} is on Cite Circle yet." }
                        ?: "Researcher suggestions will appear here.",
                    icon = Icons.Outlined.PersonSearch,
                )
            }
        }

        items(people, key = { it.user.id }) { person ->
            PersonRow(
                person = person,
                onConnect = { viewModel.connect(person.user.id) },
                onDisconnect = { viewModel.disconnect(person.user.id) },
                onMessage = { onMessage(person.user.id) },
                onClick = { onOpenProfile(person.user.id) },
            )
        }
    }
}

/**
 * Shows which discipline the list is narrowed to, in that discipline's own accent, with a way
 * back out. A filter the user cannot see or clear is the usual way this pattern traps people.
 */
@Composable
private fun FieldFilterBar(field: AcademicField, onClear: () -> Unit) {
    val accent = field.accent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.sm)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(accent.container)
            .padding(start = Spacing.base, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "Showing ${field.label}",
            style = MaterialTheme.typography.labelLarge,
            color = accent.onContainer,
        )
        Icon(
            Icons.Filled.Close,
            contentDescription = "Clear field filter",
            tint = accent.onContainer,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClear)
                .padding(Spacing.xs)
                .size(18.dp),
        )
    }
}

/**
 * Field-gradient banner with the avatar overlapping it. This is the most colour-forward surface
 * in the app, and the one place the per-discipline palette is doing purely visual work.
 */
@Composable
private fun SuggestedPersonCard(
    person: Person,
    onConnect: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Gradients.fieldBanner(person.user.field)),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .offset(y = (-24).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Avatar(
                initials = person.user.initials,
                seed = person.user.id,
                size = 56.dp,
                showPresence = true,
                isOnline = person.user.isOnline,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                person.user.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                person.user.affiliation,
                style = MaterialTheme.typography.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                "${person.mutualConnections} mutual",
                style = MaterialTheme.typography.meta,
                color = person.user.field.accent().base,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(vertical = Spacing.xs),
            ) {
                Text("Connect", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PersonRow(
    person: Person,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onMessage: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)

            .padding(
                horizontal = Spacing.screenHorizontal,
                vertical = Spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            initials = person.user.initials,
            seed = person.user.id,
            size = Spacing.avatarLg,
            showPresence = true,
            isOnline = person.user.isOnline,
            // Flies to the large avatar on that person's profile.
            sharedKey = avatarSharedKey(person.user.id),
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                person.user.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                person.headline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                "h-index ${person.hIndex} · ${person.publications} publications",
                style = MaterialTheme.typography.numeric,
                color = person.user.field.accent().base,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        ConnectionButton(
            state = person.connectionState,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
            onMessage = onMessage,
        )
    }
}

/** Connect -> Requested -> Connected, crossfaded so the state change is legible. */
@Composable
private fun ConnectionButton(
    state: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onMessage: () -> Unit,
) {
    AnimatedContent(targetState = state, label = "connectionState") { current ->
        when (current) {
            ConnectionState.NONE -> FilledTonalButton(
                onClick = onConnect,
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(
                    horizontal = Spacing.md,
                    vertical = Spacing.xs,
                ),
            ) {
                Icon(
                    Icons.Filled.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.width(16.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Connect", style = MaterialTheme.typography.labelMedium)
            }

            ConnectionState.PENDING -> OutlinedButton(
                onClick = onDisconnect,
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(
                    horizontal = Spacing.md,
                    vertical = Spacing.xs,
                ),
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.width(16.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Requested", style = MaterialTheme.typography.labelMedium)
            }

            ConnectionState.CONNECTED -> FilledTonalButton(
                onClick = onMessage,
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                contentPadding = PaddingValues(
                    horizontal = Spacing.md,
                    vertical = Spacing.xs,
                ),
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.width(16.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("Message", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
