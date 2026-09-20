package com.example

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AuthorIdentity
import com.example.data.Comment
import com.example.data.ImageStore
import com.example.data.PaperRepository
import com.example.data.PdfStore
import com.example.data.SavedPaper
import com.example.data.SettingsRepository
import com.example.data.VenueCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import com.example.network.SupabaseClient
import com.example.network.SupabaseConfig
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
 * Something worth telling the user about, optionally with a way to take it back.
 *
 * Until now failures were silent: a photo that could not be decoded, an export that threw,
 * a sync that did not happen. The UI had loading and empty states but no error state at all.
 */
data class UserMessage(val text: String, val undo: (() -> Unit)? = null)

/**
 * One entry in the activity feed.
 *
 * Derived from what is actually in the database — replies and quote posts — rather than
 * pushed by a server. The screen this backs previously rendered two fixed cards about a
 * fictional Dr. Julian Thorne.
 */
sealed interface ActivityItem {
    val timestamp: Long
    /** The post to open when this entry is tapped. */
    val targetPaperId: String

    data class Replied(val comment: Comment, val paperTitle: String) : ActivityItem {
        override val timestamp: Long get() = comment.createdAt
        override val targetPaperId: String get() = comment.paperId
    }

    data class Cited(val quote: SavedPaper) : ActivityItem {
        override val timestamp: Long get() = quote.publishedAt
        override val targetPaperId: String get() = quote.id
    }
}

/**
 * Backs the feed, the post detail screen and every social action on a post.
 *
 * [appContext] is the application context, held only so image files can be cleaned up when a
 * post is deleted; it outlives the ViewModel, so there is nothing to leak.
 */
