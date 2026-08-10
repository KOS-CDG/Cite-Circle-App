package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.data.prefs.ThemeMode
import com.example.ui.theme.Spacing
import com.example.ui.theme.eyebrow
import androidx.compose.material3.IconButton

/**
 * The theme control used to be a single icon button floating on the profile's cover banner, which
 * had two problems.
 *
 * It was a **one-way door**: SettingsStore persists three states and defaults to SYSTEM, but a
 * two-state toggle could only ever flip LIGHT and DARK. Once tapped, "follow the system" was
 * unreachable for the life of the install short of clearing app data. Three segments fix that.
 *
 * And a cover banner is not where anyone looks for settings. This screen gives the app somewhere
 * to put the next one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal),
        ) {
            SectionHeader("APPEARANCE")

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size,
                        ),
                        icon = {
                            Icon(
                                mode.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        label = {
                            Text(mode.label(), style = MaterialTheme.typography.labelMedium)
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                when (themeMode) {
                    ThemeMode.SYSTEM -> "Following your device's light and dark setting."
                    ThemeMode.LIGHT -> "Always light, regardless of the device setting."
                    ThemeMode.DARK -> "Always dark, regardless of the device setting."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))
            SectionHeader("ABOUT THIS BUILD")

            // Stated in the app rather than only in a README. Every one of these is a real limit
            // someone will otherwise discover by hitting it and assuming something is broken.
            InfoCard(
                lines = listOf(
                    "Sign-in is not configured, so the auth screen continues straight into the app.",
                    "Messages, people, notifications and reading lists are seeded mockups held in " +
                        "memory. They reset when the app process is killed.",
                    "Posts and comments are the only data that persists, via Room.",
                ),
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = Spacing.md),
    )
}

@Composable
private fun InfoCard(lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            lines.forEach { line ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(Spacing.md))
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun ThemeMode.icon(): ImageVector = when (this) {
    ThemeMode.SYSTEM -> Icons.Outlined.Brightness4
    ThemeMode.LIGHT -> Icons.Outlined.LightMode
    ThemeMode.DARK -> Icons.Outlined.DarkMode
}
