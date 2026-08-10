package com.example

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
    currentRoute.startsWith("quote/")

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
      composable("fields") { FieldsScreen() }
      composable("lists") { com.example.ui.lists.ReadingListsScreen(viewModel, navController) }
      composable("opps") { com.example.ui.opportunities.OpportunitiesScreen() }
      composable("profile") { ProfileScreen(viewModel, navController) }
      composable("chat") { com.example.ui.chat.ChatScreen() }
      composable("notifications") { NotificationsScreen(navController) }
      composable("notification_detail") { NotificationDetailScreen(navController) }
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
fun CitationChart() {
  Card(
    modifier = Modifier.fillMaxWidth().height(180.dp),
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
        Text(
          "Citation impact",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            "h-index ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            "24",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
      val lineColor = MaterialTheme.colorScheme.primary
      androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path()
        val dataPoints = listOf(10f, 15f, 30f, 25f, 40f, 60f, 85f, 110f)
        val maxPoint = dataPoints.maxOrNull() ?: 1f
        val width = size.width
        val height = size.height
        val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

        dataPoints.forEachIndexed { index, value ->
          val x = index * stepX
          val y = height - ((value / maxPoint) * height)
          if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
          drawCircle(color = lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
      }
    }
  }
}

@Composable
fun NotificationsScreen(navController: NavController) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = Gutter, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(
        "Notifications",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(vertical = 8.dp)
      )
    }
    item { CitationNotificationCard(read = false, navController = navController) }
    item { CitationNotificationCard(read = true, navController = navController) }
  }
}

@Composable
fun CitationNotificationCard(read: Boolean, navController: NavController) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { navController.navigate("notification_detail") },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = MaterialTheme.shapes.medium,
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (!read) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary)
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          "New citation",
          style = MaterialTheme.typography.labelLarge,
          color = if (read) MaterialTheme.colorScheme.onSurfaceVariant
          else MaterialTheme.colorScheme.primary
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        "Your publication has been formally referenced by Dr. Julian Thorne in a new preprint released to the Theoretical Physics circle.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface
      )

      Spacer(modifier = Modifier.height(14.dp))
      Text(
        "Entropy and the Architecture of Distributed Knowledge Systems",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        "Published Oct 2023 · CC-882-XJ",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(14.dp))
      QuoteBlock(
        "…as proposed in Thorne's recent synthesis, the friction within localized data " +
          "clusters mirrors the thermodynamic decay observed in early archival structures " +
          "(Thorne, 2023)."
      )

      Spacer(modifier = Modifier.height(14.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar("JT", 32.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            "Dr. Julian Thorne",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            "CERN",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

/** A pulled quotation, marked by a rule rather than by quotation styling alone. */
@Composable
fun QuoteBlock(text: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.small)
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .height(IntrinsicSize.Min)
  ) {
    Box(
      modifier = Modifier
        .width(3.dp)
        .fillMaxHeight()
        .background(MaterialTheme.colorScheme.primary)
    )
    Text(
      text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(14.dp)
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldsScreen() {
  var searchQuery by remember { mutableStateOf("") }
  val fields = listOf(
    "Theoretical Physics", "Molecular Biology", "Ancient History",
    "Computational Linguistics", "Cognitive Science", "Macroeconomics"
  )
  val filteredFields = fields.filter { it.contains(searchQuery, ignoreCase = true) }

  Column(modifier = Modifier.fillMaxSize()) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier.fillMaxWidth().padding(Gutter),
      placeholder = {
        Text(
          "Search fields",
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

    if (filteredFields.isEmpty()) {
      EmptyState(
        title = "No fields found",
        message = "Try a different search term.",
        icon = Icons.Outlined.SearchOff
      )
    } else {
      LazyColumn(
        contentPadding = PaddingValues(horizontal = Gutter, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filteredFields) { field ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(MaterialTheme.shapes.medium)
              .background(MaterialTheme.colorScheme.surface)
              .clickable { }
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                field,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                "${(fields.indexOf(field) + 1) * 120} researchers",
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
    }
  }
}

@Composable
fun ProfileScreen(viewModel: HomeViewModel, navController: NavController) {
  val feed by viewModel.feed.collectAsStateWithLifecycle()
  val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
  val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

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
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Avatar("JD", 56.dp)
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text(
                  "Dr. Jane Doe",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  "Senior Researcher · Oxford",
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
        }
      }

      item { CitationChart() }

      item {
        Text(
          "Publications",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = 8.dp)
        )
      }

      if (feed.isLoading) {
        items(2) { ListRowSkeleton() }
      } else {
        items(feed.items, key = { it.id }) { paper ->
          PostCard(paper, viewModel, navController)
        }
      }
    }
  }
}

@Composable
fun NotificationDetailScreen(navController: NavController) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
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
        "Citation details",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    Card(
      modifier = Modifier.fillMaxWidth().padding(Gutter),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = MaterialTheme.shapes.medium,
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        SectionHeading("Citation context")
        Spacer(modifier = Modifier.height(10.dp))
        QuoteBlock(
          "…as proposed in Thorne's recent synthesis, the friction within localized data " +
            "clusters mirrors the thermodynamic decay observed in early archival structures " +
            "(Thorne, 2023)."
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(20.dp))

        SectionHeading("Citing author")
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Avatar("JT", 40.dp)
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              "Dr. Julian Thorne",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              "CERN · Theoretical Physics",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(20.dp))

        SectionHeading("Referenced section")
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          "Section 4.2: Thermodynamic Decay in Archival Structures",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = { },
          modifier = Modifier.fillMaxWidth().height(48.dp),
          shape = MaterialTheme.shapes.extraLarge,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          )
        ) {
          Text("View full paper", style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}

/** Sentence-case section heading, replacing the all-caps 2sp-tracked micro-labels. */
@Composable
fun SectionHeading(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
}
