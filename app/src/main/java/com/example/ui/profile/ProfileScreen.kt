package com.example.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.HomeViewModel
import com.example.data.prefs.ThemeMode
import com.example.ui.components.CitationChart
import com.example.ui.components.PostCard
import com.example.ui.theme.Gradients
import com.example.ui.theme.Spacing
import com.example.ui.theme.eyebrow

@Composable
fun ProfileScreen(viewModel: HomeViewModel) {
    val papers by viewModel.savedPapers.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isDarkMode = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(Spacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(Spacing.feedGutter),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(Spacing.avatarLg)
                            .clip(CircleShape)
                            .background(Gradients.avatarFallback("jane-doe")),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "JD",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.base))
                    Column {
                        Text(
                            "Dr. Jane Doe",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Senior Researcher · Oxford",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = {
                        viewModel.setThemeMode(
                            if (isDarkMode) ThemeMode.LIGHT else ThemeMode.DARK,
                        )
                    },
                ) {
                    Icon(
                        if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = if (isDarkMode) "Switch to light theme" else "Switch to dark theme",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
            CitationChart()
            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                "PUBLICATIONS",
                style = MaterialTheme.typography.eyebrow,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        items(papers, key = { it.id }) { paper ->
            PostCard(
                paper = paper,
                onReact = { reaction ->
                    viewModel.setReaction(paper.id, reaction.key, paper.myReaction)
                },
            )
        }
    }
}
