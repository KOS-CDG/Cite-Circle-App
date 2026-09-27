package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScholarConnectionTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: PaperRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PaperRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `connection reasons list contains all 5 required academic contexts`() {
        assertEquals(5, ConnectionReasons.ALL.size)
        val reasonTitles = ConnectionReasons.ALL.map { it.first }
        assertTrue(reasonTitles.contains(ConnectionReasons.SAME_SCHOOL))
        assertTrue(reasonTitles.contains(ConnectionReasons.RESEARCH_COLLAB))
        assertTrue(reasonTitles.contains(ConnectionReasons.READ_AND_CITED))
        assertTrue(reasonTitles.contains(ConnectionReasons.ACADEMIC_EVENT))
        assertTrue(reasonTitles.contains(ConnectionReasons.SHARED_INTEREST))
    }

    @Test
    fun `seeding default scholars populates database when empty`() = runBlocking {
        // Initially empty
        val initialCount = repository.connectedCount.first()
        assertEquals(0, initialCount)

        // Seed default scholars
        repository.seedDefaultScholarsIfEmpty()

        val pending = repository.pendingInvitations.first()
        val suggested = repository.suggestedScholars.first()

        // 1 pending received invitation (Dr. Elena Rostova)
        assertEquals(1, pending.size)
        assertEquals("Dr. Elena Rostova", pending[0].name)
        assertEquals(ConnectionStatus.PENDING_RECEIVED.name, pending[0].status)
        assertEquals(ConnectionReasons.READ_AND_CITED, pending[0].connectionReason)
        assertFalse(pending[0].personalNote.isBlank())

        // Suggestions populated
        assertTrue(suggested.size >= 5)

        // Idempotent: seeding again doesn't duplicate
        repository.seedDefaultScholarsIfEmpty()
        assertEquals(1, repository.pendingInvitations.first().size)
    }

    @Test
    fun `send connection request updates status to PENDING_SENT with reason and note`() = runBlocking {
        repository.seedDefaultScholarsIfEmpty()
        val suggestions = repository.suggestedScholars.first()
        val target = suggestions.first { it.status == ConnectionStatus.NOT_CONNECTED.name }

        val note = "I loved your latest work on distributed systems. Would love to collaborate!"
        repository.sendConnectionRequest(
            id = target.id,
            reason = ConnectionReasons.RESEARCH_COLLAB,
            personalNote = note
        )

        val updatedSuggestions = repository.suggestedScholars.first()
        val updated = updatedSuggestions.first { it.id == target.id }

        assertEquals(ConnectionStatus.PENDING_SENT.name, updated.status)
        assertEquals(ConnectionReasons.RESEARCH_COLLAB, updated.connectionReason)
        assertEquals(note, updated.personalNote)
    }

    @Test
    fun `send connection request with note only updates status to PENDING_SENT`() = runBlocking {
        repository.seedDefaultScholarsIfEmpty()
        val suggestions = repository.suggestedScholars.first()
        val target = suggestions.first { it.status == ConnectionStatus.NOT_CONNECTED.name }

        val note = "Would love to connect and follow your research!"
        repository.sendConnectionRequest(
            id = target.id,
            personalNote = note
        )

        val updatedSuggestions = repository.suggestedScholars.first()
        val updated = updatedSuggestions.first { it.id == target.id }

        assertEquals(ConnectionStatus.PENDING_SENT.name, updated.status)
        assertEquals(note, updated.personalNote)
    }

    @Test
    fun `accept connection updates status to CONNECTED and increments connected count`() = runBlocking {
        repository.seedDefaultScholarsIfEmpty()

        val pendingList = repository.pendingInvitations.first()
        val invitation = pendingList.first()

        assertEquals(0, repository.connectedCount.first())

        repository.acceptConnection(invitation.id)

        assertEquals(1, repository.connectedCount.first())
        val connected = repository.connectedScholars.first()
        assertEquals(1, connected.size)
        assertEquals(invitation.id, connected[0].id)
        assertEquals(ConnectionStatus.CONNECTED.name, connected[0].status)

        // No longer in pending invitations
        assertEquals(0, repository.pendingInvitations.first().size)
    }

    @Test
    fun `ignore connection resets status to NOT_CONNECTED`() = runBlocking {
        repository.seedDefaultScholarsIfEmpty()

        val pendingList = repository.pendingInvitations.first()
        val invitation = pendingList.first()

        repository.ignoreConnection(invitation.id)

        assertEquals(0, repository.pendingInvitations.first().size)
        assertEquals(0, repository.connectedCount.first())

        val suggestions = repository.suggestedScholars.first()
        val ignoredScholar = suggestions.first { it.id == invitation.id }
        assertEquals(ConnectionStatus.NOT_CONNECTED.name, ignoredScholar.status)
    }

    @Test
    fun `remove connection transitions scholar back to NOT_CONNECTED`() = runBlocking {
        repository.seedDefaultScholarsIfEmpty()
        val invitation = repository.pendingInvitations.first().first()

        // First accept
        repository.acceptConnection(invitation.id)
        assertEquals(1, repository.connectedCount.first())

        // Now remove
        repository.removeConnection(invitation.id)
        assertEquals(0, repository.connectedCount.first())
        assertEquals(0, repository.connectedScholars.first().size)
    }
}
