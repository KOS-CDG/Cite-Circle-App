package com.example.data.chat

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An individual message within a conversation. Supports quoting and linking a research paper
 * directly into the discussion thread.
 */
@Entity(tableName = "chat_messages", indices = [Index("conversationId")])
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderName: String,
    val senderInitials: String,
    val text: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val quotedPaperId: String = "",
    val quotedPaperTitle: String = "",
    val quotedCitation: String = ""
)
