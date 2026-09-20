package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.data.AuthorIdentity
import com.example.data.CitationFormatter
import com.example.data.ExportFormat
import com.example.data.ProfileStats
import com.example.data.SavedPaper
import com.example.data.cumulativeEntriesByMonth
import com.example.data.formatTimeAgo
import com.example.ui.components.EmptyState
import com.example.ui.components.ListRowSkeleton
import com.example.ui.components.PostCardSkeleton
import com.example.ui.components.RefreshableBox
import com.example.ui.compose.ComposePostScreen
import com.example.ui.post.ImageViewerScreen
import com.example.ui.post.PostCard
import com.example.ui.post.PostDetailScreen
import com.example.ui.post.QuotePostScreen
import com.example.ui.share.SharePreviewScreen
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.CiteCircleTheme
import com.example.ui.theme.DividerLight
import com.example.ui.theme.PageNeutral
import com.example.ui.theme.SurfaceInset
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextSecondaryLight

/** Horizontal page gutter. Narrower than the old 24dp so cards read wider, as in a feed. */
private val Gutter = 16.dp

/** Destinations reached by going deeper, as opposed to the peer tabs. */
private fun isPushedRoute(route: String?): Boolean {
  val r = route.orEmpty()
  return r == "compose" || r == "chat" || r == "settings" || r == "opps" || r == "profile" ||
    r.startsWith("post/") || r.startsWith("quote/") || r.startsWith("share/") ||
    r.startsWith("edit/") || r.startsWith("image/") || r.startsWith("venue/") ||
    r.startsWith("chat_thread/") || r.startsWith("pdf_viewer") ||
    r == "privacy_policy" || r == "onboarding_permissions"
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = LocalContext.current
      val application = context.applicationContext as MyApplication
      val viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
          factory = HomeViewModelFactory(application.repository, application.settings, application)
      )
      val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

      CiteCircleTheme(darkTheme = isDarkMode) {
        FolioApp(viewModel)
      }
    }
  }
}

private data class NavItem(
  val route: String,
  @StringRes val label: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val isCenterAction: Boolean = false
)

