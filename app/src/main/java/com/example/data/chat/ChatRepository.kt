package com.example.data.chat

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.AuthorIdentity
import com.example.data.SavedPaper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Single source of truth for peer-to-peer and group academic discussions.
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
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        quotedPaperId: String = "",
        quotedPaperTitle: String = "",
        quotedCitation: String = ""
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

        // Generate intelligent academic peer response for demonstration
        val conversation = conversationDao.findConversation(conversationId)
        if (conversation != null) {
            scope.launch {
                delay(1200)
                simulatePeerReply(conversation, trimmed, quotedPaperTitle)
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
                "I reviewed the tensor calculations and the proof is airtight. Let's submit to arXiv tomorrow!"
            userMessage.contains("cite", ignoreCase = true) || userMessage.contains("paper", ignoreCase = true) ->
                "Great analysis! We should compare this with the latest preprint findings from Princeton."
            userMessage.contains("hi", ignoreCase = true) || userMessage.contains("hello", ignoreCase = true) ->
                "Hello! How is the revision on your preprint coming along?"
            else ->
                "Got your update! Working through the empirical methodology and citations now."
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
        conversationDao.updateLastMessage(conversation.id, replyText, replyTimestamp)
    }

    suspend fun markAsRead(conversationId: String) {
        conversationDao.markAsRead(conversationId)
    }

    suspend fun startOrGetConversationForPaper(paper: SavedPaper): String {
        val existing = conversationDao.findConversation("conv_${paper.id}")
        if (existing != null) return existing.id

        val convId = "conv_${paper.id}"
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

    private suspend fun seedDefaultConversations() {
        val now = System.currentTimeMillis()
        val c1 = ConversationEntity(
            id = "c_elena",
            participantName = "Dr. Elena Rostova",
            participantInitials = "EL",
            participantAffiliation = "Oxford University",
            lastMessage = "Brilliant. I'm submitting the camera-ready version to the preprint server now.",
            lastMessageTimestamp = now - 1000 * 60 * 12,
            unreadCount = 1,
            isOnline = true,
            attachedPaperId = "1",
            attachedPaperTitle = "Semantic Structures in Large Language Models"
        )
        val c2 = ConversationEntity(
            id = "c_marcus",
            participantName = "Marcus Vance",
            participantInitials = "MV",
            participantAffiliation = "Princeton Institute",
            lastMessage = "Did you check the eigenvalues of the perturbed Hamiltonian matrix?",
            lastMessageTimestamp = now - 1000 * 60 * 65,
            unreadCount = 0,
            isOnline = true
        )
        val c3 = ConversationEntity(
            id = "c_sophia",
            participantName = "Prof. Sophia Lin",
            participantInitials = "SL",
            participantAffiliation = "Broad Institute",
            lastMessage = "Cryo-EM reconstructions arrived at 1.8Å resolution! Take a look at Figure 3.",
            lastMessageTimestamp = now - 1000 * 60 * 60 * 5,
            unreadCount = 0,
            isOnline = false
        )
        val c4 = ConversationEntity(
            id = "c_maya",
            participantName = "Dr. Maya Kapoor",
            participantInitials = "MK",
            participantAffiliation = "Stanford AI Lab",
            lastMessage = "Sent the BibTeX reference for the reasoning benchmark paper.",
            lastMessageTimestamp = now - 1000 * 60 * 60 * 24,
            unreadCount = 0,
            isOnline = false
        )

        conversationDao.insertConversations(listOf(c1, c2, c3, c4))

        // Seed initial message exchange for Elena
        val m1 = ChatMessageEntity(
            id = "m1",
            conversationId = "c_elena",
            senderName = "Dr. Elena Rostova",
            senderInitials = "EL",
            text = "Hi! Did you have a chance to look at the revised proof for Section 3?",
            timestamp = now - 1000 * 60 * 25,
            isOutgoing = false
        )
        val m2 = ChatMessageEntity(
            id = "m2",
            conversationId = "c_elena",
            senderName = "You",
            senderInitials = "ME",
            text = "Yes, reviewed it this morning. The tensor decomposition is airtight now!",
            timestamp = now - 1000 * 60 * 18,
            isOutgoing = true
        )
        val m3 = ChatMessageEntity(
            id = "m3",
            conversationId = "c_elena",
            senderName = "Dr. Elena Rostova",
            senderInitials = "EL",
            text = "Brilliant. I'm submitting the camera-ready version to the preprint server now.",
            timestamp = now - 1000 * 60 * 12,
            isOutgoing = false
        )
        messageDao.insertMessages(listOf(m1, m2, m3))
    }
}
