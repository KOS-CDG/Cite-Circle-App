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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryCloudSyncTest {

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
    fun `test batch queries and inserts on collections and entries`() = runBlocking {
        val col1 = CollectionEntity(
            id = "col-1",
            name = "Deep Learning",
            description = "Neural network architectures",
            colorHex = "#1A73E8",
            iconName = "folder"
        )
        val col2 = CollectionEntity(
            id = "col-2",
            name = "Quantum Computing",
            description = "Qubits and entanglement",
            colorHex = "#34A853",
            iconName = "science"
        )

        // Batch insert
        database.collectionDao().insertCollections(listOf(col1, col2))

        val allCols = repository.getAllCollectionsOnce()
        assertEquals(2, allCols.size)
        assertTrue(allCols.any { it.name == "Deep Learning" })
        assertTrue(allCols.any { it.name == "Quantum Computing" })

        // Batch entries
        val entry1 = PaperCollectionEntry(paperId = "p-1", collectionId = "col-1")
        val entry2 = PaperCollectionEntry(paperId = "p-2", collectionId = "col-1")
        val entry3 = PaperCollectionEntry(paperId = "p-3", collectionId = "col-2")

        database.collectionDao().insertEntries(listOf(entry1, entry2, entry3))

        val allEntries = repository.getAllEntriesOnce()
        assertEquals(3, allEntries.size)
        assertEquals(2, allEntries.count { it.collectionId == "col-1" })
    }

    @Test
    fun `test syncMergeLibrary preserves local file paths and merges metadata`() = runBlocking {
        val paperDao = database.savedPaperDao()

        // 1. Pre-existing local paper with downloaded PDF and local notes
        val localPaper = SavedPaper(
            id = "10.1038/s41586-020-2649-2",
            authorInitials = "JB",
            authorName = "John Doe",
            affiliation = "Stanford",
            content = "Exciting discovery",
            title = "AlphaFold 2 Protein Structure Prediction",
            authors = "Jumper, John; Hassabis, Demis",
            year = "2021",
            venue = "Nature",
            doi = "10.1038/s41586-020-2649-2",
            url = "https://nature.com/articles/s41586-020-2649-2",
            pdfUrl = "https://nature.com/articles/s41586-020-2649-2.pdf",
            pdfLocalPath = "/data/user/0/com.example/files/vault/alphafold.pdf", // LOCAL PATH
            imageUri = "/data/user/0/com.example/files/images/thumb.jpg",       // LOCAL IMAGE
            readingStatus = "READING",
            researchNotes = "Local notes: Critical for biology",
            isBookmarked = true
        )
        paperDao.insertPaper(localPaper)

        // 2. Incoming remote paper from cloud without local paths
        val remotePaper = SavedPaper(
            id = "10.1038/s41586-020-2649-2",
            authorInitials = "JB",
            authorName = "John Doe",
            affiliation = "Stanford",
            content = "Exciting discovery updated",
            title = "AlphaFold 2 Protein Structure Prediction",
            authors = "Jumper, John; Hassabis, Demis",
            year = "2021",
            venue = "Nature",
            doi = "10.1038/s41586-020-2649-2",
            url = "https://nature.com/articles/s41586-020-2649-2",
            pdfUrl = "https://nature.com/articles/s41586-020-2649-2.pdf",
            pdfLocalPath = "", // EMPTY IN CLOUD
            imageUri = "",     // EMPTY IN CLOUD
            readingStatus = "READ", // UPDATED ON OTHER DEVICE
            researchNotes = "Remote notes: Validated experimental data",
            isBookmarked = true
        )

        val newRemotePaper = SavedPaper(
            id = "arxiv:2301.07041",
            authorInitials = "AK",
            authorName = "Alice",
            affiliation = "MIT",
            content = "RLHF alignment",
            title = "Direct Preference Optimization",
            authors = "Rafailov, Rafael",
            year = "2023",
            readingStatus = "TO_READ",
            researchNotes = ""
        )

        val remoteCollection = CollectionEntity(
            id = "col-cloud-1",
            name = "Structural Biology",
            description = "Proteins and RNA"
        )
        val remoteEntry = PaperCollectionEntry(
            paperId = "10.1038/s41586-020-2649-2",
            collectionId = "col-cloud-1"
        )

        // 3. Execute syncMergeLibrary
        repository.syncMergeLibrary(
            remotePapers = listOf(remotePaper, newRemotePaper),
            remoteCollections = listOf(remoteCollection),
            remoteEntries = listOf(remoteEntry)
        )

        // 4. Verify local paths were PRESERVED and new papers added
        val mergedLocal = repository.findPaper("10.1038/s41586-020-2649-2")
        assertNotNull(mergedLocal)
        assertEquals("/data/user/0/com.example/files/vault/alphafold.pdf", mergedLocal!!.pdfLocalPath)
        assertEquals("/data/user/0/com.example/files/images/thumb.jpg", mergedLocal.imageUri)
        assertEquals("READ", mergedLocal.readingStatus)
        assertEquals("Remote notes: Validated experimental data", mergedLocal.researchNotes)

        val retrievedNewPaper = repository.findPaper("arxiv:2301.07041")
        assertNotNull(retrievedNewPaper)
        assertEquals("Direct Preference Optimization", retrievedNewPaper!!.title)

        // Verify collection and entry were synced
        val syncedCols = repository.getAllCollectionsOnce()
        assertEquals(1, syncedCols.size)
        assertEquals("Structural Biology", syncedCols[0].name)

        val syncedEntries = repository.getAllEntriesOnce()
        assertEquals(1, syncedEntries.size)
        assertEquals("10.1038/s41586-020-2649-2", syncedEntries[0].paperId)
    }
}
