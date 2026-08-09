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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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

    /**
     * null means "Room has not answered yet", which is different from "there are no posts". The
     * initial value used to be emptyList(), so the feed rendered its empty state for a frame on
     * every cold start -- you saw "Nothing here yet" flash before the seeded posts arrived. The
     * screens now show loading placeholders for null and the empty state only for a real empty
     * list.
     */
    val savedPapers: StateFlow<List<SavedPaper>?> = repository.allPapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            // Seed when the table is empty OR when the seed itself has moved on. The empty-table
            // check alone meant an install that had ever been opened was frozen on whatever the
            // seed looked like that day.
            val seen = settingsStore.seedVersion.first()
            val papers = repository.allPapers.first()
            if (papers.isEmpty() || seen < SettingsStore.CURRENT_SEED_VERSION) {
                seedPosts().forEach { repository.savePaper(it) }
                settingsStore.setSeedVersion(SettingsStore.CURRENT_SEED_VERSION)
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
            firestoreRepo.syncPapersToCloud(repository.allPapers.first() + paper)
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
            imageUrl = figure("physics"),
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
            imageUrl = figure("biology"),
            citeWorthyCount = 5,
        ),
        SavedPaper(
            id = "p-4",
            authorInitials = "EL",
            authorName = "Prof. Erik Lindqvist",
            timeAgo = "",
            affiliation = "UPPSALA",
            content = "Three seasons of survey work at the harbour site are finally reconciled. " +
                "The revised chronology moves the second phase almost a century later than the " +
                "1974 report assumed.",
            citation = "Lindqvist, E. (2026). Revised Chronology of the Northern Harbour. " +
                "Scandinavian Archaeology, 61(1).",
            createdAt = minutes(1_450),
            authorId = "u-lindqvist",
            fieldKey = "history",
            imageUrl = figure("history"),
            insightfulCount = 8,
            citeWorthyCount = 3,
        ),
        SavedPaper(
            id = "p-5",
            authorInitials = "SN",
            authorName = "Dr. Sofia Navarro",
            timeAgo = "",
            affiliation = "BARCELONA",
            content = "Negative result worth recording: the attention-priming effect did not " +
                "replicate at n=240. We are publishing it anyway, because the original is cited " +
                "far more than it is tested.",
            citation = "Navarro, S. (2026). A Failed Replication of Attention Priming. " +
                "Cognitive Reports, 9(3).",
            createdAt = minutes(2_100),
            authorId = "u-navarro",
            fieldKey = "cognitive",
            imageUrl = figure("cognitive"),
        ),
        SavedPaper(
            id = "p-6",
            authorInitials = "KT",
            authorName = "Dr. Kenji Tanaka",
            timeAgo = "",
            affiliation = "KYOTO",
            content = "Updated the regional output series with the revised 2025 deflators. The " +
                "post-2021 divergence between the two prefectures is smaller than the earlier " +
                "figures implied.",
            citation = "Tanaka, K. (2026). Regional Output Under Revised Deflators. " +
                "Journal of Applied Macroeconomics, 38(2).",
            createdAt = minutes(3_300),
            authorId = "u-tanaka",
            fieldKey = "economics",
            imageUrl = figure("economics"),
            insightfulCount = 4,
        ),
        SavedPaper(
            id = "p-7",
            authorInitials = "NM",
            authorName = "Dr. Nomsa Mbeki",
            timeAgo = "",
            affiliation = "CAPE TOWN",
            content = "Looking for a collaborator with mass-spec capacity for a short follow-up. " +
                "Happy to share the prep protocol either way -- it took us most of a year to " +
                "get it stable.",
            citation = "Mbeki, N. (2025). A Stable Preparation Protocol for Low-Yield Samples. " +
                "Methods in Molecular Biology, 88(6).",
            createdAt = minutes(4_800),
            authorId = "u-mbeki",
            fieldKey = "biology",
        ),
        SavedPaper(
            id = "p-8",
            authorInitials = "JD",
            authorName = "Dr. Jane Doe",
            timeAgo = "",
            affiliation = "OXFORD",
            content = "Teaching note: the corpus we use in the second-year seminar is now " +
                "mirrored with the annotation layer included. Much easier to set exercises on.",
            citation = "Doe, J. (2025). An Annotated Teaching Corpus. " +
                "Folio Preprints, CC-771-QA.",
            createdAt = minutes(7_200),
            authorId = "u-me",
            fieldKey = "linguistics",
            imageUrl = figure("linguistics"),
            citeWorthyCount = 2,
        ),
    )
}

/**
 * Bundled figure for a field, as an android.resource:// URI. Coil resolves that scheme natively,
 * so seeded posts render with the network off -- which is exactly when a demo gets looked at.
 * Remote URLs would show as blank boxes offline or on a 404.
 *
 * The allowlist is deliberate rather than string-interpolating any key straight into a path: an
 * unknown field then yields no image, instead of a URI pointing at a drawable that does not exist.
 */
private fun figure(fieldKey: String): String? = when (fieldKey) {
    "physics", "biology", "history", "linguistics", "cognitive", "economics" ->
        "android.resource://${BuildConfig.APPLICATION_ID}/drawable/seed_$fieldKey"
    else -> null
}
