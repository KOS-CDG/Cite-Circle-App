package com.example.ui.navigation

/**
 * Route constants. Previously these were bare string literals scattered across the NavHost, the
 * bottom bar, and the top bar, with no single place to see the destination set.
 *
 * Note ASSISTANT: the old route name for the Gemini AI chat was "chat", which is the name the
 * person-to-person messenger wants. Renamed now so the messenger can claim `messages/...`
 * cleanly when it lands.
 */
object Routes {
    const val AUTH = "auth"
    const val FEED = "feed"
    const val DISCOVER = "discover"
    const val LIBRARY = "library"
    const val PROFILE = "profile"

    /** The Gemini AI assistant. Not messaging -- see the messenger routes below. */
    const val ASSISTANT = "assistant"

    const val NOTIFICATIONS = "notifications"
    const val NOTIFICATION_DETAIL = "notification_detail"

    // Reserved for the messenger (Phase 4). `messages/thread/{id}` rather than `messages/{id}`
    // so it can never collide with `messages/new`.
    const val MESSAGES = "messages"
    const val THREAD_ARG = "conversationId"
    const val THREAD = "messages/thread/{conversationId}"
    const val NEW_MESSAGE = "messages/new"

    fun thread(conversationId: String) = "messages/thread/$conversationId"

    /** Destinations that hide the app chrome (top bar and bottom bar). */
    val chromeless = setOf(AUTH, ASSISTANT, NOTIFICATION_DETAIL, THREAD, NEW_MESSAGE)
}
