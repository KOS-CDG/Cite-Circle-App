package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.PaperRepository
import com.example.data.SavedPaper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.take

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel(private val repository: PaperRepository) : ViewModel() {
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.update { !it }
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
                    val defaultPaper = SavedPaper(
                        id = "1",
                        authorInitials = "JD",
                        authorName = "Dr. Jane Doe",
                        timeAgo = "2h ago",
                        affiliation = "AFFILIATION: OXFORD",
                        content = "I just published a new preprint analyzing the semantic structures of large language models. The findings suggest a stark shift in latent knowledge representations.",
                        citation = "Doe, J. (2026). Semantic Structures in LLMs. Folio Preprints, CC-882-XJ. https://cite.circle/refs/882xj",
                        isEndorsed = false
                    )
                    repository.savePaper(defaultPaper)
                }
            }
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

class HomeViewModelFactory(private val repository: PaperRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