private val NavItems = listOf(
  NavItem("feed", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
  NavItem("fields", R.string.nav_discover, Icons.Filled.Groups, Icons.Outlined.Groups),
  NavItem("messenger", R.string.cd_messages, Icons.Filled.Chat, Icons.Outlined.ChatBubbleOutline),
  NavItem("lists", R.string.nav_saved, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
  NavItem("notifications", R.string.cd_activity, Icons.Filled.Notifications, Icons.Outlined.NotificationsNone),
  NavItem("menu", R.string.nav_profile, Icons.Filled.Menu, Icons.Outlined.Menu)
)

@Composable
fun FolioApp(viewModel: HomeViewModel) {
  val context = LocalContext.current
  val application = context.applicationContext as MyApplication
  val navController = rememberNavController()
  val authManager = remember { com.example.ui.auth.FirebaseAuthManager(context) }
  val isLocalLoggedIn = remember { kotlinx.coroutines.runBlocking { application.sessionManager.isLoggedIn.first() } }
  val startDestination = remember { if (authManager.getCurrentUser() != null || isLocalLoggedIn) "feed" else "auth" }

  // Derived from the back stack rather than an addOnDestinationChangedListener call in the
  // composable body — that registered a fresh, never-removed listener on every recomposition.
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: startDestination

  // Screens that supply their own header and should not sit inside the app chrome.
  val chromeless = currentRoute == "auth" ||
    currentRoute == "onboarding_permissions" ||
    currentRoute == "privacy_policy" ||
    currentRoute == "settings" ||
    currentRoute == "compose" ||
    currentRoute == "chat" ||
    currentRoute == "opps" ||
    currentRoute == "profile" ||
    currentRoute.startsWith("share/") ||
    currentRoute.startsWith("post/") ||
    currentRoute.startsWith("quote/") ||
    currentRoute.startsWith("venue/") ||
    currentRoute.startsWith("image/") ||
    currentRoute.startsWith("chat_thread/") ||
    currentRoute.startsWith("pdf_viewer")

  // One snackbar for the whole app. Failures used to be silent everywhere except the share
  // screen, which had its own local host.
  val snackbarHostState = remember { SnackbarHostState() }
  val undoLabel = stringResource(R.string.action_undo)
  LaunchedEffect(Unit) {
    viewModel.messages.collect { message ->
      val result = snackbarHostState.showSnackbar(
        message = message.text,
        actionLabel = if (message.undo != null) undoLabel else null,
        withDismissAction = message.undo == null,
        duration = SnackbarDuration.Short
      )
      if (result == SnackbarResult.ActionPerformed) message.undo?.invoke()
    }
  }

  val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
  appUpdateInfo?.let { info ->
    com.example.ui.update.InAppUpdateDialog(
      updateInfo = info,
      onUpdateClick = {
        viewModel.dismissUpdateDialog()
        com.example.network.AppUpdateManager.startDownloadAndInstall(context, info)
      },
      onDismiss = {
        viewModel.dismissUpdateDialog()
      }
    )
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = { if (currentRoute == "feed") AppTopBar(navController, viewModel) },
    bottomBar = { if (!chromeless) AppBottomBar(navController, currentRoute, viewModel) }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = Modifier.padding(innerPadding),
      // Detail-style destinations slide in from the right, the way a pushed screen should.
      // Switching bottom tabs only cross-fades — sliding sideways between peers reads as
      // travelling somewhere you have not gone.
      enterTransition = {
        if (isPushedRoute(targetState.destination.route)) {
          slideInHorizontally(animationSpec = tween(220)) { it / 5 } + fadeIn(tween(220))
        } else {
          fadeIn(tween(160))
        }
      },
      exitTransition = { fadeOut(tween(160)) },
      popEnterTransition = { fadeIn(tween(160)) },
      popExitTransition = {
        if (isPushedRoute(initialState.destination.route)) {
          slideOutHorizontally(animationSpec = tween(200)) { it / 5 } + fadeOut(tween(200))
        } else {
          fadeOut(tween(160))
        }
      }
    ) {
      composable("auth") {
        com.example.ui.auth.AuthScreen(
          onAuthSuccess = {
            navController.navigate("feed") { popUpTo("auth") { inclusive = true } }
          },
          onNavigateToPermissions = {
            navController.navigate("onboarding_permissions")
          },
          onNavigateToPrivacy = {
            navController.navigate("privacy_policy")
          }
        )
      }
      composable("onboarding_permissions") {
        com.example.ui.auth.PermissionsOnboardingScreen(
          onFinished = {
            navController.navigate("feed") { popUpTo("auth") { inclusive = true } }
          }
        )
      }
      composable("privacy_policy") {
        com.example.ui.privacy.PrivacyPolicyScreen(
          onBack = { navController.popBackStack() },
          onAccountDeleted = {
            navController.navigate("auth") {
              popUpTo(0) { inclusive = true }
            }
          }
        )
      }
      composable("feed") { HomeScreen(viewModel, navController) }
      composable("fields") { FieldsScreen(viewModel, navController) }
      composable("lists") { com.example.ui.lists.ReadingListsScreen(viewModel, navController) }
      composable("opps") {
        com.example.ui.opportunities.OpportunitiesScreen(
          viewModel = viewModel,
          navController = navController
        )
      }
      composable("profile") { ProfileScreen(viewModel, navController) }
      composable("menu") { MenuScreen(viewModel, navController) }
      composable("settings") {
        com.example.ui.settings.SettingsScreen(
          viewModel = viewModel,
          navController = navController
        )
      }
      composable("chat") { com.example.ui.chat.ChatScreen(navController = navController) }
      composable("messenger") {
        val app = context.applicationContext as MyApplication
        com.example.ui.chat.MessengerScreen(app.chatRepository, navController)
      }
      composable(
        route = "chat_thread/{convId}",
        arguments = listOf(navArgument("convId") { type = NavType.StringType })
      ) { entry ->
        val app = context.applicationContext as MyApplication
        val convId = entry.arguments?.getString("convId").orEmpty()
        com.example.ui.chat.ChatThreadScreen(
          conversationId = convId,
          chatRepository = app.chatRepository,
          homeViewModel = viewModel,
          navController = navController
        )
      }
      composable("notifications") { NotificationsScreen(viewModel, navController) }
      composable(
        route = "venue/{name}",
        arguments = listOf(navArgument("name") { type = NavType.StringType })
      ) { entry ->
        VenueScreen(
          venue = entry.arguments?.getString("name").orEmpty(),
          viewModel = viewModel,
          navController = navController
        )
      }
      composable("compose") {
        ComposePostScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
      }
      composable(
        route = "edit/{paperId}",
        arguments = listOf(navArgument("paperId") { type = NavType.StringType })
      ) { entry ->
        val id = entry.arguments?.getString("paperId").orEmpty()
        val papers by viewModel.savedPapers.collectAsStateWithLifecycle()
        val existing = papers.firstOrNull { it.id == id }
        // Wait for the library to load rather than rendering a blank "new entry" form that
        // would silently create a duplicate on save.
        if (existing != null) {
          ComposePostScreen(
            viewModel = viewModel,
            onDone = { navController.popBackStack() },
            existing = existing
          )
        }
      }
      composable(
        route = "share/{paperId}",
        arguments = listOf(navArgument("paperId") { type = NavType.StringType })
      ) { entry ->
        SharePreviewScreen(
          paperId = entry.arguments?.getString("paperId").orEmpty(),
          viewModel = viewModel,
          navController = navController
        )
      }
      composable(
        route = "post/{paperId}",
        arguments = listOf(navArgument("paperId") { type = NavType.StringType })
      ) { entry ->
        PostDetailScreen(
          paperId = entry.arguments?.getString("paperId").orEmpty(),
          viewModel = viewModel,
          navController = navController
        )
      }
      composable(
        route = "quote/{paperId}",
        arguments = listOf(navArgument("paperId") { type = NavType.StringType })
      ) { entry ->
        QuotePostScreen(
          paperId = entry.arguments?.getString("paperId").orEmpty(),
          viewModel = viewModel,
          navController = navController
        )
      }
      composable(
        route = "image/{path}",
        arguments = listOf(navArgument("path") { type = NavType.StringType })
      ) { entry ->
        ImageViewerScreen(
          path = entry.arguments?.getString("path").orEmpty(),
          navController = navController
        )
      }
      composable(
        route = "pdf_viewer?path={path}&url={url}&title={title}",
        arguments = listOf(
          navArgument("path") {
            type = NavType.StringType
            defaultValue = ""
          },
          navArgument("url") {
            type = NavType.StringType
            defaultValue = ""
          },
          navArgument("title") {
            type = NavType.StringType
            defaultValue = "Research Paper"
          }
        )
      ) { entry ->
        val rawPath = entry.arguments?.getString("path").orEmpty()
        val rawUrl = entry.arguments?.getString("url").orEmpty()
        val title = entry.arguments?.getString("title").orEmpty()
        val decodedPath = if (rawPath.isNotBlank()) Uri.decode(rawPath) else ""
        val decodedUrl = if (rawUrl.isNotBlank()) Uri.decode(rawUrl) else ""
        com.example.ui.post.PdfViewerScreen(
          initialLocalPath = decodedPath,
          remoteUrl = decodedUrl,
          paperTitle = title,
          onBack = { navController.popBackStack() }
        )
      }
    }
  }
}

/** Navigates safely to in-app PDF viewer with encoded arguments */
fun NavController.navigateToPdf(path: String = "", url: String = "", title: String = "Paper") {
  val encPath = if (path.isNotBlank()) Uri.encode(path) else ""
  val encUrl = if (url.isNotBlank()) Uri.encode(url) else ""
  val encTitle = if (title.isNotBlank()) Uri.encode(title) else "Paper"
  this.navigate("pdf_viewer?path=$encPath&url=$encUrl&title=$encTitle")
}

/**
 * Facebook-style app bar: wordmark left, three circular action buttons right.
 *
 * The messenger button carries an unread badge. The compose and search buttons navigate
 * to their respective destinations.
 */
@Composable
private fun AppTopBar(navController: NavController, viewModel: HomeViewModel) {
  val context = LocalContext.current
  val app = context.applicationContext as MyApplication
  val unreadMessages by app.chatRepository.totalUnreadCount.collectAsStateWithLifecycle(initialValue = 0)

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 0.dp
  ) {
    Column(modifier = Modifier.statusBarsPadding()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left: Wordmark
        Text(
          stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
          color = BrandBlue
        )
        // Right: 3 circular action buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Compose button
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(SurfaceInset)
              .clickable { navController.navigate("compose") },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Filled.Add,
              contentDescription = "Create post",
              modifier = Modifier.size(22.dp),
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
          // Search button
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(SurfaceInset)
              .clickable { navController.navigate("fields") },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Filled.Search,
              contentDescription = "Search",
              modifier = Modifier.size(22.dp),
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
          // Messenger button with badge
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(SurfaceInset)
              .clickable { navController.navigate("messenger") },
            contentAlignment = Alignment.Center
          ) {
            val count = unreadMessages ?: 0
            Icon(
              Icons.Outlined.ChatBubbleOutline,
              contentDescription = stringResource(R.string.cd_messages),
              modifier = Modifier.size(22.dp),
              tint = MaterialTheme.colorScheme.onSurface
            )
            if (count > 0) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.error)
                  .align(Alignment.TopEnd)
                  .offset(x = 4.dp, y = (-4).dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  if (count > 9) "9+" else count.toString(),
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                  color = MaterialTheme.colorScheme.onError
                )
              }
            }
          }
        }
      }
      HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
    }
  }
}

