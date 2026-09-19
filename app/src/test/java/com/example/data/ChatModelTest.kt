package com.example.data

import com.example.data.chat.ChatMessageEntity
import com.example.data.chat.ConversationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatModelTest {

    @Test
    fun `conversation entity holds metadata and paper link properly`() {
        val conv = ConversationEntity(
            id = "c1",
            participantName = "Dr. Elena Rostova",
            participantInitials = "EL",
            participantAffiliation = "Oxford University",
            lastMessage = "Camera-ready preprint submitted.",
            lastMessageTimestamp = 1000L,
            unreadCount = 2,
            isOnline = true,
            attachedPaperId = "p101",
            attachedPaperTitle = "Quantum Corrections in Kerr Horizon"
        )

        assertEquals("c1", conv.id)
        assertEquals("Dr. Elena Rostova", conv.participantName)
        assertEquals("EL", conv.participantInitials)
        assertEquals(2, conv.unreadCount)
        assertTrue(conv.isOnline)
        assertEquals("p101", conv.attachedPaperId)
        assertEquals("Quantum Corrections in Kerr Horizon", conv.attachedPaperTitle)
    }

    @Test
    fun `chat message entity supports citable paper quotes`() {
        val msg = ChatMessageEntity(
            id = "m1",
            conversationId = "c1",
            senderName = "Jane Doe",
            senderInitials = "JD",
            text = "Here is the key reference for our methodology.",
            timestamp = 2000L,
            isOutgoing = true,
            quotedPaperId = "p202",
            quotedPaperTitle = "Spectral Anomalies",
            quotedCitation = "Doe, J. (2026). Spectral Anomalies. Nature."
        )

        assertEquals("m1", msg.id)
        assertTrue(msg.isOutgoing)
        assertEquals("p202", msg.quotedPaperId)
        assertEquals("Spectral Anomalies", msg.quotedPaperTitle)
        assertEquals("Doe, J. (2026). Spectral Anomalies. Nature.", msg.quotedCitation)
    }

    @Test
    fun `unread count zero indicates read state`() {
        val conv = ConversationEntity(
            id = "c2",
            participantName = "Marcus Vance",
            participantInitials = "MV",
            participantAffiliation = "Princeton Institute",
            unreadCount = 0
        )
        assertFalse(conv.unreadCount > 0)
    }
}
