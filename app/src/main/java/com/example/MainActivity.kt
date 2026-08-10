package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.post.PostCard
import com.example.ui.post.PostDetailScreen
import com.example.ui.post.QuotePostScreen
import com.example.ui.share.SharePreviewScreen
import com.example.ui.theme.CiteCircleTheme

/** Horizontal page gutter. Narrower than the old 24dp so cards read wider, as in a feed. */
private val Gutter = 16.dp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = androidx.compose.ui.platform.LocalContext.current
      val application = context.applicationContext as MyApplication
      val viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
          factory = HomeViewModelFactory(application.repository, application)
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
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
)

private val NavItems = listOf(
  NavItem("feed", "Home", Icons.Filled.Home, Icons.Outlined.Home),
  NavItem("fields", "Discover", Icons.Filled.Explore, Icons.Outlined.Explore),
  NavItem("lists", "Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
  NavItem("opps", "Jobs", Icons.Filled.Work, Icons.Outlined.WorkOutline),
  NavItem("profile", "Me", Icons.Filled.Person, Icons.Outlined.Person)
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
    currentRoute.startsWith("venue/")

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      if (currentRoute == "feed") {
        FloatingActionButton(
          onClick = { navController.navigate("compose") },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
          Icon(Icons.Filled.Add, contentDescription = "New entry")
        }
      }
    },
    topBar = { if (!chromeless) AppTopBar(navController) },
    bottomBar = { if (!chromeless) AppBottomBar(navController, currentRoute) }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = Modifier.padding(innerPadding)
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
    }
  }
}

/**
 * Compact app bar: wordmark left, actions right, on a white surface.
 *
 * Replaces a 100dp-tall two-line masthead with an italic serif title, which read as a
 * magazine cover rather than an app.
 */
@Composable
private fun AppTopBar(navController: NavController) {
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
          "Cite Circle",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.primary
        )
        Row {
          IconButton(onClick = { navController.navigate("notifications") }) {
            Icon(
              Icons.Outlined.Notifications,
              contentDescription = "Notifications",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          @Suppress("DEPRECATION")
          IconButton(onClick = { navController.navigate("chat") }) {
            Icon(
              Icons.Outlined.Chat,
              contentDescription = "Assistant",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

@Composable
private fun AppBottomBar(navController: NavController, currentRoute: String) {
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
        NavigationBarItem(
          icon = {
            Icon(
              if (selected) item.selectedIcon else item.unselectedIcon,
              contentDescription = item.label
            )
          },
          // 12sp, up from a 9sp label with negative tracking that fell below the
          // minimum legible size.
          label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
          selected = selected,
          onClick = {
            navController.navigate(item.route) {
              popUpTo("feed") { saveState = true }
              launchSingleTop = true
              restoreState = true
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

  RefreshableBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
    when {
      feed.isLoading -> FeedSkeleton()

      feed.isEmpty -> EmptyState(
        title = "Your feed is empty",
        message = "Publish your first entry to start building your circle.",
        icon = Icons.Outlined.Article,
        modifier = Modifier.fillMaxSize().wrapContentHeight(),
        actionLabel = "Write an entry",
        onAction = { navController.navigate("compose") }
      )

      else -> LazyColumn(
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
        "Entries over time",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        "Last ${series.size} months",
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

  Column(modifier = Modifier.fillMaxSize()) {
    Text(
      "Activity",
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
        title = "Nothing has happened yet",
        message = "Replies and citations on entries in your circle will show up here.",
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
      headline = "${item.comment.authorName} replied"
      body = item.comment.body
      icon = Icons.Outlined.ChatBubbleOutline
    }
    is ActivityItem.Cited -> {
      initials = item.quote.authorInitials
      headline = "${item.quote.authorName} cited ${item.quote.quotedAuthorName}"
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
            "on ${item.paperTitle}",
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
          "Search venues and entries",
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
        title = if (trimmed.isBlank()) "Nothing to discover yet" else "No matches",
        message = if (trimmed.isBlank()) {
          "Venues appear here once entries in your library name one."
        } else {
          "No venue or entry matches \"$trimmed\"."
        },
        icon = Icons.Outlined.Search
      )

      else -> LazyColumn(
        contentPadding = PaddingValues(horizontal = Gutter, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (matchingVenues.isNotEmpty()) {
          item { DiscoverHeading("Venues") }
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
                  "${venue.count} ${if (venue.count == 1) "entry" else "entries"}",
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
          item { DiscoverHeading("Entries") }
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
          contentDescription = "Back",
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
        title = "No entries",
        message = "Nothing in your library names this venue any more.",
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
  val identity = remember { AuthorIdentity.current() }
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
                  contentDescription = if (isDarkMode) "Switch to light theme"
                  else "Switch to dark theme",
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
              StatTile(stats.entries, "Entries")
              StatTile(stats.endorsements, "Endorsements")
              StatTile(stats.citations, "Citations")
            }
          }
        }
      }

      item { CitationChart(feed.items) }

      item {
        Text(
          "Your entries",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = 8.dp)
        )
      }

      when {
        feed.isLoading -> items(2) { ListRowSkeleton() }

        feed.isEmpty -> item {
          EmptyState(
            title = "No entries yet",
            message = "Anything you publish appears here.",
            icon = Icons.Outlined.Article,
            actionLabel = "Write an entry",
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
