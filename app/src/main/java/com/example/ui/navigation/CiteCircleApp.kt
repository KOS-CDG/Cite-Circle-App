package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.HomeViewModel
import com.example.MyApplication
import com.example.ui.assistant.AssistantScreen
import com.example.ui.discover.DiscoverScreen
import com.example.ui.feed.ComposerScreen
import com.example.ui.feed.FeedScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.messenger.ConversationListScreen
import com.example.ui.messenger.ConversationListViewModelFactory
import com.example.ui.messenger.NewMessageScreen
import com.example.ui.messenger.ThreadScreen
import com.example.ui.messenger.ThreadViewModelFactory
import com.example.ui.notifications.NotificationDetailScreen
import com.example.ui.notifications.NotificationsScreen
import com.example.ui.notifications.NotificationsViewModel
import com.example.ui.notifications.NotificationsViewModelFactory
import com.example.ui.people.PeopleViewModel
import com.example.ui.people.PeopleViewModelFactory
import com.example.ui.profile.ProfileScreen
import kotlinx.coroutines.launch

@Composable
fun CiteCircleApp(viewModel: HomeViewModel) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val application = LocalContext.current.applicationContext as MyApplication
    val messenger = application.messengerRepository

    val unreadMessages by messenger.observeTotalUnread()
        .collectAsStateWithLifecycle(initialValue = 0)

    // Hoisted to the shell so the bell badge stays live regardless of which tab is showing.
    val notificationsViewModel: NotificationsViewModel = viewModel(
        factory = NotificationsViewModelFactory(application.notificationRepository),
    )
    val unreadNotifications by notificationsViewModel.unreadCount.collectAsStateWithLifecycle()

    val peopleViewModel: PeopleViewModel = viewModel(
        factory = PeopleViewModelFactory(application.peopleRepository, messenger),
    )

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
                    unreadNotifications = unreadNotifications,
                )
            }
        },
        bottomBar = {
            if (showChrome) {
                BottomNavBar(
                    currentDestination = currentDestination,
                    unreadMessages = unreadMessages,
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

            composable(Routes.FEED) {
                FeedScreen(
                    viewModel = viewModel,
                    onOpenComposer = { navController.navigate(Routes.COMPOSER) },
                )
            }
            composable(Routes.COMPOSER) {
                ComposerScreen(viewModel = viewModel, onClose = { navController.popBackStack() })
            }
            composable(Routes.DISCOVER) {
                DiscoverScreen(
                    peopleViewModel = peopleViewModel,
                    onOpenThread = { id -> navController.navigate(Routes.thread(id)) },
                )
            }
            composable(Routes.LIBRARY) { LibraryScreen() }
            composable(Routes.PROFILE) { ProfileScreen(viewModel) }

            composable(Routes.MESSAGES) {
                ConversationListScreen(
                    viewModel = viewModel(
                        factory = ConversationListViewModelFactory(messenger),
                    ),
                    onOpenThread = { id -> navController.navigate(Routes.thread(id)) },
                    onNewMessage = { navController.navigate(Routes.NEW_MESSAGE) },
                )
            }
            composable(Routes.NEW_MESSAGE) {
                NewMessageScreen(
                    people = messenger.allUsers(),
                    onBack = { navController.popBackStack() },
                    onStart = { userIds ->
                        scope.launch {
                            val id = messenger.startConversation(userIds)
                            navController.navigate(Routes.thread(id)) {
                                // Drop the picker from the back stack so Back from the thread
                                // returns to the conversation list, not to the picker.
                                popUpTo(Routes.NEW_MESSAGE) { inclusive = true }
                            }
                        }
                    },
                )
            }
            // The app's first parameterized route.
            composable(
                route = Routes.THREAD,
                arguments = listOf(
                    navArgument(Routes.THREAD_ARG) { type = NavType.StringType },
                ),
            ) { entry ->
                val conversationId = entry.arguments?.getString(Routes.THREAD_ARG).orEmpty()
                ThreadScreen(
                    viewModel = viewModel(
                        // Keyed so navigating between threads does not reuse the previous
                        // thread's ViewModel.
                        key = "thread-$conversationId",
                        factory = ThreadViewModelFactory(messenger, conversationId),
                    ),
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ASSISTANT) {
                AssistantScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    viewModel = notificationsViewModel,
                    onOpenDetail = { id ->
                        navController.navigate(Routes.notificationDetail(id))
                    },
                )
            }
            composable(
                route = Routes.NOTIFICATION_DETAIL,
                arguments = listOf(
                    navArgument(Routes.NOTIFICATION_ARG) { type = NavType.StringType },
                ),
            ) { entry ->
                NotificationDetailScreen(
                    viewModel = notificationsViewModel,
                    notificationId = entry.arguments
                        ?.getString(Routes.NOTIFICATION_ARG).orEmpty(),
                    onBack = { navController.popBackStack() },
                    onMessage = { userId ->
                        peopleViewModel.openConversation(userId) { id ->
                            navController.navigate(Routes.thread(id))
                        }
                    },
                )
            }
        }
    }
}
