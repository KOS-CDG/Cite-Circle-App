package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PdfReadingProgressAndNotesTest {

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
    fun `reading progress persists and updates correctly in database`() = runBlocking {
        val paper = SavedPaper(
            id = "paper-progress-1",
            authorInitials = "EN",
            authorName = "Emmy Noether",
            affiliation = "Gottingen",
            content = "Invariante Variationsprobleme",
            title = "Noether Theorem and Conservation Laws",
            lastReadPage = 1,
            totalPageCount = 0,
            pageBookmarks = ""
        )
        repository.savePaper(paper)

        var retrieved = repository.findPaper("paper-progress-1")
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.lastReadPage)
        assertEquals(0, retrieved?.totalPageCount)
        assertEquals("", retrieved?.pageBookmarks)

        // Update reading progress to page 14 of 42
        repository.updateReadingProgress("paper-progress-1", 14, 42)
        retrieved = repository.findPaper("paper-progress-1")
        assertEquals(14, retrieved?.lastReadPage)
        assertEquals(42, retrieved?.totalPageCount)
    }

    @Test
    fun `page bookmarks persist and can be toggled correctly`() = runBlocking {
        val paper = SavedPaper(
            id = "paper-bookmarks-1",
            authorInitials = "MC",
            authorName = "Marie Curie",
            affiliation = "Sorbonne",
            content = "Researches on Radioactive Substances",
            title = "Radioactive Elements and Radiation",
            lastReadPage = 1,
            totalPageCount = 30,
            pageBookmarks = ""
        )
        repository.savePaper(paper)

        // Add bookmark on page 5
        val bookmarksList = mutableSetOf<Int>()
        bookmarksList.add(5)
        repository.updatePageBookmarks("paper-bookmarks-1", bookmarksList.sorted().joinToString(","))

        var retrieved = repository.findPaper("paper-bookmarks-1")
        assertEquals("5", retrieved?.pageBookmarks)

        // Add bookmark on page 12 and page 2
        bookmarksList.add(12)
        bookmarksList.add(2)
        repository.updatePageBookmarks("paper-bookmarks-1", bookmarksList.sorted().joinToString(","))

        retrieved = repository.findPaper("paper-bookmarks-1")
        assertEquals("2,5,12", retrieved?.pageBookmarks)

        // Remove bookmark on page 5
        bookmarksList.remove(5)
        repository.updatePageBookmarks("paper-bookmarks-1", bookmarksList.sorted().joinToString(","))

        retrieved = repository.findPaper("paper-bookmarks-1")
        assertEquals("2,12", retrieved?.pageBookmarks)
    }

    @Test
    fun `excerpt highlighter appends structured quote and notes properly`() = runBlocking {
        val paper = SavedPaper(
            id = "paper-excerpt-1",
            authorInitials = "AT",
            authorName = "Alan Turing",
            affiliation = "Manchester",
            content = "Computing Machinery and Intelligence",
            title = "Can Machines Think?",
            readingStatus = "TO_READ",
            researchNotes = "Initial overview notes."
        )
        repository.savePaper(paper)

        // Append excerpt 1: Key Finding on Page 4
        val tag1 = "Key Finding"
        val pageNum1 = 4
        val quote1 = "The imitation game is played with three people, a man (A), a woman (B), and an interrogator (C)."
        val comment1 = "Core setup of the Turing test."

        val entry1 = "📌 [$tag1] Page $pageNum1:\n\"$quote1\"\n💭 Note: $comment1"
        val existing1 = repository.findPaper("paper-excerpt-1")?.researchNotes.orEmpty()
        val updatedNotes1 = if (existing1.isBlank()) entry1 else "$existing1\n\n---\n$entry1"
        repository.updateResearchNotes("paper-excerpt-1", updatedNotes1)

        var retrieved = repository.findPaper("paper-excerpt-1")
        assertNotNull(retrieved)
        assertTrue(retrieved!!.researchNotes.contains("Initial overview notes."))
        assertTrue(retrieved.researchNotes.contains("📌 [Key Finding] Page 4:"))
        assertTrue(retrieved.researchNotes.contains(quote1))
        assertTrue(retrieved.researchNotes.contains(comment1))

        // Append excerpt 2: Methodology on Page 9
        val tag2 = "Methodology"
        val pageNum2 = 9
        val quote2 = "Digital computers can be constructed to carry out any computation that could be carried out by a human computer."
        val entry2 = "📌 [$tag2] Page $pageNum2:\n\"$quote2\""
        val existing2 = retrieved.researchNotes
        val updatedNotes2 = "$existing2\n\n---\n$entry2"
        repository.updateResearchNotes("paper-excerpt-1", updatedNotes2)

        retrieved = repository.findPaper("paper-excerpt-1")
        assertTrue(retrieved!!.researchNotes.contains("📌 [Methodology] Page 9:"))
        assertTrue(retrieved.researchNotes.contains(quote2))
    }

    @Test
    fun `reading status auto-advances when user progresses in document`() = runBlocking {
        val paper = SavedPaper(
            id = "paper-auto-advance",
            authorInitials = "RP",
            authorName = "Roger Penrose",
            affiliation = "Oxford",
            content = "Gravitational Collapse and Space-Time Singularities",
            title = "Singularity Theorems in General Relativity",
            readingStatus = "TO_READ",
            lastReadPage = 1,
            totalPageCount = 18
        )
        repository.savePaper(paper)

        var retrieved = repository.findPaper("paper-auto-advance")
        assertEquals("TO_READ", retrieved?.readingStatus)

        // Advance reading progress to page 2
        val newPage = 2
        repository.updateReadingProgress("paper-auto-advance", newPage, 18)
        if (retrieved?.readingStatus == "TO_READ" && newPage > 1) {
            repository.updateReadingStatus("paper-auto-advance", "READING")
        }

        retrieved = repository.findPaper("paper-auto-advance")
        assertEquals(2, retrieved?.lastReadPage)
        assertEquals("READING", retrieved?.readingStatus)
    }
}
