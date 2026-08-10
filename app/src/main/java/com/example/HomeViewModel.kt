package com.example

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AuthorIdentity
import com.example.data.Comment
import com.example.data.ImageStore
import com.example.data.PaperRepository
import com.example.data.SavedPaper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * A list plus whether it has actually loaded yet.
 *
 * Previously every list was a bare `StateFlow<List<T>>` seeded with `emptyList()`, which made
 * "still loading" and "genuinely empty" the same value — so the empty state flashed on every
 * cold start before the first Room emission arrived. Skeletons need to know the difference.
 */
data class ListState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = true
) {
    /** True only once loading has finished and there is still nothing to show. */
    val isEmpty: Boolean get() = !isLoading && items.isEmpty()
}

/**
 * Backs the feed, the post detail screen and every social action on a post.
 *
 * [appContext] is the application context, held only so image files can be cleaned up when a
 * post is deleted; it outlives the ViewModel, so there is nothing to leak.
 */
class HomeViewModel(
    private val repository: PaperRepository,
    private val appContext: Context
) : ViewModel() {

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.update { !it }
    }

    val feed: StateFlow<ListState<SavedPaper>> = repository.allPapers
        .map { ListState(items = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListState()
        )

    val bookmarks: StateFlow<ListState<SavedPaper>> = repository.bookmarkedPapers
        .map { ListState(items = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListState()
        )

    /** Convenience for screens that only need to look a post up by id. */
    val savedPapers: StateFlow<List<SavedPaper>> = repository.allPapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun comments(paperId: String): Flow<ListState<Comment>> =
        repository.comments(paperId).map { ListState(items = it, isLoading = false) }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Pull-to-refresh.
     *
     * Room already pushes changes reactively, so there is nothing local to re-fetch. What
     * this does perform is a real cloud sync attempt; with the placeholder Firebase config it
     * fails fast inside FirestoreRepository and the spinner simply ends. No artificial delay
     * is inserted to make it feel busier than it is.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                syncToCloud()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.allPapers.take(1).collect { papers ->
                if (papers.isEmpty()) {
                    repository.savePaper(
                        SavedPaper(
                            id = "1",
                            authorInitials = "JD",
                            authorName = "Dr. Jane Doe",
                            affiliation = "AFFILIATION: OXFORD",
                            content = "I just published a new preprint analyzing the semantic structures of large language models. The findings suggest a stark shift in latent knowledge representations.",
                            title = "Semantic Structures in Large Language Models",
                            authors = "Doe, Jane",
                            year = "2026",
                            venue = "Folio Preprints",
                            url = "https://cite.circle/refs/882xj",
                            publishedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    private val firestoreRepo = com.example.data.FirestoreRepository()

    fun savePaper(paper: SavedPaper) {
        viewModelScope.launch {
            repository.savePaper(paper)
            syncToCloud()
        }
    }

    /** Publishes a post that quotes [original], crediting the original with the repost. */
    fun publishQuote(original: SavedPaper, commentary: String) {
        viewModelScope.launch {
            val identity = AuthorIdentity.current()
            repository.publishQuote(
                SavedPaper(
                    id = UUID.randomUUID().toString(),
                    authorInitials = identity.initials,
                    authorName = identity.name,
                    affiliation = identity.affiliation,
                    content = commentary.trim(),
                    // The citation travels with the quote so the repost is independently citable.
                    title = original.title,
                    authors = original.authors,
                    year = original.year,
                    venue = original.venue,
                    doi = original.doi,
                    url = original.url,
                    citationOverride = original.citationOverride,
                    publishedAt = System.currentTimeMillis(),
                    quotedId = original.id,
                    quotedAuthorName = original.authorName,
                    quotedTitle = original.title,
                    quotedContent = original.content
                )
            )
            syncToCloud()
        }
    }

    fun removePaper(paper: SavedPaper) {
        viewModelScope.launch {
            repository.deletePaper(paper.id)
            // The row is gone; drop its image so deleted posts do not leak storage.
            ImageStore.delete(appContext, paper.imageUri)
            syncToCloud()
        }
    }

    fun toggleEndorsement(id: String, currentStatus: Boolean) {
        viewModelScope.launch { repository.toggleEndorsement(id, currentStatus) }
    }

    fun toggleBookmark(id: String, currentStatus: Boolean) {
        viewModelScope.launch { repository.toggleBookmark(id, currentStatus) }
    }

    fun addComment(paperId: String, body: String) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val identity = AuthorIdentity.current()
            repository.addComment(
                Comment(
                    id = UUID.randomUUID().toString(),
                    paperId = paperId,
                    authorInitials = identity.initials,
                    authorName = identity.name,
                    affiliation = identity.affiliation,
                    body = trimmed,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch { repository.deleteComment(comment) }
    }

    private suspend fun syncToCloud() {
        // Read back after the write so the sync sees the canonical list.
        repository.allPapers.take(1).collect { papers ->
            firestoreRepo.syncPapersToCloud(papers)
        }
    }
}

class HomeViewModelFactory(
    private val repository: PaperRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
