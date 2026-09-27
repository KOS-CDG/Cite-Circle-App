package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ui.lists.DocumentTypeFilter
import com.example.ui.lists.ReadingStatusFilter
import com.example.ui.lists.RepositoryFilterCriteria
import com.example.ui.lists.RepositorySortOrder
import kotlinx.coroutines.flow.first
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
class RepositoryFilteringAndNotesTest {

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
    fun `reading status and research notes persist and update correctly`() = runBlocking {
        val paper = SavedPaper(
            id = "paper-test-1",
            authorInitials = "NB",
            authorName = "Niels Bohr",
            affiliation = "Copenhagen",
            content = "On the Constitution of Atoms and Molecules",
            title = "Atomic Structure and Spectra",
            publishedAt = 1000L,
            readingStatus = "TO_READ",
            researchNotes = ""
        )
        repository.savePaper(paper)

        var retrieved = repository.findPaper("paper-test-1")
        assertNotNull(retrieved)
        assertEquals("TO_READ", retrieved?.readingStatus)
        assertEquals("", retrieved?.researchNotes)

        // Update reading status to READING
        repository.updateReadingStatus("paper-test-1", "READING")
        retrieved = repository.findPaper("paper-test-1")
        assertEquals("READING", retrieved?.readingStatus)

        // Update research notes
        val notes = "Key idea: electrons orbit in quantized energy states without radiating energy."
        repository.updateResearchNotes("paper-test-1", notes)
        retrieved = repository.findPaper("paper-test-1")
        assertEquals(notes, retrieved?.researchNotes)

        // Update reading status to COMPLETED
        repository.updateReadingStatus("paper-test-1", "COMPLETED")
        retrieved = repository.findPaper("paper-test-1")
        assertEquals("COMPLETED", retrieved?.readingStatus)
    }

    @Test
    fun `batch BibTeX citation export formats all papers into valid bibliography`() {
        val paper1 = SavedPaper(
            id = "p1",
            authorInitials = "AE",
            authorName = "Albert Einstein",
            authors = "Einstein, Albert",
            affiliation = "Bern",
            content = "On the electrodynamics of moving bodies",
            title = "Special Relativity",
            year = "1905",
            venue = "Annalen der Physik",
            doi = "10.1002/andp.19053221004"
        )
        val paper2 = SavedPaper(
            id = "p2",
            authorInitials = "MP",
            authorName = "Max Planck",
            authors = "Planck, Max",
            affiliation = "Berlin",
            content = "Law of Energy Distribution",
            title = "Quantum Theory",
            year = "1900",
            venue = "Annalen der Physik",
            doi = "10.1002/andp.19013090310"
        )

        val batchBib = CitationFormatter.exportBatch(listOf(paper1, paper2), ExportFormat.BIBTEX)
        assertTrue(batchBib.contains("@article{"))
        assertTrue(batchBib.contains("Special Relativity"))
        assertTrue(batchBib.contains("Quantum Theory"))
        assertTrue(batchBib.contains("Einstein, Albert"))
        assertTrue(batchBib.contains("Planck, Max"))
        assertTrue(batchBib.contains("1905"))
        assertTrue(batchBib.contains("1900"))
    }

    @Test
    fun `filter criteria active count calculates correctly`() {
        val defaultCriteria = RepositoryFilterCriteria()
        assertEquals(0, defaultCriteria.activeFilterCount)

        val withStatus = defaultCriteria.copy(readingStatus = ReadingStatusFilter.READING)
        assertEquals(1, withStatus.activeFilterCount)

        val withStatusAndDoc = withStatus.copy(documentType = DocumentTypeFilter.PDF_ONLY)
        assertEquals(2, withStatusAndDoc.activeFilterCount)

        val fullFilter = withStatusAndDoc.copy(sortOrder = RepositorySortOrder.TITLE_AZ)
        assertEquals(3, fullFilter.activeFilterCount)
    }
}
