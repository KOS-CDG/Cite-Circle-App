package com.example.data.messenger

import com.example.ui.theme.AcademicField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Seeded, in-memory messenger.
 *
 * IMPORTANT: this must be an application-scoped singleton (it is built in MyApplication), not
 * owned by a ViewModel. If it lived in a ViewModel, every message you sent would vanish the
 * moment you navigated away from the thread, which destroys the illusion instantly.
 *
 * Sent messages survive for the app session only. Nothing is persisted and nothing touches
 * Firebase.
 */
class InMemoryMessengerRepository(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val now: () -> Long = System::currentTimeMillis,
) : MessengerRepository {

    private val users = MutableStateFlow(seedUsers())
    private val conversations = MutableStateFlow(seedConversations(now()))
    private val messages = MutableStateFlow(seedMessages(now()))
    private val typing = MutableStateFlow<Set<String>>(emptySet())

    override fun observeConversations(): Flow<List<Conversation>> =
        conversations.map { list -> list.sortedByDescending { it.lastMessageAt } }

    override fun observeMessages(conversationId: String): Flow<List<Message>> =
        messages.map { all ->
            all.filter { it.conversationId == conversationId }.sortedBy { it.sentAt }
        }

    override fun observeTyping(conversationId: String): Flow<Boolean> =
        typing.map { conversationId in it }

    override fun observeTotalUnread(): Flow<Int> =
        conversations.map { list -> list.sumOf { it.unreadCount } }

    override fun user(userId: String): MessengerUser? =
        users.value.firstOrNull { it.id == userId }

    override fun participantsOf(conversationId: String): List<MessengerUser> {
        val conversation = conversations.value.firstOrNull { it.id == conversationId }
            ?: return emptyList()
        return conversation.participantIds.mapNotNull { id -> user(id) }
    }

    override fun allUsers(): List<MessengerUser> =
        users.value.filter { it.id != CURRENT_USER_ID }

    override suspend fun send(conversationId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val id = "m-${now()}-${Random.nextInt(10_000)}"
        val sentAt = now()
        val message = Message(
            id = id,
            conversationId = conversationId,
            senderId = CURRENT_USER_ID,
            text = trimmed,
            sentAt = sentAt,
            status = DeliveryStatus.SENDING,
        )
        messages.update { it + message }
        touchConversation(conversationId, trimmed, sentAt)

        // Fake the delivery lifecycle. Without this the bubbles look inert -- a real messenger
        // always shows movement after you hit send.
        scope.launch {
            delay(400)
            setStatus(id, DeliveryStatus.SENT)
            delay(700)
            setStatus(id, DeliveryStatus.DELIVERED)
            delay(1_500)
            setStatus(id, DeliveryStatus.SEEN)
        }

        scope.launch { autoReply(conversationId) }
    }

    override suspend fun markRead(conversationId: String) {
        conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(unreadCount = 0) else it }
        }
    }

    override suspend fun react(conversationId: String, messageId: String, emoji: String) {
        messages.update { list ->
            list.map { message ->
                if (message.id != messageId) {
                    message
                } else {
                    val existing = message.reactions[CURRENT_USER_ID]
                    val updated = if (existing == emoji) {
                        message.reactions - CURRENT_USER_ID
                    } else {
                        message.reactions + (CURRENT_USER_ID to emoji)
                    }
                    message.copy(reactions = updated)
                }
            }
        }
    }

    override suspend fun startConversation(userIds: List<String>): String {
        val participants = (userIds + CURRENT_USER_ID).distinct()

        conversations.value
            .firstOrNull { it.participantIds.toSet() == participants.toSet() }
            ?.let { return it.id }

        val id = "c-${now()}"
        val conversation = Conversation(
            id = id,
            participantIds = participants,
            lastMessagePreview = "",
            lastMessageAt = now(),
            isGroup = participants.size > 2,
            groupTitle = if (participants.size > 2) "New group" else null,
        )
        conversations.update { it + conversation }
        return id
    }

    // ------------------------------------------------------------------ internals

    private fun setStatus(messageId: String, status: DeliveryStatus) {
        messages.update { list ->
            list.map { if (it.id == messageId) it.copy(status = status) else it }
        }
    }

    private fun touchConversation(conversationId: String, preview: String, at: Long) {
        conversations.update { list ->
            list.map {
                if (it.id == conversationId) {
                    it.copy(lastMessagePreview = preview, lastMessageAt = at)
                } else {
                    it
                }
            }
        }
    }

    private suspend fun autoReply(conversationId: String) {
        val conversation = conversations.value.firstOrNull { it.id == conversationId } ?: return
        val responder = conversation.participantIds.firstOrNull { it != CURRENT_USER_ID } ?: return

        delay(900)
        typing.update { it + conversationId }
        delay(Random.nextLong(1_800, 2_800))
        typing.update { it - conversationId }

        val reply = cannedReplies.random()
        val at = now()
        messages.update {
            it + Message(
                id = "m-${at}-${Random.nextInt(10_000)}",
                conversationId = conversationId,
                senderId = responder,
                text = reply,
                sentAt = at,
                status = DeliveryStatus.SEEN,
            )
        }
        touchConversation(conversationId, reply, at)
    }

    private companion object {
        val cannedReplies = listOf(
            "That matches what we saw in the replication, actually.",
            "Can you send the preprint? I want to look at section 4.",
            "Interesting -- though I'd want a larger sample before claiming that.",
            "Agreed. I'll cite it in the discussion.",
            "Let me check with my co-author and come back to you.",
            "Have you seen Thorne's 2023 paper on this? Very relevant.",
        )
    }
}