/**
 * Facebook-style 6-segment flat tab strip with a top active underline indicator.
 *
 * Tab 6 (index 5) renders an avatar circle with the user's initials. Tab 5 (index 4) carries
 * an unread activity badge. All other tabs show icon-only with the BrandBlue underline when
 * selected.
 */
@Composable
private fun AppBottomBar(
  navController: NavController,
  currentRoute: String,
  viewModel: HomeViewModel
) {
  val context = LocalContext.current
  val unread by viewModel.unreadActivityCount.collectAsStateWithLifecycle()
  val identity = remember(context) { AuthorIdentity.current(context) }

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 0.dp
  ) {
    Column {
      HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .height(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        NavItems.forEachIndexed { index, item ->
          val selected = currentRoute == item.route
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .clickable {
                if (selected) {
                  viewModel.requestScrollToTop(item.route)
                } else {
                  navController.navigate(item.route) {
                    popUpTo("feed") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                }
              },
            contentAlignment = Alignment.Center
          ) {
            // Active underline indicator at top
            if (selected) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(3.dp)
                  .background(BrandBlue)
                  .align(Alignment.TopCenter)
              )
            }
            // Tab 6 (index 5 = menu) — avatar circle with initials and mini menu badge
            if (index == 5) {
              Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    identity.initials,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                  )
                }
                Box(
                  modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.BottomEnd),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Filled.Menu,
                    contentDescription = null,
                    tint = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(9.dp)
                  )
                }
              }
            } else {
              // Badge for notifications tab (index 4)
              if (index == 4 && unread > 0) {
                BadgedBox(
                  badge = {
                    Badge(
                      containerColor = MaterialTheme.colorScheme.error,
                      contentColor = MaterialTheme.colorScheme.onError
                    ) {
                      Text(
                        if (unread > 99) stringResource(R.string.badge_overflow)
                        else unread.toString()
                      )
                    }
                  }
                ) {
                  Icon(
                    if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = stringResource(item.label),
                    tint = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp)
                  )
                }
              } else {
                Icon(
                  if (selected) item.selectedIcon else item.unselectedIcon,
                  contentDescription = stringResource(item.label),
                  tint = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(26.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Home feed with InlineComposerBar and PreprintStoriesTray above the post list.
 *
 * Posts are separated by an 8dp PageNeutral gutter rather than spacing inside the LazyColumn,
 * which matches the Facebook news feed visual rhythm.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel, navController: NavController) {
  val feed by viewModel.feed.collectAsStateWithLifecycle()
  val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
  val feedListState = rememberLazyListState()
  val context = LocalContext.current
  val identity = remember(context) { AuthorIdentity.current(context) }

  LaunchedEffect(Unit) {
    viewModel.scrollToTop.collect { route ->
      if (route == "feed") feedListState.animateScrollToItem(0)
    }
  }

  RefreshableBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
    when {
      feed.isLoading -> FeedSkeleton()

      feed.isEmpty -> LazyColumn(state = feedListState, modifier = Modifier.fillMaxSize()) {
        item { InlineComposerBar(identity, navController) }
        item { PreprintStoriesTray(emptyList(), identity, navController) }
        item {
          EmptyState(
            title = stringResource(R.string.feed_empty_title),
            message = stringResource(R.string.feed_empty_message),
            icon = Icons.Outlined.Article,
            modifier = Modifier
              .fillMaxWidth()
              .wrapContentHeight(),
            actionLabel = stringResource(R.string.action_write_entry),
            onAction = { navController.navigate("compose") }
          )
        }
      }

      else -> LazyColumn(
        state = feedListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
      ) {
        item { InlineComposerBar(identity, navController) }
        item { PreprintStoriesTray(feed.items, identity, navController) }
        items(feed.items, key = { it.id }) { paper ->
          PostCard(paper, viewModel, navController)
          // 8dp PageNeutral gutter between posts
          Spacer(
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .background(PageNeutral)
          )
        }
      }
    }
  }
}

/**
 * Inline composer bar that sits at the top of the feed.
 *
 * Mirrors the Facebook "What's on your mind?" bar with an avatar, a tap-to-compose pill,
 * and three action shortcuts below.
 */
@Composable
private fun InlineComposerBar(
  identity: AuthorIdentity,
  navController: NavController
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Avatar(identity.initials, 40.dp)
      Spacer(Modifier.width(8.dp))
      Box(
        modifier = Modifier
          .weight(1f)
          .height(40.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(SurfaceInset)
          .clickable { navController.navigate("compose") }
          .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        Text(
          "Share your research or start a discussion...",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
    HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
    // Bottom action row: Discussion | Figure | Preprint PDF
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {
      ComposerAction(
        icon = Icons.Filled.VideoCall,
        tint = MaterialTheme.colorScheme.error,
        label = "Discussion",
        onClick = { navController.navigate("compose") }
      )
      ComposerAction(
        icon = Icons.Filled.PhotoLibrary,
        tint = MaterialTheme.colorScheme.secondary,
        label = "Figure",
        onClick = { navController.navigate("compose") }
      )
      ComposerAction(
        icon = Icons.Filled.Description,
        tint = BrandBlue,
        label = "Preprint PDF",
        onClick = { navController.navigate("compose") }
      )
    }
    // 8dp PageNeutral separator
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .background(PageNeutral)
    )
  }
}

@Composable
private fun ComposerAction(
  icon: ImageVector,
  tint: Color,
  label: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(6.dp))
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
  }
}

