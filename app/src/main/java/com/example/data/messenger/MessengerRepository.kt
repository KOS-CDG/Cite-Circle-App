package com.example.data.messenger

import kotlinx.coroutines.flow.Flow

/**
 * The messenger is a UI mockup, but it is written against an interface so the screens never learn
 * where messages come from. Swapping InMemoryMessengerRepository for a Firestore-backed
 * implementation later is a wiring change in AppContainer, not a UI rewrite.
 */
interface MessengerRepository {
    fun observeConversations(): Flow<List<Conversation>>
    fun observeMessages(conversationId: String): Flow<List<Message>>
    fun observeTyping(conversationId: String): Flow<Boolean>
    fun observeTotalUnread(): Flow<Int>

    fun user(userId: String): MessengerUser?
    fun participantsOf(conversationId: String): List<MessengerUser>
    fun allUsers(): List<MessengerUser>

    suspend fun send(conversationId: String, text: String)
    suspend fun markRead(conversationId: String)
    suspend fun react(conversationId: String, messageId: String, emoji: String)
    suspend fun startConversation(userIds: List<String>): String
}
