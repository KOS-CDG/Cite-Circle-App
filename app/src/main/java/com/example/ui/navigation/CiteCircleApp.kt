package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.HomeViewModel
import com.example.ui.assistant.AssistantScreen
import com.example.ui.discover.DiscoverScreen
import com.example.ui.feed.FeedScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.notifications.NotificationDetailScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.profile.ProfileScreen

@Composable
fun CiteCircleApp(viewModel: HomeViewModel) {
    val navController = rememberNavController()

    // Replaces a `var currentRoute by remember` mirrored via addOnDestinationChangedListener.
    // That listener was registered on EVERY recomposition and never removed, so listeners
    // accumulated without bound.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showChrome = currentRoute != null && currentRoute !in Routes.chromeless

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showChrome) {
                CiteCircleTopBar(
                    onNotificationsClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onAssistantClick = { navController.navigate(Routes.ASSISTANT) },
                )
            }
        },
        bottomBar = {
            if (showChrome) {
                BottomNavBar(
                    currentDestination = currentDestination,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // popUpTo the graph's actual start destination rather than a
                            // hardcoded "feed".
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.FEED,
            modifier = Modifier.padding(innerPadding),
        ) {
            // Not the start destination and nothing navigates here today -- sign-in is bypassed
            // (AuthScreen calls onAuthSuccess() in its failure branch) and google-services.json
            // is a placeholder. Kept registered so the screen is not orphaned.
            composable(Routes.AUTH) {
                com.example.ui.auth.AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Routes.FEED) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.FEED) { FeedScreen(viewModel) }
            composable(Routes.DISCOVER) { DiscoverScreen() }
            composable(Routes.LIBRARY) { LibraryScreen() }
            composable(Routes.PROFILE) { ProfileScreen(viewModel) }

            composable(Routes.ASSISTANT) { AssistantScreen() }

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onOpenDetail = { navController.navigate(Routes.NOTIFICATION_DETAIL) },
                )
            }
            composable(Routes.NOTIFICATION_DETAIL) {
                NotificationDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
