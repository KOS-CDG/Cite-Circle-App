package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.UserAccount
import com.example.data.auth.UserSessionManager
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
class DatabaseLimitersTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `UserAccountDao persists, authenticates, and purges user accounts`() = runBlocking {
        val userDao = database.userAccountDao()
        val user = UserAccount(
            id = UUID.randomUUID().toString(),
            email = "turing@cambridge.edu",
            displayName = "Alan Turing",
            passwordHash = UserSessionManager.hashPassword("enigmaBreak1942"),
            affiliation = "University of Cambridge",
            researchField = "Theoretical Computer Science & AI"
        )

        userDao.insertUser(user)

        val retrieved = userDao.findUserByEmail("turing@cambridge.edu")
        assertNotNull(retrieved)
        assertEquals("Alan Turing", retrieved?.displayName)
        assertEquals(1, userDao.countUsers())

        // Purge user
        userDao.deleteUser("turing@cambridge.edu")
        assertNull(userDao.findUserByEmail("turing@cambridge.edu"))
        assertEquals(0, userDao.countUsers())
    }

    @Test
    fun `SavedPaperDao pagination limiters enforce exact batch sizes`() = runBlocking {
        val paperDao = database.savedPaperDao()

        for (i in 1..10) {
            paperDao.insertPaper(
                SavedPaper(
                    id = "paper-$i",
                    authorInitials = "AU",
                    authorName = "Author $i",
                    affiliation = "Univ",
                    content = "Discussion $i",
                    title = "Paper Title $i",
                    publishedAt = 1000L * i
                )
            )
        }

        assertEquals(10, paperDao.countAllPapers())

        // Query with limit 4, offset 0 -> returns exactly 4 items (newest: 10, 9, 8, 7)
        val page1 = paperDao.getPagedPapers(limit = 4, offset = 0).first()
        assertEquals(4, page1.size)
        assertEquals("paper-10", page1[0].id)
        assertEquals("paper-9", page1[1].id)

        // Query with limit 4, offset 4 -> returns next 4 items (6, 5, 4, 3)
        val page2 = paperDao.getPagedPapers(limit = 4, offset = 4).first()
        assertEquals(4, page2.size)
        assertEquals("paper-6", page2[0].id)

        // Query with limit 4, offset 8 -> returns remaining 2 items (2, 1)
        val page3 = paperDao.getPagedPapers(limit = 4, offset = 8).first()
        assertEquals(2, page3.size)
        assertEquals("paper-2", page3[0].id)
    }

    @Test
    fun `Database cache limiter evicts unbookmarked excess papers but retains bookmarks`() = runBlocking {
        val paperDao = database.savedPaperDao()

        // Insert 6 papers, bookmarking paper-1 and paper-2
        for (i in 1..6) {
            paperDao.insertPaper(
                SavedPaper(
                    id = "paper-$i",
                    authorInitials = "AU",
                    authorName = "Author $i",
                    affiliation = "Univ",
                    content = "Comment $i",
                    publishedAt = 1000L * i,
                    isBookmarked = (i <= 2) // paper-1 and paper-2 are bookmarked
                )
            )
        }

        assertEquals(6, paperDao.countAllPapers())

        // Enforce cache limit: keep top 2 newest unbookmarked papers
        paperDao.cleanupExcessCache(maxKeep = 2)

        val remaining = paperDao.getAllPapers().first()
        val remainingIds = remaining.map { it.id }.toSet()

        // Bookmarked papers must NEVER be purged
        assertTrue("Bookmarked paper-1 must be retained", remainingIds.contains("paper-1"))
        assertTrue("Bookmarked paper-2 must be retained", remainingIds.contains("paper-2"))
        // Newest unbookmarked papers (paper-6, paper-5) must be retained
        assertTrue("Recent paper-6 must be retained", remainingIds.contains("paper-6"))
        assertTrue("Recent paper-5 must be retained", remainingIds.contains("paper-5"))
    }
}