class HomeViewModel(
    private val repository: PaperRepository,
    private val settings: SettingsRepository,
    private val appContext: Context
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = settings.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )

    fun toggleTheme() {
        viewModelScope.launch { settings.setDarkMode(!isDarkMode.value) }
    }

    /**
     * One-shot messages for the app-level snackbar.
     *
     * extraBufferCapacity keeps emit() non-suspending, so failures can be reported from
     * anywhere without a caller having to care whether anything is listening.
     */
    private val _messages = MutableSharedFlow<UserMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UserMessage> = _messages.asSharedFlow()

    fun report(text: String, undo: (() -> Unit)? = null) {
        _messages.tryEmit(UserMessage(text, undo))
    }

    /** Emits the route whose active tab was re-tapped, so that screen can jump to the top. */
    private val _scrollToTop = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val scrollToTop: SharedFlow<String> = _scrollToTop.asSharedFlow()

    fun requestScrollToTop(route: String) {
        _scrollToTop.tryEmit(route)
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

    val vaultPapers: StateFlow<ListState<SavedPaper>> = repository.vaultPapers
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

    fun comments(paperId: String): Flow<ListState<Comment>> {
        viewModelScope.launch {
            try {
                val session = (appContext.applicationContext as? MyApplication)?.sessionManager
                val token = session?.currentAccessToken?.first() ?: SupabaseConfig.ANON_KEY
                val res = SupabaseClient.getComments(paperId, token)
                if (res.isSuccess) {
                    res.getOrThrow().forEach { comment ->
                        repository.addComment(comment)
                    }
                }
            } catch (e: Exception) {
                // Background sync error ignored
            }
        }
        return repository.comments(paperId).map { ListState(items = it, isLoading = false) }
    }

    /**
     * Replies and quote posts, interleaved newest-first.
     *
     * Titles are resolved against the library rather than joined in SQL, because a reply on a
     * post that has since been withdrawn should still render — it just loses its title.
     */
    val activity: StateFlow<ListState<ActivityItem>> = combine(
        repository.recentComments,
        repository.recentQuotes,
        repository.allPapers
    ) { comments, quotes, papers ->
        val titles = papers.associate { it.id to it.title.ifBlank { it.content } }
        val items = comments.map { ActivityItem.Replied(it, titles[it.paperId].orEmpty()) } +
            quotes.map { ActivityItem.Cited(it) }
        ListState(items = items.sortedByDescending { it.timestamp }, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListState()
    )

    /** Venues that actually appear in the library, most-published first. */
    val venues: StateFlow<ListState<VenueCount>> = repository.venueCounts
        .map { ListState(items = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ListState()
        )

    fun papersInVenue(venue: String): Flow<ListState<SavedPaper>> =
        repository.papersInVenue(venue).map { ListState(items = it, isLoading = false) }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Pull-to-refresh: synchronizes the public feed from Supabase PostgreSQL.
     */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                syncFromSupabase()
            } catch (e: Exception) {
                report(appContext.getString(R.string.sync_failed))
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /** Marks every activity entry up to [timestamp] as seen, clearing the unread badge. */
    fun markActivitySeen(timestamp: Long) {
        if (timestamp <= 0L) return
        viewModelScope.launch { settings.markActivitySeen(timestamp) }
    }

    /** Count of activity entries newer than the last one the user looked at. */
    val unreadActivityCount: StateFlow<Int> =
        combine(activity, settings.lastSeenActivityAt) { state, lastSeen ->
            state.items.count { it.timestamp > lastSeen }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    init {
        viewModelScope.launch {
            syncFromSupabase()
        }
    }

    private suspend fun syncFromSupabase() {
        try {
            val session = (appContext.applicationContext as? MyApplication)?.sessionManager
            val token = session?.currentAccessToken?.first() ?: SupabaseConfig.ANON_KEY
            val res = SupabaseClient.getPosts(limit = 50, accessToken = token)
            if (res.isSuccess) {
                val remotePosts = res.getOrThrow()
                remotePosts.forEach { paper ->
                    repository.savePaper(paper)
                }
            }
        } catch (e: Exception) {
            // Offline or initial launch: Room cached posts displayed
        }
    }

    fun savePaper(paper: SavedPaper) {
        viewModelScope.launch {
            repository.savePaper(paper)
            try {
                val session = (appContext.applicationContext as? MyApplication)?.sessionManager
                val userId = session?.currentUserUid?.first().orEmpty()
                val token = session?.currentAccessToken?.first() ?: SupabaseConfig.ANON_KEY
                if (userId.isNotBlank()) {
                    SupabaseClient.createPost(paper, userId, token)
                }
            } catch (e: Exception) {
                // Post saved locally in Room SQLite
            }
        }
    }

    /** Publishes a post that quotes [original], crediting the original with the repost. */
    fun publishQuote(original: SavedPaper, commentary: String) {
        viewModelScope.launch {
            val identity = AuthorIdentity.current(appContext)
            val quotePost = SavedPaper(
                id = UUID.randomUUID().toString(),
                authorInitials = identity.initials,
                authorName = identity.name,
                affiliation = identity.affiliation,
                content = commentary.trim(),
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
            repository.publishQuote(quotePost)
            try {
                val session = (appContext.applicationContext as? MyApplication)?.sessionManager
                val userId = session?.currentUserUid?.first().orEmpty()
                val token = session?.currentAccessToken?.first() ?: SupabaseConfig.ANON_KEY
                if (userId.isNotBlank()) {
                    SupabaseClient.createPost(quotePost, userId, token)
                }
            } catch (e: Exception) {
                // Quote saved locally
            }
        }
    }

    /**
     * Withdraws a post, offering it back.
     */
    fun removePaper(paper: SavedPaper) {
        viewModelScope.launch {
            val orphanedComments = repository.commentsOnce(paper.id)
            repository.deletePaper(paper.id)
            report(appContext.getString(R.string.entry_withdrawn)) {
                restorePaper(paper, orphanedComments)
            }
        }
    }

    private fun restorePaper(paper: SavedPaper, comments: List<Comment>) {
        viewModelScope.launch {
            repository.restorePaper(paper, comments)
        }
    }

    /** Called once an undo can no longer happen, to reclaim the image and pdf files the post held. */
    fun forgetPaper(imageUri: String, pdfLocalPath: String = "") {
        if (imageUri.isNotBlank()) {
            viewModelScope.launch { ImageStore.delete(appContext, imageUri) }
        }
        if (pdfLocalPath.isNotBlank()) {
            viewModelScope.launch { PdfStore.delete(appContext, pdfLocalPath) }
        }
    }

    fun cacheRemotePdf(paperId: String, pdfUrl: String) {
        viewModelScope.launch {
            val localPath = PdfStore.downloadPdf(appContext, pdfUrl)
            if (localPath != null) {
                repository.updatePdfLocalPath(paperId, localPath)
                report("Paper PDF saved to offline vault!")
            } else {
                report("Failed to download PDF. Please check network.")
            }
        }
    }

    fun toggleEndorsement(id: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleEndorsement(id, currentStatus)
            try {
                val session = (appContext.applicationContext as? MyApplication)?.sessionManager
                val userId = session?.currentUserUid?.first().orEmpty()
                val token = session?.currentAccessToken?.first() ?: SupabaseConfig.ANON_KEY
                if (userId.isNotBlank()) {
                    SupabaseClient.toggleLike(id, userId, currentStatus, token)
                }
            } catch (e: Exception) {
                // Local state maintained
            }
        }
    }

    fun toggleBookmark(id: String, currentStatus: Boolean) {
        viewModelScope.launch { repository.toggleBookmark(id, currentStatus) }
    }

    fun addComment(paperId: String, body: String) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val identity = AuthorIdentity.current(appContext)
            val comment = Comment(
                id = UUID.randomUUID().toString(),
                paperId = paperId,
                authorInitials = identity.initials,
                authorName = identity.name,
                affiliation = identity.affiliation,
                body = trimmed,
                createdAt = System.currentTimeMillis()
            )
            repository.addComment(comment)
            try {
                val session = (appContext.applicationContext as? MyApplication)?.sessionManager
                val userId = session?.currentUserUid?.first().orEmpty()
                val token = session?.currentAccessToken?.first() ?: SupabaseConfig.ANON_KEY
                if (userId.isNotBlank()) {
                    SupabaseClient.addComment(paperId, userId, trimmed, token)
                }
            } catch (e: Exception) {
                // Comment saved locally
            }
        }
    }

    fun deleteComment(comment: Comment) {
        viewModelScope.launch { repository.deleteComment(comment) }
    }
}

class HomeViewModelFactory(
    private val repository: PaperRepository,
    private val settings: SettingsRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, settings, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
