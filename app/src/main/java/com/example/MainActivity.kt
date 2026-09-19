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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.AuthorIdentity
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
import com.example.ui.theme.CiteCircleTheme

/** Horizontal page gutter. Narrower than the old 24dp so cards read wider, as in a feed. */
private val Gutter = 16.dp

/** Destinations reached by going deeper, as opposed to the five peer tabs. */
private fun isPushedRoute(route: String?): Boolean {
  val r = route.orEmpty()
  return r == "compose" || r == "chat" || r == "notifications" || r == "messenger" ||
    r.startsWith("post/") || r.startsWith("quote/") || r.startsWith("share/") ||
    r.startsWith("edit/") || r.startsWith("image/") || r.startsWith("venue/") ||
    r.startsWith("chat_thread/") || r.startsWith("pdf_viewer")
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = androidx.compose.ui.platform.LocalContext.current
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
  val unselectedIcon: ImageVector
)

private val NavItems = listOf(
  NavItem("feed", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
  NavItem("fields", R.string.nav_discover, Icons.Filled.Explore, Icons.Outlined.Explore),
  NavItem("lists", R.string.nav_saved, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
  NavItem("opps", R.string.nav_jobs, Icons.Filled.Work, Icons.Outlined.WorkOutline),
  NavItem("profile", R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun FolioApp(viewModel: HomeViewModel) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val navController = rememberNavController()
  val authManager = remember { com.example.ui.auth.FirebaseAuthManager(context) }
  val startDestination = remember { if (authManager.getCurrentUser() != null) "feed" else "auth" }

  // Derived from the back stack rather than an addOnDestinationChangedListener call in the
  // composable body — that registered a fresh, never-removed listener on every recomposition.
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: startDestination

  // Screens that supply their own header and should not sit inside the app chrome.
  val chromeless = currentRoute == "auth" ||
    currentRoute == "compose" ||
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

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButton = {
      if (currentRoute == "feed") {
        FloatingActionButton(
          onClick = { navController.navigate("compose") },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
          Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_new_entry))
        }
      }
    },
    topBar = { if (!chromeless) AppTopBar(navController, viewModel) },
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
              }
          )
      }
      composable("feed") { HomeScreen(viewModel, navController) }
      composable("fields") { FieldsScreen(viewModel, navController) }
      composable("lists") { com.example.ui.lists.ReadingListsScreen(viewModel, navController) }
      composable("opps") { com.example.ui.opportunities.OpportunitiesScreen() }
      composable("profile") { ProfileScreen(viewModel, navController) }
      composable("chat") { com.example.ui.chat.ChatScreen() }
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
 * Compact app bar: wordmark left, actions right, on a white surface.
 *
 * Replaces a 100dp-tall two-line masthead with an italic serif title, which read as a
 * magazine cover rather than an app.
 */