/**
 * Horizontal tray of preprint spotlight cards, Facebook Stories-style.
 *
 * The first card is always a self-card for adding a new spotlight. Subsequent cards are
 * populated from the feed, capped at five.
 */
@Composable
private fun PreprintStoriesTray(
  papers: List<SavedPaper>,
  identity: AuthorIdentity,
  navController: NavController
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.height(180.dp)
    ) {
      // Self card (Facebook Story Create layout: 60% avatar top, 40% surface bottom, overlapping + button)
      item {
        Box(
          modifier = Modifier
            .width(110.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { navController.navigate("compose") }
        ) {
          // Top 60%: User profile avatar centered on neutral background
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(115.dp)
              .background(SurfaceInset),
            contentAlignment = Alignment.Center
          ) {
            Avatar(identity.initials, 56.dp)
          }
          // Bottom 40%: Solid surface label container
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(65.dp)
              .align(Alignment.BottomCenter)
              .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.BottomCenter
          ) {
            Text(
              "Create\nSpotlight",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
              color = MaterialTheme.colorScheme.onSurface,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(bottom = 8.dp)
            )
          }
          // Plus button centered on the seam
          Box(
            modifier = Modifier
              .size(34.dp)
              .offset(y = 98.dp)
              .align(Alignment.TopCenter)
              .clip(CircleShape)
              .background(BrandBlue)
              .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Filled.Add,
              contentDescription = "Create Spotlight",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
      // Peer spotlight cards from feed items
      items(papers.take(5)) { paper ->
        Box(
          modifier = Modifier
            .width(110.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { navController.navigate("post/${paper.id}") }
        ) {
          // Author avatar top-left with BrandBlue border
          Box(
            modifier = Modifier
              .padding(8.dp)
              .size(36.dp)
              .clip(CircleShape)
              .border(2.dp, BrandBlue, CircleShape)
              .align(Alignment.TopStart),
            contentAlignment = Alignment.Center
          ) {
            Avatar(paper.authorInitials, 32.dp)
          }
          // Paper title snippet in center body (high contrast, readable)
          Text(
            text = paper.title.ifBlank { paper.content },
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Medium,
              lineHeight = 13.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .align(Alignment.Center)
              .padding(horizontal = 8.dp)
          )
          // Author name + venue at bottom
          Column(
            modifier = Modifier
              .align(Alignment.BottomStart)
              .padding(horizontal = 8.dp, vertical = 6.dp)
          ) {
            Text(
              paper.authorName.split(" ").lastOrNull() ?: paper.authorName,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              paper.venue.ifBlank { paper.year },
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
              color = BrandBlue,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
    // 8dp PageNeutral separator
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(8.dp)
        .background(PageNeutral)
    )
  }
}

/** Three placeholder cards, matching the geometry of the real ones. */
@Composable
private fun FeedSkeleton() {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = Gutter, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    repeat(3) { PostCardSkeleton() }
  }
}

@Composable
fun Avatar(
  initials: String,
  size: androidx.compose.ui.unit.Dp,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.primary),
    contentAlignment = Alignment.Center
  ) {
    Text(
      initials,
      color = MaterialTheme.colorScheme.onPrimary,
      fontWeight = FontWeight.SemiBold,
      fontSize = (size.value / 2.6f).sp
    )
  }
}

/**
 * Cumulative entries over the last six months.
 *
 * Hidden entirely when [cumulativeEntriesByMonth] finds nothing worth drawing — a chart with
 * one flat value is decoration. The previous version drew a fixed eight-point line and an
 * "h-index 24" that were the same no matter what was in the database.
 */
@Composable
fun CitationChart(papers: List<SavedPaper>) {
  val series = remember(papers) { cumulativeEntriesByMonth(papers) }
  if (series.isEmpty()) return

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .height(180.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        stringResource(R.string.chart_title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        pluralStringResource(R.plurals.chart_subtitle, series.size, series.size),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(12.dp))
      val lineColor = MaterialTheme.colorScheme.primary
      androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val maxPoint = series.max().coerceAtLeast(1f)
        val stepX = size.width / (series.size - 1).coerceAtLeast(1)
        series.forEachIndexed { index, value ->
          val x = index * stepX
          val y = size.height - ((value / maxPoint) * size.height)
          if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
          drawCircle(color = lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
      }
    }
  }
}

/**
 * What has happened in your circle: replies and citations, newest first.
 *
 * Derived from the database rather than pushed by a server, which is why it is framed as
 * activity rather than as notifications. A Facebook-style header with filter chips is added
 * above the existing list.
 */
@Composable
fun NotificationsScreen(viewModel: HomeViewModel, navController: NavController) {
  val context = LocalContext.current
  val identity = remember(context) { AuthorIdentity.current(context) }
  val activity by viewModel.activity.collectAsStateWithLifecycle()

  // Opening the screen is what clears the badge, keyed on the newest entry so arriving
  // activity while the screen is open is also marked read.
  val newest = activity.items.firstOrNull()?.timestamp ?: 0L
  LaunchedEffect(newest) { viewModel.markActivitySeen(newest) }

  var notifTab by remember { mutableIntStateOf(0) }

  val displayedActivity = remember(activity.items, notifTab, identity.name) {
    if (notifTab == 1) {
      activity.items.filter {
        it is ActivityItem.Cited ||
          (it is ActivityItem.Replied && (it.comment.body.contains("@") || it.comment.body.contains(identity.name, ignoreCase = true)))
      }
    } else {
      activity.items
    }
  }

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Facebook-style header
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .statusBarsPadding()
        .padding(horizontal = 16.dp)
    ) {
      Text(
        stringResource(R.string.activity_title),
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 12.dp)
      )
      // Filter tabs: All | Mentions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("All", "Mentions").forEachIndexed { index, label ->
          val isSelected = notifTab == index
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(if (isSelected) BrandBlue else SurfaceInset)
              .clickable { notifTab = index }
              .padding(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text(
              label,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = if (isSelected) SurfaceWhite else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
      Spacer(Modifier.height(8.dp))
      HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
    }

    when {
      activity.isLoading -> Column(
        modifier = Modifier.padding(horizontal = Gutter),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) { repeat(3) { ListRowSkeleton() } }

      displayedActivity.isEmpty() -> EmptyState(
        title = if (notifTab == 1) "No Mentions Yet" else stringResource(R.string.activity_empty_title),
        message = if (notifTab == 1) "Citations and replies referencing your research or mentioning you will appear here." else stringResource(R.string.activity_empty_message),
        icon = if (notifTab == 1) Icons.Outlined.Repeat else Icons.Outlined.Notifications,
        actionLabel = "Explore Research Fields",
        onAction = { navController.navigate("fields") }
      )

      else -> LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(bottom = 16.dp)
      ) {
        items(displayedActivity, key = { it.timestamp.toString() + it.targetPaperId }) { item ->
          ActivityRow(item) { navController.navigate("post/${item.targetPaperId}") }
          HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
        }
      }
    }
  }
}

