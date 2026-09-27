package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CollectionDaoTest {

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
    fun `insert, update, and count collections with papers`() = runBlocking {
        val collectionDao = database.collectionDao()
        val paperDao = database.savedPaperDao()

        // 1. Insert Collection
        val colId1 = UUID.randomUUID().toString()
        val col1 = CollectionEntity(
            id = colId1,
            name = "Large Language Models",
            description = "Reasoning, alignment, and agents",
            colorHex = "#1A73E8",
            iconName = "science",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        collectionDao.insertCollection(col1)

        val retrievedCol = collectionDao.getCollectionById(colId1)
        assertNotNull(retrievedCol)
        assertEquals("Large Language Models", retrievedCol?.name)
        assertEquals("science", retrievedCol?.iconName)

        // 2. Insert Papers
        val paper1 = SavedPaper(
            id = "p1",
            authorInitials = "AT",
            authorName = "Alan Turing",
            affiliation = "Cambridge",
            content = "Computing Machinery and Intelligence",
            title = "Can Machines Think?",
            publishedAt = 1000L
        )
        val paper2 = SavedPaper(
            id = "p2",
            authorInitials = "HN",
            authorName = "John von Neumann",
            affiliation = "IAS Princeton",
            content = "First Draft of a Report on the EDVAC",
            title = "EDVAC Architecture",
            publishedAt = 2000L
        )
        paperDao.insertPaper(paper1)
        paperDao.insertPaper(paper2)

        // 3. Assign paper1 and paper2 to collection
        collectionDao.addPaperToCollection(PaperCollectionEntry("p1", colId1, 1000L))
        collectionDao.addPaperToCollection(PaperCollectionEntry("p2", colId1, 2000L))

        // Verify count
        val collectionsWithCount = collectionDao.getCollectionsWithCount().first()
        assertEquals(1, collectionsWithCount.size)
        assertEquals(colId1, collectionsWithCount[0].id)
        assertEquals(2, collectionsWithCount[0].paperCount)

        // Verify papers in collection
        val papersInCol = collectionDao.getPapersInCollection(colId1).first()
        assertEquals(2, papersInCol.size)

        // 4. Remove one paper from collection
        collectionDao.removePaperFromCollection("p1", colId1)
        val updatedWithCount = collectionDao.getCollectionsWithCount().first()
        assertEquals(1, updatedWithCount[0].paperCount)
        val remainingPapers = collectionDao.getPapersInCollection(colId1).first()
        assertEquals(1, remainingPapers.size)
        assertEquals("p2", remainingPapers[0].id)
    }

    @Test
    fun `setPaperCollections batch synchronizes paper folders in repository`() = runBlocking {
        val colA = CollectionEntity(id = "colA", name = "Folder A")
        val colB = CollectionEntity(id = "colB", name = "Folder B")
        val colC = CollectionEntity(id = "colC", name = "Folder C")
        repository.saveCollection(colA)
        repository.saveCollection(colB)
        repository.saveCollection(colC)

        val paper = SavedPaper(
            id = "paper-alpha",
            authorInitials = "CE",
            authorName = "Claude Shannon",
            affiliation = "Bell Labs",
            content = "A Mathematical Theory of Communication",
            title = "Information Theory Foundations",
            publishedAt = 5000L
        )
        repository.savePaper(paper)

        // Assign to colA and colB
        repository.setPaperCollections("paper-alpha", setOf("colA", "colB"))

        val assigned1 = repository.collectionIdsForPaper("paper-alpha").first().toSet()
        assertEquals(setOf("colA", "colB"), assigned1)

        // Reassign to colB and colC (removes colA, adds colC)
        repository.setPaperCollections("paper-alpha", setOf("colB", "colC"))

        val assigned2 = repository.collectionIdsForPaper("paper-alpha").first().toSet()
        assertEquals(setOf("colB", "colC"), assigned2)

        // Verify collectionsForPaper returns entity objects
        val entityList = repository.collectionsForPaper("paper-alpha").first()
        assertEquals(2, entityList.size)
        val names = entityList.map { it.name }.toSet()
        assertTrue(names.contains("Folder B"))
        assertTrue(names.contains("Folder C"))
    }

    @Test
    fun `deleteCollection cascades entries and deletePaper cleans up collection entries`() = runBlocking {
        val col = CollectionEntity(id = "col-to-delete", name = "Temporary Project")
        repository.saveCollection(col)

        val paper = SavedPaper(
            id = "paper-xyz",
            authorInitials = "GA",
            authorName = "Grace Hopper",
            affiliation = "US Navy",
            content = "Compiler Design",
            title = "A-0 Compiler System",
            publishedAt = 3000L
        )
        repository.savePaper(paper)
        repository.addPaperToCollection("paper-xyz", "col-to-delete")

        assertEquals(1, database.collectionDao().countPapersInCollection("col-to-delete"))

        // Deleting collection must clear entries
        repository.deleteCollection("col-to-delete")
        assertNull(database.collectionDao().getCollectionById("col-to-delete"))
        assertEquals(0, database.collectionDao().countPapersInCollection("col-to-delete"))

        // Re-create and test deleting paper
        val col2 = CollectionEntity(id = "col-permanent", name = "Permanent Collection")
        repository.saveCollection(col2)
        repository.addPaperToCollection("paper-xyz", "col-permanent")
        assertEquals(1, database.collectionDao().countPapersInCollection("col-permanent"))

        // Delete paper
        repository.deletePaper("paper-xyz")
        assertNull(repository.findPaper("paper-xyz"))
        assertEquals(0, database.collectionDao().countPapersInCollection("col-permanent"))
    }
}
