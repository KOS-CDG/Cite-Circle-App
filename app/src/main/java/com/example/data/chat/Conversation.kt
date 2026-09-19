package com.example.data.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A direct conversation or paper collaboration thread with another academic.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val participantName: String,
    val participantInitials: String,
    val participantAffiliation: String,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val attachedPaperId: String = "",
    val attachedPaperTitle: String = ""
)
