package com.example

import androidx.lifecycle.ViewModel
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.PaperCloudSync
import com.example.data.PaperRepository
import com.example.data.SavedPaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [HomeViewModel] over a real in-memory database. [PaperRepository] is final and Room-backed,
 * so substituting it would mean reshaping production code; the cloud-sync dependency is an interface
 * and is faked, which is the part that would otherwise drag Firebase into the test.
 *
 * These use [runBlocking] rather than `runTest` deliberately: Room delivers Flow invalidations on
 * its own executor, and virtual time would let assertions run before that real work lands.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeViewModelTest {

  private lateinit var database: AppDatabase
  private lateinit var repository: PaperRepository
  private lateinit var cloudSync: RecordingCloudSync

  /** Records each sync payload so tests can assert on exactly what would hit Firestore. */
  private class RecordingCloudSync : PaperCloudSync {
    private val calls = Channel<List<SavedPaper>>(Channel.UNLIMITED)

    override suspend fun syncPapersToCloud(papers: List<SavedPaper>) {
      calls.send(papers)
    }

    suspend fun awaitSync(): List<SavedPaper> = withTimeout(SETTLE_MILLIS) { calls.receive() }

    suspend fun awaitNoSync(): List<SavedPaper>? =
      withTimeoutOrNull(NO_EVENT_MILLIS) { calls.receive() }
  }

  private fun paper(id: String, endorsed: Boolean = false) =
    SavedPaper(
      id = id,
      authorInitials = "AB",
      authorName = "Dr. Ada Byron",
      timeAgo = "1h ago",
      affiliation = "AFFILIATION: CAMBRIDGE",
      content = "Content for $id",
      citation = "Byron, A. (2026). Paper $id.",
      isEndorsed = endorsed,
    )

  @Before
  fun setUp() {
    Dispatchers.setMain(Dispatchers.Unconfined)
    database =
      Room.inMemoryDatabaseBuilder(
          ApplicationProvider.getApplicationContext(),
          AppDatabase::class.java,
        )
        .build()
    repository = PaperRepository(database.savedPaperDao())
    cloudSync = RecordingCloudSync()
  }

  @After
  fun tearDown() {
    database.close()
    Dispatchers.resetMain()
  }

  private fun createViewModel() = HomeViewModel(repository, cloudSync)

  /** Suspends until the stored papers satisfy [predicate], or fails the test on timeout. */
  private suspend fun awaitPapers(predicate: (List<SavedPaper>) -> Boolean): List<SavedPaper> =
    withTimeout(SETTLE_MILLIS) { repository.allPapers.first(predicate) }

  @Test
  fun `seeds the sample paper when the database is empty`() = runBlocking {
    createViewModel()

    val papers = awaitPapers { it.isNotEmpty() }

    assertEquals(1, papers.size)
    assertEquals("Dr. Jane Doe", papers.single().authorName)
    assertFalse(papers.single().isEndorsed)
  }

  @Test
  fun `does not seed when papers already exist`() = runBlocking {
    repository.savePaper(paper("existing"))

    createViewModel()

    val extra = withTimeoutOrNull(NO_EVENT_MILLIS) { repository.allPapers.first { it.size > 1 } }
    assertNull("view model seeded a sample paper over existing data", extra)
    assertEquals(listOf("existing"), repository.allPapers.first().map { it.id })
  }

  @Test
  fun `toggleTheme flips dark mode and back`() {
    val viewModel = createViewModel()
    assertFalse(viewModel.isDarkMode.value)

    viewModel.toggleTheme()
    assertTrue(viewModel.isDarkMode.value)

    viewModel.toggleTheme()
    assertFalse(viewModel.isDarkMode.value)
  }

  @Test
  fun `removePaper deletes from storage`() = runBlocking {
    repository.savePaper(paper("1"))
    repository.savePaper(paper("2"))
    val viewModel = createViewModel()

    viewModel.removePaper("1")

    assertEquals(listOf("2"), awaitPapers { it.size == 1 }.map { it.id })
  }

  @Test
  fun `toggleEndorsement persists the flipped value`() = runBlocking {
    repository.savePaper(paper("1", endorsed = false))
    val viewModel = createViewModel()

    viewModel.toggleEndorsement("1", currentStatus = false)

    assertTrue(awaitPapers { it.single().isEndorsed }.single().isEndorsed)
  }

  @Test
  fun `savePaper persists the paper`() = runBlocking {
    val viewModel = createViewModel()
    awaitPapers { it.isNotEmpty() } // let the seed settle first

    viewModel.savePaper(paper("new"))

    assertTrue(awaitPapers { ids -> ids.any { it.id == "new" } }.any { it.id == "new" })
  }

  /**
   * Regression test: `savePaper` inserts the paper and then reads the table back, so appending the
   * same paper to the synced list sent it to Firestore twice. The payload must contain each paper
   * exactly once.
   */
  @Test
  fun `savePaper syncs each paper exactly once`() = runBlocking {
    val viewModel = createViewModel()
    awaitPapers { it.isNotEmpty() }

    val added = paper("new")
    viewModel.savePaper(added)

    val synced = cloudSync.awaitSync()
    assertEquals(
      "synced payload contained duplicate ids: ${synced.map { it.id }}",
      synced.map { it.id }.distinct(),
      synced.map { it.id },
    )
    assertTrue("saved paper missing from sync payload", synced.any { it.id == added.id })
  }

  @Test
  fun `removePaper does not trigger a cloud sync`() = runBlocking {
    repository.savePaper(paper("1"))
    val viewModel = createViewModel()

    viewModel.removePaper("1")

    assertNull("deleting a paper should not push to the cloud", cloudSync.awaitNoSync())
  }

  @Test
  fun `factory builds a HomeViewModel`() {
    // Declared as ViewModel so the type check below is a real assertion, not a tautology.
    val created: ViewModel = HomeViewModelFactory(repository, cloudSync).create(HomeViewModel::class.java)

    assertTrue(created is HomeViewModel)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `factory rejects unknown view model classes`() {
    HomeViewModelFactory(repository, cloudSync).create(UnrelatedViewModel::class.java)
  }

  private class UnrelatedViewModel : ViewModel()

  private companion object {
    /** Bound on how long real Room/Flow work is given to land before a test gives up. */
    const val SETTLE_MILLIS = 5_000L

    /** Shorter window used when asserting that something does *not* happen. */
    const val NO_EVENT_MILLIS = 500L
  }
}
