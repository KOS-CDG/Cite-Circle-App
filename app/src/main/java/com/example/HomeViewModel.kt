package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CommentEntity
import com.example.data.PaperRepository
import com.example.data.SavedPaper
import com.example.data.prefs.SettingsStore
import com.example.data.prefs.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: PaperRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    /**
     * SYSTEM / LIGHT / DARK, persisted via DataStore. The composable layer resolves SYSTEM into an
     * actual boolean, since that needs isSystemInDarkTheme().
     */
    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM,
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    val savedPapers: StateFlow<List<SavedPaper>> = repository.allPapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            repository.allPapers.take(1).collect { papers ->
                if (papers.isEmpty()) {
                    seedPosts().forEach { repository.savePaper(it) }
                }
            }
        }
    }

    fun comments(postId: String): Flow<List<CommentEntity>> = repository.comments(postId)

    fun addComment(postId: String, body: String) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.addComment(
                CommentEntity(
                    id = "cm-${System.currentTimeMillis()}",
                    postId = postId,
                    authorId = "u-me",
                    authorName = "Dr. Jane Doe",
                    authorInitials = "JD",
                    body = trimmed,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    /** Tapping the already-selected reaction clears it. */
    fun setReaction(postId: String, reaction: String, current: String?) {
        viewModelScope.launch {
            val next = if (current == reaction) null else reaction
            repository.setReaction(postId, next)
            if (reaction == "ENDORSE") {
                repository.toggleEndorsement(postId, current == "ENDORSE")
            }
        }
    }

    fun createPost(content: String, citation: String, fieldKey: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.savePaper(
                SavedPaper(
                    id = "p-$now",
                    authorInitials = "JD",
                    authorName = "Dr. Jane Doe",
                    timeAgo = "",
                    affiliation = "OXFORD",
                    content = trimmed,
                    citation = citation.trim(),
                    createdAt = now,
                    authorId = "u-me",
                    fieldKey = fieldKey,
                ),
            )
        }
    }

    private val firestoreRepo = com.example.data.FirestoreRepository()

    fun savePaper(paper: SavedPaper) {
        viewModelScope.launch {
            repository.savePaper(paper)
            // Sync all papers to cloud (in a real app this might be more targeted)
            repository.allPapers.take(1).collect { papers ->
                firestoreRepo.syncPapersToCloud(papers + paper)
            }
        }
    }

    fun removePaper(id: String) {
        viewModelScope.launch {
            repository.deletePaper(id)
        }
    }
    
    fun toggleEndorsement(id: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleEndorsement(id, currentStatus)
        }
    }
}

class HomeViewModelFactory(
    private val repository: PaperRepository,
    private val settingsStore: SettingsStore,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, settingsStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Seeded feed content. Uses real epoch timestamps so the feed can be ordered by createdAt --
 * the original seed baked in a literal timeAgo String ("2h ago"), which made chronological
 * sorting impossible.
 */
private fun seedPosts(): List<SavedPaper> {
    val now = System.currentTimeMillis()
    fun minutes(n: Long) = now - n * 60_000L

    return listOf(
        SavedPaper(
            id = "p-1",
            authorInitials = "JD",
            authorName = "Dr. Jane Doe",
            timeAgo = "",
            affiliation = "OXFORD",
            content = "I just published a new preprint analyzing the semantic structures of " +
                "large language models. The findings suggest a stark shift in latent knowledge " +
                "representations.",
            citation = "Doe, J. (2026). Semantic Structures in LLMs. Folio Preprints, CC-882-XJ. " +
                "https://cite.circle/refs/882xj",
            createdAt = minutes(35),
            authorId = "u-me",
            fieldKey = "linguistics",
        ),
        SavedPaper(
            id = "p-2",
            authorInitials = "JT",
            authorName = "Dr. Julian Thorne",
            timeAgo = "",
            affiliation = "CERN",
            content = "Replication of the entropy decay result held across all three archival " +
                "clusters. Writing it up now -- the effect is larger than we expected in the " +
                "cold-storage condition.",
            citation = "Thorne, J. (2026). Thermodynamic Decay in Archival Structures. " +
                "Journal of Archival Science, 44(2).",
            createdAt = minutes(180),
            authorId = "u-thorne",
            fieldKey = "physics",
            insightfulCount = 12,
        ),
        SavedPaper(
            id = "p-3",
            authorInitials = "AO",
            authorName = "Dr. Amara Okafor",
            timeAgo = "",
            affiliation = "MIT",
            content = "Sequencing data from the replication study is now open access. If you " +
                "are working on expression variance in the same pathway, please get in touch.",
            citation = "Okafor, A. (2026). Open Sequencing Data for Pathway Variance. " +
                "Molecular Reports, 12(4).",
            createdAt = minutes(600),
            authorId = "u-okafor",
            fieldKey = "biology",
            citeWorthyCount = 5,
        ),
    )
}
