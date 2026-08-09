package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy

/**
 * A tab. The old implementation used a Triple and derived the label from the route string with
 * `route.replaceFirstChar { it.uppercase() }`, which is why the fourth tab literally read "Opps".
 */
data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * Fields, Reading Lists and Opportunities used to be three separate tabs. Fields is now inside
 * Discover (which gains People in a later phase) and Lists/Opportunities share Library, which
 * frees a slot for Messages without going to six tabs.
 */
val bottomNavItems = listOf(
    NavItem(Routes.FEED, "Feed", Icons.Filled.Home, Icons.Outlined.Home),
    NavItem(Routes.DISCOVER, "Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
    NavItem(Routes.MESSAGES, "Messages", Icons.Filled.Forum, Icons.Outlined.Forum),
    NavItem(Routes.LIBRARY, "Library", Icons.AutoMirrored.Filled.LibraryBooks, Icons.AutoMirrored.Outlined.LibraryBooks),
    NavItem(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)

@Composable
fun BottomNavBar(
    currentDestination: NavDestination?,
    onNavigate: (String) -> Unit,
    unreadMessages: Int = 0,
) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            // No windowInsetsPadding here: NavigationBar already consumes the navigation-bar
            // inset through its own default windowInsets, and adding it again double-padded
            // the bar.
        ) {
            bottomNavItems.forEach { item ->
                // Hierarchy match rather than an equality check on the route string, so nested
                // and parameterized children keep their parent tab lit.
                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                NavigationBarItem(
                    icon = {
                        val badgeCount =
                            if (item.route == Routes.MESSAGES) unreadMessages else 0
                        BadgedBox(
                            badge = {
                                if (badgeCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary,
                                    ) {
                                        Text("$badgeCount")
                                    }
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (selected) {
                                    item.selectedIcon
                                } else {
                                    item.unselectedIcon
                                },
                                contentDescription = item.label,
                            )
                        }
                    },
                    label = {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