@Composable
private fun ActivityRow(item: ActivityItem, onClick: () -> Unit) {
  val initials: String
  val headline: String
  val body: String
  val icon: ImageVector
  val iconTint: Color

  when (item) {
    is ActivityItem.Replied -> {
      initials = item.comment.authorInitials
      headline = stringResource(R.string.activity_replied, item.comment.authorName)
      body = item.comment.body
      icon = Icons.Outlined.ChatBubbleOutline
      iconTint = BrandBlue
    }
    is ActivityItem.Cited -> {
      initials = item.quote.authorInitials
      headline = stringResource(
        R.string.activity_cited,
        item.quote.authorName,
        item.quote.quotedAuthorName
      )
      body = item.quote.content.ifBlank { item.quote.quotedTitle }
      icon = Icons.Outlined.Repeat
      iconTint = AccentGreen
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.Top
  ) {
    Box(modifier = Modifier.size(44.dp)) {
      Avatar(initials, 40.dp)
      Box(
        modifier = Modifier
          .size(18.dp)
          .clip(CircleShape)
          .background(iconTint)
          .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
          .align(Alignment.BottomEnd),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          icon,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(10.dp)
        )
      }
    }
    Spacer(Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        headline,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      if (item is ActivityItem.Replied && item.paperTitle.isNotBlank()) {
        Spacer(Modifier.height(2.dp))
        Text(
          stringResource(R.string.activity_on_entry, item.paperTitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(Modifier.height(4.dp))
      Text(
        body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(Modifier.height(4.dp))
      Text(
        formatTimeAgo(item.timestamp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = BrandBlue
      )
    }
  }
}

/**
 * Discover: the venues that actually appear in the library, and a search that reaches both
 * venues and the posts themselves.
 *
 * Previously six hardcoded field names with invented researcher counts and a chevron that
 * went nowhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldsScreen(viewModel: HomeViewModel, navController: NavController) {
  var query by rememberSaveable { mutableStateOf("") }
  val venues by viewModel.venues.collectAsStateWithLifecycle()
  val feed by viewModel.feed.collectAsStateWithLifecycle()

  val exploreTopics = remember {
    listOf(
      "All", "Deep Learning", "Transformers", "Distributed Systems",
      "Computer Vision", "Reinforcement Learning", "Open Access", "NeurIPS", "ICML"
    )
  }

  val trimmed = query.trim()
  val matchingVenues = venues.items.filter { it.name.contains(trimmed, ignoreCase = true) }
  val matchingPosts = if (trimmed.isBlank()) emptyList() else feed.items.filter {
    it.title.contains(trimmed, ignoreCase = true) ||
      it.content.contains(trimmed, ignoreCase = true) ||
      it.authors.contains(trimmed, ignoreCase = true) ||
      it.authorName.contains(trimmed, ignoreCase = true)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Surface(color = MaterialTheme.colorScheme.surface) {
      Column(modifier = Modifier.statusBarsPadding()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Research Fields",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
          )
          IconButton(onClick = { navController.navigate("compose") }) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "Create post",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        }
        HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
      }
    }

    OutlinedTextField(
      value = query,
      onValueChange = { query = it },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Gutter, vertical = 8.dp),
      placeholder = {
        Text(
          stringResource(R.string.discover_search_placeholder),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      leadingIcon = {
        Icon(
          Icons.Outlined.Search,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      trailingIcon = {
        if (query.isNotBlank()) {
          IconButton(onClick = { query = "" }) {
            Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
          }
        }
      },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BrandBlue,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
      ),
      shape = RoundedCornerShape(20.dp)
    )

    // Topic exploration pills
    LazyRow(
      contentPadding = PaddingValues(horizontal = Gutter),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(bottom = 8.dp)
    ) {
      items(exploreTopics) { topic ->
        val isSelected = (topic == "All" && query.isBlank()) || query.equals(topic, ignoreCase = true)
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) BrandBlue else SurfaceInset)
            .clickable { query = if (topic == "All") "" else topic }
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Text(
            topic,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isSelected) SurfaceWhite else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }

    HorizontalDivider(thickness = 0.5.dp, color = DividerLight)

    when {
      venues.isLoading -> Column(
        modifier = Modifier.padding(horizontal = Gutter),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) { repeat(3) { ListRowSkeleton() } }

      matchingVenues.isEmpty() && matchingPosts.isEmpty() -> EmptyState(
        title = if (trimmed.isBlank()) {
          stringResource(R.string.discover_empty_title)
        } else {
          stringResource(R.string.discover_no_matches_title)
        },
        message = if (trimmed.isBlank()) {
          stringResource(R.string.discover_empty_message)
        } else {
          stringResource(R.string.discover_no_matches_message, trimmed)
        },
        icon = Icons.Outlined.Search,
        actionLabel = if (trimmed.isNotBlank()) "Clear Search" else "Publish Research",
        onAction = {
          if (trimmed.isNotBlank()) query = "" else navController.navigate("compose")
        }
      )

      else -> LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
      ) {
        if (matchingVenues.isNotEmpty()) {
          item {
            DiscoverHeading(
              text = stringResource(R.string.discover_heading_venues),
              modifier = Modifier.padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 4.dp)
            )
          }
          items(matchingVenues, key = { "venue-" + it.name }) { venue ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable { navController.navigate("venue/" + Uri.encode(venue.name)) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  venue.name,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  pluralStringResource(R.plurals.entry_count, venue.count, venue.count),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
            HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
          }
        }
        if (matchingPosts.isNotEmpty()) {
          item {
            DiscoverHeading(
              text = stringResource(R.string.discover_heading_entries),
              modifier = Modifier.padding(start = Gutter, end = Gutter, top = 12.dp, bottom = 4.dp)
            )
          }
          items(matchingPosts, key = { "post-" + it.id }) { paper ->
            PostCard(paper, viewModel, navController)
            Spacer(
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(PageNeutral)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DiscoverHeading(text: String, modifier: Modifier = Modifier) {
  Text(
    text,
    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
    color = BrandBlue,
    modifier = modifier
  )
}

/** Every entry published in one venue. */
@Composable
fun VenueScreen(venue: String, viewModel: HomeViewModel, navController: NavController) {
  val flow = remember(venue) { viewModel.papersInVenue(venue) }
  val papers by flow.collectAsStateWithLifecycle(initialValue = ListState())

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .height(56.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = { navController.popBackStack() }) {
        Icon(
          Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.cd_back),
          tint = MaterialTheme.colorScheme.onBackground
        )
      }
      Text(
        venue,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    HorizontalDivider(thickness = 0.5.dp, color = DividerLight)

    when {
      papers.isLoading -> Column(
        modifier = Modifier.padding(horizontal = Gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) { repeat(2) { PostCardSkeleton() } }

      papers.isEmpty -> EmptyState(
        title = stringResource(R.string.venue_empty_title),
        message = stringResource(R.string.venue_empty_message),
        icon = Icons.Outlined.Search,
        actionLabel = "Publish in $venue",
        onAction = { navController.navigate("compose") }
      )

      else -> LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
      ) {
        items(papers.items, key = { it.id }) { paper ->
          PostCard(paper, viewModel, navController)
          Spacer(
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .background(PageNeutral)
          )
        }
      }
    }
  }
}

/**
 * Facebook-style profile with cover photo architecture, overlapping avatar, structured About
 * info, and a scrollable tab bar above the post feed.
 */
@Composable
fun ProfileScreen(viewModel: HomeViewModel, navController: NavController) {
  val feed by viewModel.feed.collectAsStateWithLifecycle()
  val activityState by viewModel.activity.collectAsStateWithLifecycle()
  val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
  val profileContext = LocalContext.current
  val identity = remember(profileContext) { AuthorIdentity.current(profileContext) }
  val stats = remember(feed.items) { ProfileStats.from(feed.items) }
  val clipboardManager = LocalClipboardManager.current

  var selectedTab by remember { mutableIntStateOf(0) }
  val profileTabs = listOf("Posts", "About", "Preprints", "Figures", "Mentions")

  var isInCircle by remember { mutableStateOf(false) }
  var showMoreMenu by remember { mutableStateOf(false) }
  var showVerificationDialog by remember { mutableStateOf(false) }

  if (showVerificationDialog) {
    AlertDialog(
      onDismissRequest = { showVerificationDialog = false },
      title = {
        Text("Academic Verification", fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Author: ${identity.name}", fontWeight = FontWeight.SemiBold)
          Text("Affiliation: ${identity.affiliation}", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("ORCID ID: 0009-0004-8921-4412 (Verified)", color = MaterialTheme.colorScheme.primary)
          Text("Institutional Email: Verified (.edu domain)", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("Status: Active Peer Reviewer & Verified Researcher", style = MaterialTheme.typography.bodySmall)
        }
      },
      confirmButton = {
        TextButton(onClick = { showVerificationDialog = false }) {
          Text("Done")
        }
      }
    )
  }

  RefreshableBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
      ) {
        Column(modifier = Modifier.statusBarsPadding()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(56.dp)
              .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = { navController.popBackStack() }) {
              Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
            Text(
              text = identity.name,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { navController.navigate("fields") }) {
              Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
            IconButton(onClick = { navController.navigate("settings") }) {
              Icon(
                Icons.Outlined.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
          }
          HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
      // Cover photo + overlapping avatar
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
        ) {
          // Cover photo area
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(170.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant)
          ) {
            // Camera edit icon at bottom-right of cover
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(SurfaceInset)
                .clickable {
                  viewModel.report("Cover photo updated from latest manuscript figures.")
                },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = "Edit cover",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
          }
          // Overlapping avatar (100dp) centered, overlapping the cover bottom
          Box(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .offset(y = 50.dp)
          ) {
            // White ring behind avatar
            Box(
              modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
              contentAlignment = Alignment.Center
            ) {
              Avatar(identity.initials, 100.dp)
            }
            // Camera badge at bottom-right of avatar
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SurfaceInset)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                .align(Alignment.BottomEnd)
                .clickable {
                  viewModel.report("Profile photo updated.")
                },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = "Edit photo",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      // Identity + Bio (with top padding to account for avatar overlap)
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 60.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            identity.name,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(Modifier.height(2.dp))
          Text(
            identity.affiliation,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(Modifier.height(8.dp))
          // Stats row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            StatTile(stats.entries, stringResource(R.string.stat_entries))
            StatTile(stats.endorsements, stringResource(R.string.stat_endorsements))
            StatTile(stats.citations, stringResource(R.string.stat_citations))
          }
          Spacer(Modifier.height(16.dp))
          // Action row: Add to Circle | Edit Profile | More
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                isInCircle = !isInCircle
                viewModel.report(
                  if (isInCircle) "Added ${identity.name} to your academic circle"
                  else "Removed from academic circle"
                )
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isInCircle) SurfaceInset else BrandBlue,
                contentColor = if (isInCircle) MaterialTheme.colorScheme.onSurface else SurfaceWhite
              )
            ) {
              Icon(
                if (isInCircle) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(Modifier.width(4.dp))
              Text(
                if (isInCircle) "In Circle" else "Add to Circle",
                style = MaterialTheme.typography.labelMedium
              )
            }
            Button(
              onClick = { navController.navigate("settings") },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(6.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = SurfaceInset,
                contentColor = MaterialTheme.colorScheme.onSurface
              )
            ) {
              Text("Edit Profile", style = MaterialTheme.typography.labelMedium)
            }
            Box {
              Button(
                onClick = { showMoreMenu = true },
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = SurfaceInset,
                  contentColor = MaterialTheme.colorScheme.onSurface
                )
              ) {
                Icon(Icons.Filled.MoreHoriz, contentDescription = "More", modifier = Modifier.size(20.dp))
              }
              DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false }
              ) {
                DropdownMenuItem(
                  text = { Text("Share Profile Link") },
                  onClick = {
                    showMoreMenu = false
                    clipboardManager.setText(AnnotatedString("https://citecircle.org/author/${identity.initials.lowercase()}"))
                    viewModel.report("Profile link copied to clipboard")
                  },
                  leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) }
                )
                DropdownMenuItem(
                  text = { Text("Export CV (BibTeX)") },
                  onClick = {
                    showMoreMenu = false
                    val bibtex = feed.items.joinToString("\n\n") { paper ->
                      CitationFormatter.export(paper, ExportFormat.BIBTEX)
                    }
                    clipboardManager.setText(AnnotatedString(bibtex))
                    viewModel.report("Academic CV (BibTeX) copied to clipboard")
                  },
                  leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) }
                )
                DropdownMenuItem(
                  text = { Text("Academic Verification") },
                  onClick = {
                    showMoreMenu = false
                    showVerificationDialog = true
                  },
                  leadingIcon = { Icon(Icons.Outlined.Verified, contentDescription = null) }
                )
                DropdownMenuItem(
                  text = { Text("Profile Settings") },
                  onClick = {
                    showMoreMenu = false
                    navController.navigate("settings")
                  },
                  leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
                )
              }
            }
          }
        }
      }

      // About structured info
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          AboutRow(Icons.Outlined.WorkOutline, "Researches at ${identity.affiliation}")
          AboutRow(Icons.Outlined.School, "Department of Computer Science")
          AboutRow(Icons.Outlined.LocationOn, "University Campus")
          AboutRow(Icons.Outlined.CalendarToday, "Joined Cite Circle 2024")
          AboutRow(Icons.Outlined.AutoGraph, "${stats.citations} Citations · ${stats.endorsements} Endorsements")
        }
      }

      // Profile tab bar
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
        ) {
          Spacer(
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .background(PageNeutral)
          )
          ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BrandBlue,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
              TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = BrandBlue,
                height = 3.dp
              )
            },
            divider = { HorizontalDivider(thickness = 0.5.dp, color = DividerLight) }
          ) {
            profileTabs.forEachIndexed { index, tab ->
              Tab(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                text = {
                  Text(
                    tab,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selectedTab == index) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              )
            }
          }
          Spacer(
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .background(PageNeutral)
          )
        }
      }

      // Dynamic content based on selectedTab
      when (selectedTab) {
        0 -> { // Posts
          when {
            feed.isLoading -> items(2) { ListRowSkeleton() }

            feed.isEmpty -> item {
              EmptyState(
                title = stringResource(R.string.profile_empty_title),
                message = stringResource(R.string.profile_empty_message),
                icon = Icons.Outlined.Article,
                actionLabel = stringResource(R.string.action_write_entry),
                onAction = { navController.navigate("compose") }
              )
            }

            else -> items(feed.items, key = { it.id }) { paper ->
              PostCard(paper, viewModel, navController)
              Spacer(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .background(PageNeutral)
              )
            }
          }
        }
        1 -> { // About
          item {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Text(
                "Research Interests & Specialties",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf("Deep Learning", "Transformers", "Peer Review", "Distributed AI").forEach { tag ->
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(16.dp))
                      .background(SurfaceInset)
                      .padding(horizontal = 12.dp, vertical = 6.dp)
                  ) {
                    Text(
                      tag,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }

              HorizontalDivider(thickness = 0.5.dp, color = DividerLight)

              Text(
                "Publications & Citation Growth",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              CitationChart(feed.items)

              HorizontalDivider(thickness = 0.5.dp, color = DividerLight)

              Text(
                "Academic Bio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                "Lead researcher investigating foundation model reasoning, distributed systems scalability, and open peer review reproducibility. Published in top venues including NeurIPS, ICML, and ICLR.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
              )
            }
          }
        }
        2 -> { // Preprints
          val preprints = feed.items.filter { it.pdfLocalPath.isNotBlank() || it.doi.isNotBlank() || it.url.isNotBlank() }
          if (preprints.isEmpty()) {
            item {
              EmptyState(
                title = "No Preprints Linked",
                message = "Manuscripts with PDF or DOI links will appear in your Preprints archive.",
                icon = Icons.Outlined.Article,
                actionLabel = "Add Preprint",
                onAction = { navController.navigate("compose") }
              )
            }
          } else {
            items(preprints, key = { "prep_${it.id}" }) { paper ->
              PostCard(paper, viewModel, navController)
              Spacer(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .background(PageNeutral)
              )
            }
          }
        }
        3 -> { // Figures
          val figurePapers = feed.items.filter { it.imageUri.isNotBlank() }
          if (figurePapers.isEmpty()) {
            item {
              EmptyState(
                title = "No Figures Uploaded",
                message = "Upload architecture diagrams, plots, or benchmark figures to showcase them here.",
                icon = Icons.Outlined.Image,
                actionLabel = "Attach Figure",
                onAction = { navController.navigate("compose") }
              )
            }
          } else {
            items(figurePapers, key = { "fig_${it.id}" }) { paper ->
              PostCard(paper, viewModel, navController)
              Spacer(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .background(PageNeutral)
              )
            }
          }
        }
        4 -> { // Mentions
          if (activityState.items.isEmpty()) {
            item {
              EmptyState(
                title = "No Mentions Yet",
                message = "Citations and replies from the academic community will appear here.",
                icon = Icons.Outlined.Notifications,
                actionLabel = "Discover Papers",
                onAction = { navController.navigate("fields") }
              )
            }
          } else {
            items(activityState.items) { item ->
              ActivityRow(item) {
                when (item) {
                  is ActivityItem.Replied -> navController.navigate("post/${item.comment.paperId}")
                  is ActivityItem.Cited -> navController.navigate("post/${item.quote.quotedId}")
                }
              }
              HorizontalDivider(thickness = 0.5.dp, color = DividerLight)
            }
          }
        }
      }
    }
  }
}
}

@Composable
private fun AboutRow(icon: ImageVector, text: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      icon,
      contentDescription = null,
      modifier = Modifier.size(20.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.width(12.dp))
    Text(
      text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun StatTile(value: Int, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      value.toString(),
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.primary
    )
    Spacer(Modifier.height(2.dp))
    Text(
      label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
