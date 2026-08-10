package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.auth.FirebaseAuthManager
import com.example.ui.components.CiteCircleDefaults
import com.example.ui.navigation.CiteCircleNavHost
import com.example.ui.navigation.Routes
import com.example.ui.navigation.isTopLevelRoute
import com.example.ui.navigation.topLevelDestinations
import com.example.ui.theme.InkAndFieldNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val application = context.applicationContext as MyApplication
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(application.repository),
            )
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

            InkAndFieldNotesTheme(darkTheme = isDarkMode) {
                FolioApp(viewModel)
            }
        }
    }
}

@Composable
fun FolioApp(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authManager = remember { FirebaseAuthManager(context) }
    val startDestination = remember {
        if (authManager.getCurrentUser() != null) Routes.FEED else Routes.ONBOARDING
    }

    // Read the route from the back stack rather than mirroring it into local state. Detail
    // routes report their pattern (`paper_detail/{paperId}`), which is exactly what the
    // top-level comparison below needs.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showAppChrome = isTopLevelRoute(currentRoute)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (showAppChrome) {
                RegistryHeader(
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenChat = { navController.navigate(Routes.CHAT) },
                )
            }
        },
        bottomBar = {
            if (showAppChrome) {
                RegistryBottomBar(navController = navController, currentRoute = currentRoute)
            }
        },
    ) { innerPadding ->
        CiteCircleNavHost(
            navController = navController,
            viewModel = viewModel,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun RegistryHeader(
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenChat: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = CiteCircleDefaults.ScreenPadding, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "JOURNAL REGISTRY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Cite Circle",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderAction(
                    icon = Icons.Outlined.Search,
                    contentDescription = "Search the registry",
                    onClick = onOpenSearch,
                )
                HeaderAction(
                    icon = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    onClick = onOpenNotifications,
                )
                @Suppress("DEPRECATION")
                HeaderAction(
                    icon = Icons.Outlined.Chat,
                    contentDescription = "Research assistant",
                    onClick = onOpenChat,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = CiteCircleDefaults.hairlineColor(), thickness = 1.dp)
    }
}

@Composable
private fun HeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                CiteCircleDefaults.cardBorder(alpha = 0.2f),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun RegistryBottomBar(navController: NavHostController, currentRoute: String?) {
    Column {
        HorizontalDivider(color = CiteCircleDefaults.hairlineColor(), thickness = 1.dp)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            topLevelDestinations.forEach { destination ->
                val selected = currentRoute == destination.route
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (selected) {
                                destination.selectedIcon
                            } else {
                                destination.unselectedIcon
                            },
                            contentDescription = destination.label,
                        )
                    },
                    label = {
                        Text(
                            destination.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                fontSize = 9.sp,
                            ),
                            maxLines = 1,
                        )
                    },
                    selected = selected,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(Routes.FEED) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.4f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.4f),
                    ),
                )
            }
        }
    }
}
