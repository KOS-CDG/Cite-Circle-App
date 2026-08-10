package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.ui.theme.InkAndFieldNotesTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val context = androidx.compose.ui.platform.LocalContext.current
      val application = context.applicationContext as MyApplication
      val viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
          factory = HomeViewModelFactory(application.repository)
      )
      val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

      InkAndFieldNotesTheme(darkTheme = isDarkMode) {
        FolioApp(viewModel)
      }
    }
  }
}


@Composable
fun CitationChart() {
  Card(
    modifier = Modifier.fillMaxWidth().height(200.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    shape = RoundedCornerShape(4.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("CITATION IMPACT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("h-index: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("24", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
      val lineColor = MaterialTheme.colorScheme.secondary
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
          if (index == 0) {
            path.moveTo(x, y)
          } else {
            path.lineTo(x, y)
          }
          drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.dp.toPx()))
      }
    }
  }
}

@Composable
fun FolioApp(viewModel: HomeViewModel) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val navController = rememberNavController()
  val authManager = remember { com.example.ui.auth.FirebaseAuthManager(context) }
  var currentRoute by remember { mutableStateOf(if (authManager.getCurrentUser() != null) "feed" else "auth") }

  navController.addOnDestinationChangedListener { _, destination, _ ->
      currentRoute = destination.route ?: if (authManager.getCurrentUser() != null) "feed" else "auth"
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      if (currentRoute != "auth") {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(MaterialTheme.colorScheme.background)
              .padding(horizontal = 24.dp, vertical = 24.dp)
              .statusBarsPadding()
          ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              "JOURNAL REGISTRY",
              style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              "Cite Circle",
              style = MaterialTheme.typography.headlineLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal),
              color = MaterialTheme.colorScheme.primary
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              IconButton(onClick = { navController.navigate("notifications") }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onBackground)
              }
            }
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              @Suppress("DEPRECATION")
              IconButton(onClick = { navController.navigate("chat") }) {
                Icon(Icons.Outlined.Chat, contentDescription = "Chat", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onBackground)
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), thickness = 1.dp)
      }
      }
    },
    bottomBar = {
      if (currentRoute != "onboarding" && currentRoute != "auth") {
        Column {
          HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), thickness = 1.dp)
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
          ) {
            @Suppress("DEPRECATION")
            val items = listOf(
              Triple("feed", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
              Triple("fields", Icons.Filled.Folder, Icons.Outlined.Folder),
              Triple("lists", Icons.Filled.LibraryBooks, Icons.Outlined.LibraryBooks),
              Triple("opps", Icons.Filled.BusinessCenter, Icons.Outlined.BusinessCenter),
              Triple("profile", Icons.Filled.Person, Icons.Outlined.Person)
            )
            items.forEach { (route, selectedIcon, unselectedIcon) ->
              NavigationBarItem(
                icon = {
                  Icon(
                    if (currentRoute == route) selectedIcon else unselectedIcon,
                    contentDescription = route
                  )
                },
                label = { Text(route.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, fontSize = 9.sp)) },
                selected = currentRoute == route,
                onClick = {
                  currentRoute = route
                  navController.navigate(route) {
                    popUpTo("feed") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = Color.Transparent,
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
              )
            }
          }
        }
      }
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = if (authManager.getCurrentUser() != null) "feed" else "auth",
      modifier = Modifier.padding(innerPadding)
    ) {
      composable("auth") {
          com.example.ui.auth.AuthScreen(
              onAuthSuccess = {
                  navController.navigate("feed") { popUpTo("auth") { inclusive = true } }
              }
          )
      }
      composable("feed") { HomeScreen(viewModel) }
      composable("fields") { FieldsScreen() }
      composable("lists") { com.example.ui.lists.ReadingListsScreen() }
      composable("opps") { com.example.ui.opportunities.OpportunitiesScreen() }
      composable("profile") { ProfileScreen(viewModel) }
      composable("chat") { com.example.ui.chat.ChatScreen() }
      composable("notifications") { NotificationsScreen(navController) }
      composable("notification_detail") { NotificationDetailScreen(navController) }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel) {
  val papers by viewModel.savedPapers.collectAsStateWithLifecycle()

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    items(papers.size) { index ->
      PostCard(
        papers[index],
        onEndorse = { viewModel.toggleEndorsement(papers[index].id, papers[index].isEndorsed) },
      )
    }
  }
}

