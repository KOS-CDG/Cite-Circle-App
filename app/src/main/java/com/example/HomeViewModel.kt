package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FirestoreRepository
import com.example.data.PaperRepository
import com.example.data.SavedPaper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(private val repository: PaperRepository) : ViewModel() {

    private val firestoreRepo = FirestoreRepository()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    /**
     * True until the database has produced its first emission. [savedPapers] starts at
     * `emptyList()`, so without this the UI cannot tell "still loading" from "genuinely empty"
     * and would flash the empty state on every cold start.
     */
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val savedPapers: StateFlow<List<SavedPaper>> = repository.allPapers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    init {
        viewModelScope.launch {
            val papers = repository.allPapers.first()
            if (papers.isEmpty()) {
                repository.savePaper(seedPaper())
            }
            _isLoading.value = false
        }
    }

    fun toggleTheme() {
        _isDarkMode.update { !it }
    }

    fun paperById(id: String?): SavedPaper? = savedPapers.value.firstOrNull { it.id == id }

    fun savePaper(paper: SavedPaper) {
        viewModelScope.launch {
            repository.savePaper(paper)
            // Read back after the write so the upload reflects the stored table exactly.
            firestoreRepo.syncPapersToCloud(repository.allPapers.first())
        }
    }

    /** Builds a [SavedPaper] from the compose form and persists it. */
    fun publishPaper(
        content: String,
        citation: String,
        authorName: String,
        authorInitials: String,
        affiliation: String,
    ) {
        savePaper(
            SavedPaper(
                id = UUID.randomUUID().toString(),
                authorInitials = authorInitials,
                authorName = authorName,
                timeAgo = "just now",
                affiliation = affiliation,
                content = content,
                citation = citation,
                isEndorsed = false,
            ),
        )
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

    private fun seedPaper() = SavedPaper(
        id = "1",
        authorInitials = "JD",
        authorName = "Dr. Jane Doe",
        timeAgo = "2h ago",
        affiliation = "AFFILIATION: OXFORD",
        content = "I just published a new preprint analyzing the semantic structures of large " +
            "language models. The findings suggest a stark shift in latent knowledge " +
            "representations.",
        citation = "Doe, J. (2026). Semantic Structures in LLMs. Folio Preprints, CC-882-XJ. " +
            "https://cite.circle/refs/882xj",
        isEndorsed = false,
    )
}

class HomeViewModelFactory(private val repository: PaperRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
