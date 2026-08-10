package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every destination in the app, in one place.
 *
 * Routes that take an argument expose both the pattern (registered with `composable`) and a
 * builder that fills the argument in. Compare against [PAPER_DETAIL] and friends rather than
 * a literal, because `NavDestination.route` reports the *pattern* — `paper_detail/{paperId}` —
 * not the resolved path.
 */
object Routes {

    const val ONBOARDING = "onboarding"
    const val AUTH = "auth"

    const val FEED = "feed"
    const val FIELDS = "fields"
    const val LISTS = "lists"
    const val OPPS = "opps"
    const val PROFILE = "profile"

    const val CHAT = "chat"
    const val NOTIFICATIONS = "notifications"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val COMPOSE_PAPER = "compose_paper"
    const val PROFILE_EDIT = "profile_edit"

    const val ARG_PAPER_ID = "paperId"
    const val ARG_LIST_ID = "listId"
    const val ARG_OPPORTUNITY_ID = "opportunityId"
    const val ARG_FIELD_ID = "fieldId"
    const val ARG_NOTIFICATION_ID = "notificationId"
    const val ARG_USER_ID = "userId"

    const val PAPER_DETAIL = "paper_detail/{$ARG_PAPER_ID}"
    const val LIST_DETAIL = "list_detail/{$ARG_LIST_ID}"
    const val LIST_EDITOR = "list_editor?$ARG_LIST_ID={$ARG_LIST_ID}"
    const val OPPORTUNITY_DETAIL = "opportunity_detail/{$ARG_OPPORTUNITY_ID}"
    const val FIELD_DETAIL = "field_detail/{$ARG_FIELD_ID}"
    const val NOTIFICATION_DETAIL = "notification_detail/{$ARG_NOTIFICATION_ID}"
    const val USER_PROFILE = "user_profile/{$ARG_USER_ID}"

    fun paperDetail(paperId: String) = "paper_detail/$paperId"

    fun listDetail(listId: String) = "list_detail/$listId"

    /** Pass no id to create a new list, or an existing id to edit it. */
    fun listEditor(listId: String? = null) =
        if (listId == null) "list_editor" else "list_editor?$ARG_LIST_ID=$listId"

    fun opportunityDetail(opportunityId: String) = "opportunity_detail/$opportunityId"

    fun fieldDetail(fieldId: String) = "field_detail/$fieldId"

    fun notificationDetail(notificationId: String) = "notification_detail/$notificationId"

    fun userProfile(userId: String) = "user_profile/$userId"
}

/** A tab in the bottom navigation bar. */
data class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val topLevelDestinations = listOf(
    TopLevelDestination(Routes.FEED, "Feed", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    TopLevelDestination(Routes.FIELDS, "Fields", Icons.Filled.Folder, Icons.Outlined.Folder),
    TopLevelDestination(Routes.LISTS, "Lists", Icons.Filled.LibraryBooks, Icons.Outlined.LibraryBooks),
    TopLevelDestination(
        Routes.OPPS,
        "Opportunities",
        Icons.Filled.BusinessCenter,
        Icons.Outlined.BusinessCenter,
    ),
    TopLevelDestination(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)

private val topLevelRoutes = topLevelDestinations.map { it.route }.toSet()

/** Top-level routes get the registry header and the bottom bar; everything else is full-screen. */
fun isTopLevelRoute(route: String?) = route in topLevelRoutes
