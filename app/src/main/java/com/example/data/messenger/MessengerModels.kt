package com.example.data.messenger

import com.example.ui.theme.AcademicField

/**
 * Messenger domain models.
 *
 * Deliberately plain Kotlin, not Room entities: this is a UI mockup backed by an in-memory
 * repository, so there is no schema to migrate and nothing to persist. See
 * InMemoryMessengerRepository.
 */
data class MessengerUser(
    val id: String,
    val name: String,
    val initials: String,
    val affiliation: String,
    val field: AcademicField,
    val isOnline: Boolean = false,
    val lastActiveAt: Long = 0L,
)

enum class DeliveryStatus { SENDING, SENT, DELIVERED, SEEN }

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val sentAt: Long,
    val status: DeliveryStatus = DeliveryStatus.SEEN,
    /** Emoji keyed by the user id that applied it. */
    val reactions: Map<String, String> = emptyMap(),
)

data class Conversation(
    val id: String,
    val participantIds: List<String>,
    val lastMessagePreview: String,
    val lastMessageAt: Long,
    val unreadCount: Int = 0,
    val isTyping: Boolean = false,
    val isGroup: Boolean = false,
    val groupTitle: String? = null,
)

/** The signed-in user. Everything else in the mock is somebody else. */
const val CURRENT_USER_ID = "u-me"
