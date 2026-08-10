package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises [PaperRepository] against a real in-memory Room database, so the DAO's SQL and the
 * repository's behaviour are covered together. A mocked DAO would not catch a broken `@Query`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaperRepositoryTest {

  private lateinit var database: AppDatabase
  private lateinit var repository: PaperRepository

  private fun paper(id: String, endorsed: Boolean = false) =
    SavedPaper(
      id = id,
      authorInitials = "JD",
      authorName = "Dr. Jane Doe",
      timeAgo = "2h ago",
      affiliation = "AFFILIATION: OXFORD",
      content = "Content for $id",
      citation = "Doe, J. (2026). Paper $id.",
      isEndorsed = endorsed,
    )

  @Before
  fun setUp() {
    database =
      Room.inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext(),
          AppDatabase::class.java,
        )
        .build()
    repository = PaperRepository(database.savedPaperDao())
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `starts empty`() = runTest {
    assertEquals(emptyList<SavedPaper>(), repository.allPapers.first())
  }

  @Test
  fun `saved paper is read back with all fields intact`() = runTest {
    val saved = paper("1")
    repository.savePaper(saved)

    assertEquals(listOf(saved), repository.allPapers.first())
  }

  @Test
  fun `saving the same id replaces rather than duplicates`() = runTest {
    repository.savePaper(paper("1"))
    repository.savePaper(paper("1").copy(authorName = "Dr. John Roe"))

    val papers = repository.allPapers.first()
    assertEquals(1, papers.size)
    assertEquals("Dr. John Roe", papers.single().authorName)
  }

  @Test
  fun `deletePaper removes only the requested row`() = runTest {
    repository.savePaper(paper("1"))
    repository.savePaper(paper("2"))

    repository.deletePaper("1")

    assertEquals(listOf("2"), repository.allPapers.first().map { it.id })
  }

  @Test
  fun `deleting an unknown id leaves the table untouched`() = runTest {
    repository.savePaper(paper("1"))

    repository.deletePaper("does-not-exist")

    assertEquals(listOf("1"), repository.allPapers.first().map { it.id })
  }

  @Test
  fun `toggleEndorsement flips false to true`() = runTest {
    repository.savePaper(paper("1", endorsed = false))

    repository.toggleEndorsement("1", currentStatus = false)

    assertTrue(repository.allPapers.first().single().isEndorsed)
  }

  @Test
  fun `toggleEndorsement flips true to false`() = runTest {
    repository.savePaper(paper("1", endorsed = true))

    repository.toggleEndorsement("1", currentStatus = true)

    assertFalse(repository.allPapers.first().single().isEndorsed)
  }

  /**
   * `toggleEndorsement` inverts the caller-supplied `currentStatus` instead of reading the stored
   * value, so a stale value from the UI writes the wrong result. This pins that behaviour: passing
   * a `currentStatus` that disagrees with the database overwrites the row with the caller's view.
   */
  @Test
  fun `toggleEndorsement trusts the caller over the stored value`() = runTest {
    repository.savePaper(paper("1", endorsed = true))

    // Caller wrongly believes the paper is un-endorsed; the stored `true` is ignored.
    repository.toggleEndorsement("1", currentStatus = false)

    assertTrue(repository.allPapers.first().single().isEndorsed)
  }

  @Test
  fun `allPapers re-emits when the table changes`() = runTest {
    repository.allPapers.test {
      assertEquals(emptyList<SavedPaper>(), awaitItem())

      repository.savePaper(paper("1"))
      assertEquals(listOf("1"), awaitItem().map { it.id })

      repository.savePaper(paper("2"))
      assertEquals(listOf("1", "2"), awaitItem().map { it.id })

      repository.deletePaper("1")
      assertEquals(listOf("2"), awaitItem().map { it.id })

      cancelAndIgnoreRemainingEvents()
    }
  }
}