@Composable
private fun AppTopBar(navController: NavController, viewModel: HomeViewModel) {
  val unread by viewModel.unreadActivityCount.collectAsStateWithLifecycle()
  val context = androidx.compose.ui.platform.LocalContext.current
  val app = context.applicationContext as MyApplication
  val unreadMessages by app.chatRepository.totalUnreadCount.collectAsStateWithLifecycle(initialValue = 0)

  Surface(color = MaterialTheme.colorScheme.surface) {
    Column(modifier = Modifier.statusBarsPadding()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .padding(start = Gutter, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.primary
        )
        Row {
          IconButton(onClick = { navController.navigate("notifications") }) {
            BadgedBox(
              badge = {
                if (unread > 0) {
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
              }
            ) {
              Icon(
                Icons.Outlined.Notifications,
                contentDescription = if (unread > 0) {
                  stringResource(R.string.cd_activity_unread, unread)
                } else {
                  stringResource(R.string.cd_activity)
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          IconButton(onClick = { navController.navigate("messenger") }) {
            BadgedBox(
              badge = {
                val count = unreadMessages ?: 0
                if (count > 0) {
                  Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                  ) {
                    Text(
                      if (count > 99) stringResource(R.string.badge_overflow)
                      else count.toString()
                    )
                  }
                }
              }
            ) {
              Icon(
                Icons.Outlined.Chat,
                contentDescription = stringResource(R.string.cd_messages),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

@Composable
private fun AppBottomBar(
  navController: NavController,
  currentRoute: String,
  viewModel: HomeViewModel
) {
  Column {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    NavigationBar(
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.onSurface,
      tonalElevation = 0.dp,
      modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
      NavItems.forEach { item ->
        val selected = currentRoute == item.route
        val label = stringResource(item.label)
        NavigationBarItem(
          icon = {
            Icon(
              if (selected) item.selectedIcon else item.unselectedIcon,
              contentDescription = label
            )
          },
          // 12sp, up from a 9sp label with negative tracking that fell below the
          // minimum legible size.
          label = { Text(label, style = MaterialTheme.typography.labelSmall) },
          selected = selected,
          onClick = {
            // Re-tapping the tab you are already on scrolls that screen back to the top,
            // as it does in both reference apps, rather than re-navigating to itself.
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
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        )
      }
    }
  }
}

@Composable
fun HomeScreen(viewModel: HomeViewModel, navController: NavController) {
  val feed by viewModel.feed.collectAsStateWithLifecycle()
  val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()

  LaunchedEffect(Unit) {
    viewModel.scrollToTop.collect { route ->
      if (route == "feed") listState.animateScrollToItem(0)
    }
  }

  RefreshableBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
    when {
      feed.isLoading -> FeedSkeleton()

      feed.isEmpty -> EmptyState(
        title = stringResource(R.string.feed_empty_title),
        message = stringResource(R.string.feed_empty_message),
        icon = Icons.Outlined.Article,
        modifier = Modifier.fillMaxSize().wrapContentHeight(),
        actionLabel = stringResource(R.string.action_write_entry),
        onAction = { navController.navigate("compose") }
      )

      else -> LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(feed.items, key = { it.id }) { paper ->
          PostCard(paper, viewModel, navController)
        }
      }
    }
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
fun Avatar(initials: String, size: androidx.compose.ui.unit.Dp) {
  Box(
    modifier = Modifier
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
    modifier = Modifier.fillMaxWidth().height(180.dp),
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
 * activity rather than as notifications.
 */
@Composable
fun NotificationsScreen(viewModel: HomeViewModel, navController: NavController) {
  val activity by viewModel.activity.collectAsStateWithLifecycle()

  // Opening the screen is what clears the badge, keyed on the newest entry so arriving
  // activity while the screen is open is also marked read.
  val newest = activity.items.firstOrNull()?.timestamp ?: 0L
  LaunchedEffect(newest) { viewModel.markActivitySeen(newest) }

  Column(modifier = Modifier.fillMaxSize()) {
    Text(
      stringResource(R.string.activity_title),
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onBackground,
      modifier = Modifier.padding(horizontal = Gutter, vertical = 12.dp)
    )

    when {
      activity.isLoading -> Column(
        modifier = Modifier.padding(horizontal = Gutter),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) { repeat(3) { ListRowSkeleton() } }

      activity.isEmpty -> EmptyState(
        title = stringResource(R.string.activity_empty_title),
        message = stringResource(R.string.activity_empty_message),
        icon = Icons.Outlined.Notifications
      )

      else -> LazyColumn(
        contentPadding = PaddingValues(horizontal = Gutter, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(activity.items, key = { it.timestamp.toString() + it.targetPaperId }) { item ->
          ActivityRow(item) { navController.navigate("post/${item.targetPaperId}") }
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

  when (item) {
    is ActivityItem.Replied -> {
      initials = item.comment.authorInitials
      headline = stringResource(R.string.activity_replied, item.comment.authorName)
      body = item.comment.body
      icon = Icons.Outlined.ChatBubbleOutline
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
    }
  }

  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Row(modifier = Modifier.padding(16.dp)) {
      Avatar(initials, 40.dp)
      Spacer(Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(Modifier.width(6.dp))
          Text(
            headline,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        }
        if (item is ActivityItem.Replied && item.paperTitle.isNotBlank()) {
          Spacer(Modifier.height(2.dp))
          Text(
            stringResource(R.string.activity_on_entry, item.paperTitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
          )
        }
        Spacer(Modifier.height(6.dp))
        Text(
          body,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 3,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(6.dp))
        Text(
          formatTimeAgo(item.timestamp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
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

  val trimmed = query.trim()
  val matchingVenues = venues.items.filter { it.name.contains(trimmed, ignoreCase = true) }
  val matchingPosts = if (trimmed.isBlank()) emptyList() else feed.items.filter {
    it.title.contains(trimmed, ignoreCase = true) ||
      it.content.contains(trimmed, ignoreCase = true) ||
      it.authors.contains(trimmed, ignoreCase = true) ||
      it.authorName.contains(trimmed, ignoreCase = true)
  }

  Column(modifier = Modifier.fillMaxSize()) {
    OutlinedTextField(
      value = query,
      onValueChange = { query = it },
      modifier = Modifier.fillMaxWidth().padding(Gutter),
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
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
      ),
      shape = MaterialTheme.shapes.small
    )

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
        icon = Icons.Outlined.Search
      )

      else -> LazyColumn(
        contentPadding = PaddingValues(horizontal = Gutter, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (matchingVenues.isNotEmpty()) {
          item { DiscoverHeading(stringResource(R.string.discover_heading_venues)) }
          items(matchingVenues, key = { "venue-" + it.name }) { venue ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { navController.navigate("venue/" + Uri.encode(venue.name)) }
                .padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  venue.name,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  pluralStringResource(R.plurals.entry_count, venue.count, venue.count),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              @Suppress("DEPRECATION")
              Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
        if (matchingPosts.isNotEmpty()) {
          item { DiscoverHeading(stringResource(R.string.discover_heading_entries)) }
          items(matchingPosts, key = { "post-" + it.id }) { paper ->
            PostCard(paper, viewModel, navController)
          }
        }
      }
    }
  }
}

@Composable
private fun DiscoverHeading(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
  )
}

/** Every entry published in one venue. */
@Composable
fun VenueScreen(venue: String, viewModel: HomeViewModel, navController: NavController) {
  val flow = remember(venue) { viewModel.papersInVenue(venue) }
  val papers by flow.collectAsStateWithLifecycle(initialValue = ListState())

  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Row(
      modifier = Modifier.fillMaxWidth().statusBarsPadding().height(56.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = { navController.popBackStack() }) {
        Icon(
          Icons.Filled.ArrowBack,
          contentDescription = stringResource(R.string.cd_back),
          tint = MaterialTheme.colorScheme.onBackground
        )
      }
      Text(
        venue,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
      )
    }

    when {
      papers.isLoading -> Column(
        modifier = Modifier.padding(horizontal = Gutter),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) { repeat(2) { PostCardSkeleton() } }

      papers.isEmpty -> EmptyState(
        title = stringResource(R.string.venue_empty_title),
        message = stringResource(R.string.venue_empty_message),
        icon = Icons.Outlined.Search
      )

      else -> LazyColumn(
        contentPadding = PaddingValues(horizontal = Gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(papers.items, key = { it.id }) { paper ->
          PostCard(paper, viewModel, navController)
        }
      }
    }
  }
}

@Composable
fun ProfileScreen(viewModel: HomeViewModel, navController: NavController) {
  val feed by viewModel.feed.collectAsStateWithLifecycle()
  val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
  val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
  val profileContext = androidx.compose.ui.platform.LocalContext.current
  val identity = remember(profileContext) { AuthorIdentity.current(profileContext) }
  val stats = remember(feed.items) { ProfileStats.from(feed.items) }

  RefreshableBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = Gutter, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = MaterialTheme.shapes.medium,
          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(identity.initials, 56.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                  Text(
                    identity.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(Modifier.height(2.dp))
                  Text(
                    identity.affiliation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              IconButton(onClick = { viewModel.toggleTheme() }) {
                Icon(
                  if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                  contentDescription = if (isDarkMode) {
                    stringResource(R.string.cd_switch_to_light_theme)
                  } else {
                    stringResource(R.string.cd_switch_to_dark_theme)
                  },
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              StatTile(stats.entries, stringResource(R.string.stat_entries))
              StatTile(stats.endorsements, stringResource(R.string.stat_endorsements))
              StatTile(stats.citations, stringResource(R.string.stat_citations))
            }
          }
        }
      }

      item { CitationChart(feed.items) }

      item {
        Text(
          stringResource(R.string.profile_your_entries),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = 8.dp)
        )
      }

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
        }
      }
    }
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
