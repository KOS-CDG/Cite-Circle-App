package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.HomeViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.compose.ComposePaperScreen
import com.example.ui.feed.HomeScreen
import com.example.ui.fields.FieldDetailScreen
import com.example.ui.fields.FieldsScreen
import com.example.ui.lists.ReadingListDetailScreen
import com.example.ui.lists.ReadingListEditorScreen
import com.example.ui.lists.ReadingListsScreen
import com.example.ui.notifications.NotificationDetailScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.opportunities.OpportunitiesScreen
import com.example.ui.opportunities.OpportunityDetailScreen
import com.example.ui.paper.PaperDetailScreen
import com.example.ui.profile.ProfileEditScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.profile.UserProfileScreen
import com.example.ui.search.SearchScreen
import com.example.ui.settings.SettingsScreen

/** Every destination, wired to its neighbours. */
@Composable
fun CiteCircleNavHost(
    navController: NavHostController,
    viewModel: HomeViewModel,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onContinue = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    navController.navigate(Routes.FEED) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.FEED) {
            HomeScreen(
                viewModel = viewModel,
                onViewPaper = { navController.navigate(Routes.paperDetail(it)) },
                onViewAuthor = { navController.navigate(Routes.userProfile(it)) },
                onComposePaper = { navController.navigate(Routes.COMPOSE_PAPER) },
            )
        }

        composable(Routes.FIELDS) {
            FieldsScreen(onOpenField = { navController.navigate(Routes.fieldDetail(it)) })
        }

        composable(Routes.LISTS) {
            ReadingListsScreen(
                onOpenList = { navController.navigate(Routes.listDetail(it)) },
                onCreateList = { navController.navigate(Routes.listEditor()) },
            )
        }

        composable(Routes.OPPS) {
            OpportunitiesScreen(
                onOpenOpportunity = { navController.navigate(Routes.opportunityDetail(it)) },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                viewModel = viewModel,
                onViewPaper = { navController.navigate(Routes.paperDetail(it)) },
                onEditProfile = { navController.navigate(Routes.PROFILE_EDIT) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onComposePaper = { navController.navigate(Routes.COMPOSE_PAPER) },
            )
        }

        composable(Routes.CHAT) {
            ChatScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenPaper = { navController.navigate(Routes.paperDetail(it)) },
                onOpenField = { navController.navigate(Routes.fieldDetail(it)) },
                onOpenList = { navController.navigate(Routes.listDetail(it)) },
                onOpenOpportunity = { navController.navigate(Routes.opportunityDetail(it)) },
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onOpenNotification = { navController.navigate(Routes.notificationDetail(it)) },
            )
        }

        composable(
            route = Routes.NOTIFICATION_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_NOTIFICATION_ID) { type = NavType.StringType }),
        ) { entry ->
            NotificationDetailScreen(
                notificationId = entry.stringArg(Routes.ARG_NOTIFICATION_ID),
                onBack = { navController.popBackStack() },
                onViewPaper = {
                    // Notifications reference papers that are not in the local registry yet, so
                    // this lands on the feed rather than a paper that cannot be resolved.
                    navController.navigate(Routes.FEED) { launchSingleTop = true }
                },
            )
        }

        composable(
            route = Routes.PAPER_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_PAPER_ID) { type = NavType.StringType }),
        ) { entry ->
            PaperDetailScreen(
                paperId = entry.stringArg(Routes.ARG_PAPER_ID),
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onViewAuthor = { navController.navigate(Routes.userProfile(it)) },
            )
        }

        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(navArgument(Routes.ARG_USER_ID) { type = NavType.StringType }),
        ) { entry ->
            UserProfileScreen(
                paperId = entry.stringArg(Routes.ARG_USER_ID),
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onViewPaper = { navController.navigate(Routes.paperDetail(it)) },
            )
        }

        composable(
            route = Routes.LIST_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_LIST_ID) { type = NavType.StringType }),
        ) { entry ->
            ReadingListDetailScreen(
                listId = entry.stringArg(Routes.ARG_LIST_ID),
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.listEditor(it)) },
            )
        }

        composable(
            route = Routes.LIST_EDITOR,
            arguments = listOf(
                navArgument(Routes.ARG_LIST_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            ReadingListEditorScreen(
                listId = entry.stringArg(Routes.ARG_LIST_ID),
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.OPPORTUNITY_DETAIL,
            arguments = listOf(
                navArgument(Routes.ARG_OPPORTUNITY_ID) { type = NavType.StringType },
            ),
        ) { entry ->
            OpportunityDetailScreen(
                opportunityId = entry.stringArg(Routes.ARG_OPPORTUNITY_ID),
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.FIELD_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_FIELD_ID) { type = NavType.StringType }),
        ) { entry ->
            FieldDetailScreen(
                fieldId = entry.stringArg(Routes.ARG_FIELD_ID),
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.COMPOSE_PAPER) {
            ComposePaperScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.PROFILE_EDIT) {
            ProfileEditScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
    }
}

private fun androidx.navigation.NavBackStackEntry.stringArg(key: String): String? =
    arguments?.getString(key)