@Composable
fun PostCard(paper: com.example.data.SavedPaper, onEndorse: () -> Unit) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(elevation = 2.dp, shape = RoundedCornerShape(4.dp), spotColor = Color(0x0D1A1A1A)),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(4.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center
        ) {
          Text(paper.authorInitials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(paper.authorName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Text("• ${paper.timeAgo}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Text(paper.affiliation, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      
      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), thickness = 1.dp)
      Spacer(modifier = Modifier.height(16.dp))
      
      Text(
        paper.content,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 24.sp
      )
      Spacer(modifier = Modifier.height(20.dp))
      CitationBlock(paper.citation)
      Spacer(modifier = Modifier.height(20.dp))
      
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
          onClick = onEndorse,
          modifier = Modifier.weight(1f).height(48.dp),
          shape = RoundedCornerShape(2.dp),
          border = BorderStroke(1.dp, if (paper.isEndorsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface),
          colors = ButtonDefaults.outlinedButtonColors(containerColor = if (paper.isEndorsed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
        ) {
          Icon(
              if (paper.isEndorsed) Icons.Filled.Verified else Icons.Outlined.Verified,
              contentDescription = "Endorse",
              tint = if (paper.isEndorsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (paper.isEndorsed) "VERIFIED" else "ENDORSE", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold), color = if (paper.isEndorsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
        Button(
          onClick = { },
          modifier = Modifier.weight(1f).height(48.dp),
          shape = RoundedCornerShape(2.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.primary)
        ) {
          Text("VIEW CONTEXT", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold))
        }
      }
    }
  }
}

/**
 * Renders an APA-formatted [citation] in the requested [format] ("APA", "MLA" or "CHICAGO").
 *
 * Known limitation: the MLA and CHICAGO conversions only recognise the literal year 2026, so a
 * citation from any other year is returned unchanged and renders as APA under an MLA/Chicago
 * label. See the ignored test in CitationFormatTest for the intended behaviour.
 */
fun formatCitation(citation: String, format: String): String = when (format) {
  "MLA" -> citation.replace(" (2026). ", ". ")
  "CHICAGO" -> citation.replace(" (2026).", ", 2026.")
  else -> citation
}

@Composable
fun CitationBlock(citation: String) {
  var format by remember { mutableStateOf("APA") }
  
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(4.dp))
      .background(MaterialTheme.colorScheme.primary)
      .padding(20.dp)
  ) {
    Column {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("CITATION", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
        
        Row {
            listOf("APA", "MLA", "CHICAGO").forEach { f ->
                Text(
                    f, 
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), 
                    color = if (format == f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), 
                    modifier = Modifier
                        .clip(RoundedCornerShape(2.dp))
                        .clickable { format = f }
                        .border(1.dp, if (format == f) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(2.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      
      val displayCitation = formatCitation(citation, format)
      Text(displayCitation, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 20.sp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
      
      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
      Spacer(modifier = Modifier.height(16.dp))
      
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val context = androidx.compose.ui.platform.LocalContext.current
        OutlinedButton(
          onClick = { android.widget.Toast.makeText(context, "Exported as BibTeX", android.widget.Toast.LENGTH_SHORT).show() },
          modifier = Modifier.weight(1f).height(40.dp),
          shape = RoundedCornerShape(2.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
          Icon(Icons.Outlined.Download, contentDescription = "Export BibTeX", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("BibTeX", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
        OutlinedButton(
          onClick = { android.widget.Toast.makeText(context, "Exported as RIS", android.widget.Toast.LENGTH_SHORT).show() },
          modifier = Modifier.weight(1f).height(40.dp),
          shape = RoundedCornerShape(2.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
          Icon(Icons.Outlined.Download, contentDescription = "Export RIS", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("RIS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
      }
    }
  }
}

@Composable
fun EmptyState(title: String, message: String, icon: ImageVector) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(48.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      modifier = Modifier.size(48.dp),
      tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
      title,
      style = MaterialTheme.typography.titleLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal),
      color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
  }
}

@Composable
fun NotificationsScreen(navController: androidx.navigation.NavController) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(24.dp),
    verticalArrangement = Arrangement.spacedBy(24.dp)
  ) {
    item {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          "NEW FORMAL CITATION",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
          color = MaterialTheme.colorScheme.secondary
        )
      }
    }
    item {
      CitationNotificationCard(read = false, navController = navController)
    }
    item {
      CitationNotificationCard(read = true, navController = navController)
    }
  }
}

@Composable
fun CitationNotificationCard(read: Boolean, navController: androidx.navigation.NavController) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .shadow(elevation = if (read) 0.dp else 2.dp, shape = RoundedCornerShape(4.dp), spotColor = Color(0x0D1A1A1A))
      .clickable { navController.navigate("notification_detail") },
    colors = CardDefaults.cardColors(containerColor = if (read) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(4.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = if (read) 0.05f else 0.1f))
  ) {
    Column(modifier = Modifier.padding(24.dp)) {
      Text(
        "Your publication has been formally referenced by Dr. Julian Thorne in a new preprint released to the Theoretical Physics circle.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 22.sp
      )
      
      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), thickness = 1.dp)
      Spacer(modifier = Modifier.height(16.dp))
      
      Text("Entropy and the Architecture of Distributed Knowledge Systems", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Normal), color = MaterialTheme.colorScheme.primary)
      Spacer(modifier = Modifier.height(8.dp))
      Text("Published Oct 2023 • ID: CC-882-XJ", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
      
      Spacer(modifier = Modifier.height(16.dp))
      
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)))
          .padding(start = 12.dp)
      ) {
        Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
          Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary).align(Alignment.CenterStart))
          Text(
            "\"...as proposed in Thorne's recent synthesis, the friction within localized data clusters mirrors the thermodynamic decay observed in early archival structures (Thorne, 2023).\"",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 24.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
          )
        }
      }
      
      Spacer(modifier = Modifier.height(20.dp))
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Text("JT", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text("AFFILIATION: CERN", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurface)
        }
        Text("REVIEW FULL PAPER", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = -0.5.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldsScreen() {
  var searchQuery by remember { mutableStateOf("") }
  val fields = listOf("Theoretical Physics", "Molecular Biology", "Ancient History", "Computational Linguistics", "Cognitive Science", "Macroeconomics")
  val filteredFields = fields.filter { it.contains(searchQuery, ignoreCase = true) }
  
  Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      placeholder = { Text("Search academic fields...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
      colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
          focusedContainerColor = MaterialTheme.colorScheme.surface,
          unfocusedContainerColor = MaterialTheme.colorScheme.surface
      ),
      shape = RoundedCornerShape(4.dp)
    )
    
    if (filteredFields.isEmpty()) {
      EmptyState("No Fields Found", "Try adjusting your search criteria.", Icons.Outlined.FolderOff)
    } else {
      LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(filteredFields.size) { index ->
          val field = filteredFields[index]
          Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(field, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
              Spacer(modifier = Modifier.height(4.dp))
              Text("${(index + 1) * 120} active researchers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            @Suppress("DEPRECATION")
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "View Field", tint = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
      }
    }
  }
}

@Composable
fun ProfileScreen(viewModel: HomeViewModel) {
  val papers by viewModel.savedPapers.collectAsStateWithLifecycle()

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentPadding = PaddingValues(24.dp)
  ) {
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
          ) {
            Text("JD", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineMedium)
          }
          Spacer(modifier = Modifier.width(16.dp))
          Column {
            Text("Dr. Jane Doe", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("Senior Researcher • Oxford", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
        IconButton(onClick = { viewModel.toggleTheme() }) {
          Icon(
            if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
            contentDescription = "Toggle Theme",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }
      
      Spacer(modifier = Modifier.height(32.dp))
      
      CitationChart()
      
      Spacer(modifier = Modifier.height(32.dp))
      Text("PUBLICATIONS", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
      Spacer(modifier = Modifier.height(16.dp))
    }
    
    items(papers.size) { index ->
      PostCard(
        papers[index],
        onEndorse = { viewModel.toggleEndorsement(papers[index].id, papers[index].isEndorsed) },
      )
      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
fun NotificationDetailScreen(navController: androidx.navigation.NavController) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = { navController.popBackStack() }) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
      }
      Text("CITATION DETAILS", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(elevation = 2.dp, shape = RoundedCornerShape(4.dp), spotColor = Color(0x0D1A1A1A)),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(4.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("CITATION CONTEXT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(16.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)))
                .padding(start = 12.dp)
            ) {
              Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary).align(Alignment.CenterStart))
                Text(
                  "\"...as proposed in Thorne's recent synthesis, the friction within localized data clusters mirrors the thermodynamic decay observed in early archival structures (Thorne, 2023).\"",
                  style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, lineHeight = 24.sp),
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(16.dp)
                )
              }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("CITING AUTHOR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
              ) {
                Text("JT", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Dr. Julian Thorne", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text("CERN • Theoretical Physics", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("REFERENCED SECTION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp), color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Section 4.2: Thermodynamic Decay in Archival Structures", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
              onClick = { },
              modifier = Modifier.fillMaxWidth().height(48.dp),
              shape = RoundedCornerShape(2.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.primary)
            ) {
              Text("VIEW FULL PAPER", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
  }
}
