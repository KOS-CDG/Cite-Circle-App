package com.example.data.notifications

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.concurrent.TimeUnit

enum class NotificationType {
    CITATION,
    REACTION,
    COMMENT,
    CONNECTION_REQUEST,
    MESSAGE,
}

data class AppNotification(
    val id: String,
    val type: NotificationType,
    val actorId: String,
    val actorName: String,
    val actorInitials: String,
    /** Actor name is rendered separately and bolded, so this is the rest of the sentence. */
    val body: String,
    val detail: String?,
    val createdAt: Long,
    val isRead: Boolean,
)

interface NotificationRepository {
    fun observeAll(): Flow<List<AppNotification>>
    fun observeUnreadCount(): Flow<Int>
    fun byId(id: String): AppNotification?
    fun markRead(id: String)
    fun markAllRead()
}

/**
 * In-memory, like the rest of the mocked layers. The screen this replaces rendered exactly two
 * hardcoded cards with the body text as a string literal, and the "read" flag only changed a
 * shadow -- there was no notification model at all.
 */
class InMemoryNotificationRepository(
    private val now: () -> Long = System::currentTimeMillis,
) : NotificationRepository {

    private val notifications = MutableStateFlow(seed(now()))

    override fun observeAll(): Flow<List<AppNotification>> =
        notifications.map { list -> list.sortedByDescending { it.createdAt } }

    override fun observeUnreadCount(): Flow<Int> =
        notifications.map { list -> list.count { !it.isRead } }

    override fun byId(id: String): AppNotification? =
        notifications.value.firstOrNull { it.id == id }

    override fun markRead(id: String) {
        notifications.update { list ->
            list.map { if (it.id == id) it.copy(isRead = true) else it }
        }
    }

    override fun markAllRead() {
        notifications.update { list -> list.map { it.copy(isRead = true) } }
    }
}

private fun seed(now: Long): List<AppNotification> {
    fun minutes(n: Long) = now - TimeUnit.MINUTES.toMillis(n)
    fun hours(n: Long) = now - TimeUnit.HOURS.toMillis(n)
    fun days(n: Long) = now - TimeUnit.DAYS.toMillis(n)

    return listOf(
        AppNotification(
            id = "n-1",
            type = NotificationType.CITATION,
            actorId = "u-thorne",
            actorName = "Dr. Julian Thorne",
            actorInitials = "JT",
            body = "formally referenced your publication in a new preprint.",
            detail = "Entropy and the Architecture of Distributed Knowledge Systems",
            createdAt = minutes(12),
            isRead = false,
        ),
        AppNotification(
            id = "n-2",
            type = NotificationType.REACTION,
            actorId = "u-okafor",
            actorName = "Dr. Amara Okafor",
            actorInitials = "AO",
            body = "marked your post as cite-worthy.",
            detail = null,
            createdAt = minutes(48),
            isRead = false,
        ),
        AppNotification(
            id = "n-3",
            type = NotificationType.CONNECTION_REQUEST,
            actorId = "u-navarro",
            actorName = "Dr. Sofia Navarro",
            actorInitials = "SN",
            body = "wants to connect with you.",
            detail = "Attention and working memory under load",
            createdAt = hours(3),
            isRead = false,
        ),
        AppNotification(
            id = "n-4",
            type = NotificationType.COMMENT,
            actorId = "u-tanaka",
            actorName = "Dr. Kenji Tanaka",
            actorInitials = "KT",
            body = "commented on your preprint.",
            detail = "\"The elasticity estimate looks off to me -- have you controlled for " +
                "the 2024 revision?\"",
            createdAt = hours(20),
            isRead = true,
        ),
        AppNotification(
            id = "n-5",
            type = NotificationType.MESSAGE,
            actorId = "u-lindqvist",
            actorName = "Prof. Erik Lindqvist",
            actorInitials = "EL",
            body = "sent you a message.",
            detail = "Thanks, that archive reference was perfect.",
            createdAt = days(1),
            isRead = true,
        ),
        AppNotification(
            id = "n-6",
            type = NotificationType.CITATION,
            actorId = "u-mbeki",
            actorName = "Dr. Nomsa Mbeki",
            actorInitials = "NM",
            body = "cited your work in a review article.",
            detail = "Population genomics of arid-zone flora",
            createdAt = days(4),
            isRead = true,
        ),
    )
}
