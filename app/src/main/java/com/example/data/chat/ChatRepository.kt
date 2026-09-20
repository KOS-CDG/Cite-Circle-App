package com.example.data.chat

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.AuthorIdentity
import com.example.data.SavedPaper
import com.example.network.SupabaseClient
import com.example.network.SupabaseConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Single source of truth for peer-to-peer and group academic discussions.
 * Synchronized with Supabase Academic Lounge chat.
 */
class ChatRepository(
    private val database: AppDatabase,
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.chatMessageDao()

    val conversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val totalUnreadCount: Flow<Int?> = conversationDao.getTotalUnreadCount()

    fun messagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>> =
        messageDao.getMessagesForConversation(conversationId)

    fun conversation(id: String): Flow<ConversationEntity?> =
        conversationDao.getConversation(id)

    init {
        scope.launch {
            conversationDao.getAllConversations().take(1).collect { list ->
                if (list.isEmpty()) {
                    seedDefaultConversations()
                }
            }
            syncLoungeMessages()
        }
    }

    suspend fun syncLoungeMessages(currentUserId: String = "") {
        scope.launch {
            val result = SupabaseClient.getLoungeMessages(currentUserId)
            if (result.isSuccess) {
                val cloudMessages = result.getOrThrow()
                if (cloudMessages.isNotEmpty()) {
                    messageDao.insertMessages(cloudMessages)
                    val last = cloudMessages.last()
                    conversationDao.updateLastMessage(
                        id = SupabaseConfig.ACADEMIC_LOUNGE_CONVERSATION_ID,
                        message = "${last.senderName}: ${last.text}",
                        timestamp = last.timestamp
                    )
                }
            }
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        quotedPaperId: String = "",
        quotedPaperTitle: String = "",
        quotedCitation: String = "",
        userId: String = "",
        accessToken: String = ""
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && quotedPaperId.isEmpty()) return

        val identity = AuthorIdentity.current(context)
        val now = System.currentTimeMillis()

        val message = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderName = identity.name,
            senderInitials = identity.initials,
            text = trimmed,
            timestamp = now,
            isOutgoing = true,
            quotedPaperId = quotedPaperId,
            quotedPaperTitle = quotedPaperTitle,
            quotedCitation = quotedCitation
        )

        messageDao.insertMessage(message)
        conversationDao.updateLastMessage(
            id = conversationId,
            message = if (trimmed.isNotEmpty()) trimmed else "Shared a citation: $quotedPaperTitle",
            timestamp = now
        )

        // If chatting in the Academic Lounge, sync to Supabase Cloud
        if (conversationId == SupabaseConfig.ACADEMIC_LOUNGE_CONVERSATION_ID && userId.isNotBlank()) {
            scope.launch {
                val token = accessToken.ifBlank { SupabaseConfig.ANON_KEY }
                SupabaseClient.sendLoungeMessage(userId, trimmed, token)
            }
        } else if (conversationId != SupabaseConfig.ACADEMIC_LOUNGE_CONVERSATION_ID) {
            // Local peer response for personal notes
            val conversation = conversationDao.findConversation(conversationId)
            if (conversation != null) {
                scope.launch {
                    delay(1200)
                    simulatePeerReply(conversation, trimmed, quotedPaperTitle)
                }
            }
        }
    }

    private suspend fun simulatePeerReply(
        conversation: ConversationEntity,
        userMessage: String,
        paperTitle: String
    ) {
        val replyText = when {
            paperTitle.isNotBlank() ->
                "Thanks for sharing “$paperTitle”! I will add this to our reading circle right away."
            userMessage.contains("proof", ignoreCase = true) || userMessage.contains("section", ignoreCase = true) ->
                "I reviewed the calculations and the proof is airtight. Looking forward to the preprint!"
            userMessage.contains("cite", ignoreCase = true) || userMessage.contains("paper", ignoreCase = true) ->
                "Great analysis! We should compare this with the latest preprint findings."
            userMessage.contains("hi", ignoreCase = true) || userMessage.contains("hello", ignoreCase = true) ->
                "Hello! How is the revision on your research coming along?"
            else ->
                "Got your update! Reviewing the empirical methodology and citations now."
        }

        val replyTimestamp = System.currentTimeMillis()
        val reply = ChatMessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversation.id,
            senderName = conversation.participantName,
            senderInitials = conversation.participantInitials,
            text = replyText,
            timestamp = replyTimestamp,
            isOutgoing = false
        )

        messageDao.insertMessage(reply)
        conversationDao.updateLastMessage(
            id = conversation.id,
            message = replyText,
            timestamp = replyTimestamp
        )
    }

    suspend fun markAsRead(conversationId: String) {
        conversationDao.markAsRead(conversationId)
    }

    suspend fun startOrGetConversationForPaper(paper: SavedPaper): String {
        val convId = "conv_${paper.id}"
        val existing = conversationDao.findConversation(convId)
        if (existing != null) return existing.id

        val newConv = ConversationEntity(
            id = convId,
            participantName = paper.authorName,
            participantInitials = paper.authorInitials,
            participantAffiliation = paper.affiliation,
            lastMessage = "Started a reading circle on: ${paper.title.ifBlank { paper.content.take(30) }}",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            isOnline = true,
            attachedPaperId = paper.id,
            attachedPaperTitle = paper.title
        )
        conversationDao.insertConversation(newConv)
        return convId
    }

    suspend fun startOrGetConversationWithAuthor(
        name: String,
        initials: String,
        affiliation: String
    ): String {
        val convId = "conv_" + name.filter { it.isLetterOrDigit() }.lowercase()
        val existing = conversationDao.findConversation(convId)
        if (existing != null) return existing.id

        val newConv = ConversationEntity(
            id = convId,
            participantName = name,
            participantInitials = initials,
            participantAffiliation = affiliation,
            lastMessage = "Started academic discussion.",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            isOnline = true
        )
        conversationDao.insertConversation(newConv)
        return convId
    }

    suspend fun createOrGetConversation(
        name: String,
        initials: String,
        affiliation: String
    ): String = startOrGetConversationWithAuthor(name, initials, affiliation)

    private suspend fun seedDefaultConversations() {
        val now = System.currentTimeMillis()
        val lounge = ConversationEntity(
            id = SupabaseConfig.ACADEMIC_LOUNGE_CONVERSATION_ID,
            participantName = "Academic Lounge",
            participantInitials = "AL",
            participantAffiliation = "Cite Circle Global Forum",
            lastMessage = "Welcome to the Academic Lounge. Connect and discuss preprint discoveries with researchers worldwide.",
            lastMessageTimestamp = now,
            unreadCount = 0,
            isOnline = true
        )
        conversationDao.insertConversation(lounge)
    }
}