// ---------------------------------------------------------------------- seed data

private fun seedUsers(): List<MessengerUser> = listOf(
    MessengerUser(CURRENT_USER_ID, "Dr. Jane Doe", "JD", "Oxford", AcademicField.LINGUISTICS),
    MessengerUser("u-thorne", "Dr. Julian Thorne", "JT", "CERN", AcademicField.PHYSICS, isOnline = true),
    MessengerUser("u-okafor", "Dr. Amara Okafor", "AO", "MIT", AcademicField.BIOLOGY, isOnline = true),
    MessengerUser("u-lindqvist", "Prof. Erik Lindqvist", "EL", "Uppsala", AcademicField.HISTORY),
    MessengerUser("u-navarro", "Dr. Sofia Navarro", "SN", "Barcelona", AcademicField.COGNITIVE, isOnline = true),
    MessengerUser("u-tanaka", "Dr. Kenji Tanaka", "KT", "Kyoto", AcademicField.ECONOMICS),
    MessengerUser("u-mbeki", "Dr. Nomsa Mbeki", "NM", "Cape Town", AcademicField.BIOLOGY),
)

private fun minutesAgo(now: Long, minutes: Long) = now - TimeUnit.MINUTES.toMillis(minutes)
private fun hoursAgo(now: Long, hours: Long) = now - TimeUnit.HOURS.toMillis(hours)
private fun daysAgo(now: Long, days: Long) = now - TimeUnit.DAYS.toMillis(days)

private fun seedConversations(now: Long): List<Conversation> = listOf(
    Conversation(
        id = "c-thorne",
        participantIds = listOf(CURRENT_USER_ID, "u-thorne"),
        lastMessagePreview = "Have you seen Thorne's 2023 paper on this?",
        lastMessageAt = minutesAgo(now, 4),
        unreadCount = 2,
    ),
    Conversation(
        id = "c-okafor",
        participantIds = listOf(CURRENT_USER_ID, "u-okafor"),
        lastMessagePreview = "I'll send the sequencing data tonight.",
        lastMessageAt = minutesAgo(now, 52),
        unreadCount = 1,
    ),
    Conversation(
        id = "c-group",
        participantIds = listOf(CURRENT_USER_ID, "u-navarro", "u-tanaka", "u-mbeki"),
        lastMessagePreview = "Sofia: Let's lock the abstract by Friday.",
        lastMessageAt = hoursAgo(now, 5),
        isGroup = true,
        groupTitle = "NeurIPS submission",
    ),
    Conversation(
        id = "c-lindqvist",
        participantIds = listOf(CURRENT_USER_ID, "u-lindqvist"),
        lastMessagePreview = "Thanks, that archive reference was perfect.",
        lastMessageAt = daysAgo(now, 1),
    ),
    Conversation(
        id = "c-tanaka",
        participantIds = listOf(CURRENT_USER_ID, "u-tanaka"),
        lastMessagePreview = "The elasticity estimate looks off to me.",
        lastMessageAt = daysAgo(now, 3),
    ),
)

private fun seedMessages(now: Long): List<Message> = buildList {
    // Spans three days so the day separators actually have something to separate.
    // Named `msg` rather than `add` so it cannot be confused with MutableList.add.
    fun msg(
        id: String,
        conversationId: String,
        senderId: String,
        text: String,
        at: Long,
        status: DeliveryStatus = DeliveryStatus.SEEN,
    ) = add(Message(id, conversationId, senderId, text, at, status))

    msg("m1", "c-thorne", "u-thorne", "Jane -- your entropy paper came up in our group meeting.", daysAgo(now, 2))
    msg("m2", "c-thorne", CURRENT_USER_ID, "Oh? Good context or bad context?", daysAgo(now, 2) + 60_000)
    msg("m3", "c-thorne", "u-thorne", "Very good. We're citing it in the CERN preprint.", daysAgo(now, 2) + 120_000)
    msg("m4", "c-thorne", CURRENT_USER_ID, "That's great news, thank you.", hoursAgo(now, 3))
    msg("m5", "c-thorne", "u-thorne", "One question about section 4.2 though.", minutesAgo(now, 6))
    msg("m6", "c-thorne", "u-thorne", "Have you seen Thorne's 2023 paper on this?", minutesAgo(now, 4))

    msg("m7", "c-okafor", "u-okafor", "The replication came back clean.", hoursAgo(now, 2))
    msg("m8", "c-okafor", CURRENT_USER_ID, "All three conditions?", hoursAgo(now, 2) + 90_000)
    msg("m9", "c-okafor", "u-okafor", "All three. I'll send the sequencing data tonight.", minutesAgo(now, 52))

    msg("m10", "c-group", "u-navarro", "Draft is in the shared folder.", hoursAgo(now, 7))
    msg("m11", "c-group", "u-tanaka", "Reading now. The methods section is tight.", hoursAgo(now, 6))
    msg("m12", "c-group", CURRENT_USER_ID, "I can take the related work section.", hoursAgo(now, 6) + 120_000)
    msg("m13", "c-group", "u-navarro", "Let's lock the abstract by Friday.", hoursAgo(now, 5))

    msg("m14", "c-lindqvist", CURRENT_USER_ID, "Found the 1904 catalogue entry you mentioned.", daysAgo(now, 1) - 3_600_000)
    msg("m15", "c-lindqvist", "u-lindqvist", "Thanks, that archive reference was perfect.", daysAgo(now, 1))

    msg("m16", "c-tanaka", "u-tanaka", "The elasticity estimate looks off to me.", daysAgo(now, 3))
}
